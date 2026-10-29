package tv.zelta.app.ui

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.settings.AppSettings
import tv.zelta.app.ui.components.ChannelSidebar
import tv.zelta.app.ui.components.PlayerPane
import tv.zelta.app.ui.components.ShortcutsDialog
import tv.zelta.app.ui.components.ZeltaRiButton
import tv.zelta.app.ui.components.zt
import tv.zelta.app.ui.screens.ConfirmDialog
import tv.zelta.app.ui.screens.MainMenuDialog
import tv.zelta.app.ui.screens.QualityDialog
import tv.zelta.app.ui.screens.SettingsScreen
import tv.zelta.app.ui.screens.SetupScreen
import tv.zelta.app.ui.screens.SourcesDialog
import tv.zelta.app.ui.screens.ChannelMenuDialog
import tv.zelta.app.ui.screens.HomeLibraryScreen
import tv.zelta.app.ui.theme.Ri

private const val ROUTE_HOME = "home"
private const val ROUTE_SETUP = "setup"
private const val ROUTE_SETTINGS = "settings"

/**
 * Android shell matching the desktop window: 36dp top bar, 256dp channel rail,
 * full-bleed player, centered dialogs for sources, channels, quality and quit.
 */
@Composable
fun ZeltaApp(settings: AppSettings) {
    val viewModel: PlayerViewModel = viewModel()
    var route by rememberSaveable {
        mutableStateOf(if (settings.setupDone) ROUTE_HOME else ROUTE_SETUP)
    }

    BackHandler(enabled = route == ROUTE_SETTINGS) { route = ROUTE_HOME }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (route) {
            ROUTE_SETUP -> SetupScreen(
                settings = settings,
                viewModel = viewModel,
                onDone = { route = ROUTE_HOME },
            )
            ROUTE_SETTINGS -> SettingsScreen(
                settings = settings,
                viewModel = viewModel,
                onBack = { route = ROUTE_HOME },
                onOpenSources = { route = ROUTE_HOME },
            )
            else -> HomeShell(
                settings = settings,
                viewModel = viewModel,
                onOpenSettings = { route = ROUTE_SETTINGS },
            )
        }
    }
}

@Composable
private fun HomeShell(
    settings: AppSettings,
    viewModel: PlayerViewModel,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = androidx.compose.ui.platform.LocalView.current
    val configuration = LocalConfiguration.current
    val isTelevision = remember(context) {
        val manager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        manager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    var menuOpen by remember { mutableStateOf(false) }
    var channelMenuOpen by remember { mutableStateOf(false) }
    var qualityOpen by remember { mutableStateOf(false) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var shortcutsOpen by remember { mutableStateOf(false) }
    var quitOpen by remember { mutableStateOf(false) }
    var sidebarVisible by rememberSaveable { mutableStateOf(true) }
    var wantsPlayer by rememberSaveable { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val portrait = maxWidth < maxHeight
        // phones (not tablets, not TVs) keep working in portrait: the player sits
        // above the channel list instead of forcing a landscape rotation
        val handset = configuration.smallestScreenWidthDp < 600
        val compact = maxWidth < 840.dp || handset
        val playing = wantsPlayer && viewModel.selected != null
        val portraitPlayback = handset && portrait && playing
        val playerVisible = playing
        // No playlist yet: the desktop shows the tile library instead of the
        // sidebar, so Android does the same.
        val libraryOnly = viewModel.channels.isEmpty() && !playing

        // Rotation follows the device sensor: turning the phone sideways rotates
        // the app, even with the system rotation lock on. TVs keep their own
        // fixed orientation.
        androidx.compose.runtime.LaunchedEffect(isTelevision) {
            if (isTelevision) return@LaunchedEffect
            val target = activity ?: return@LaunchedEffect
            if (target.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
                target.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }

        BackHandler(enabled = fullscreen) { fullscreen = false }
        BackHandler(enabled = playerVisible) { wantsPlayer = false }
        BackHandler(enabled = !compact && !sidebarVisible) { sidebarVisible = true }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            // the app opens fullscreen: system bars stay hidden for the whole session
            val window = activity?.window ?: return@LaunchedEffect
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Column(Modifier.fillMaxSize()) {
            if (!fullscreen && !(handset && playerVisible)) {
                TopBar(
                    hasPlaylist = viewModel.channels.isNotEmpty(),
                    sidebarVisible = sidebarVisible,
                    compact = compact,
                    onHome = {
                        wantsPlayer = false
                        if (!compact) sidebarVisible = true
                    },
                    onMenu = { menuOpen = true },
                    onQuit = { quitOpen = true },
                    onToggleSidebar = {
                        if (compact) {
                            if (playerVisible) wantsPlayer = false
                            else if (viewModel.selected != null) wantsPlayer = true
                        } else {
                            sidebarVisible = !sidebarVisible
                        }
                    },
                    onShortcuts = { shortcutsOpen = true },
                    onGithub = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zeltatv/android")),
                            )
                        }
                    },
                )
            }

            if (libraryOnly) {
                HomeLibraryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            } else if (compact && portraitPlayback) {
                // portrait phone: 16:9 player on top, channel list underneath
                Column(Modifier.fillMaxSize()) {
                    PlayerPane(
                        viewModel = viewModel,
                        compact = true,
                        fullscreen = false,
                        onToggleFullscreen = { fullscreen = true },
                        onOpenChannels = { channelMenuOpen = true },
                        onOpenMenu = { menuOpen = true },
                        onOpenSources = { sourcesOpen = true },
                        onOpenQuality = { qualityOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                    ChannelSidebar(
                        viewModel = viewModel,
                        onChannelSelected = { channel ->
                            viewModel.play(channel)
                            wantsPlayer = true
                        },
                        onOpenSources = { sourcesOpen = true },
                        onOpenSettings = onOpenSettings,
                        onFullscreen = { fullscreen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            } else if (compact) {
                if (playerVisible) {
                    PlayerPane(
                        viewModel = viewModel,
                        compact = true,
                        fullscreen = fullscreen,
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        onOpenChannels = { channelMenuOpen = true },
                        onOpenMenu = { menuOpen = true },
                        onOpenSources = { sourcesOpen = true },
                        onOpenQuality = { qualityOpen = true },
                    )
                } else {
                    ChannelSidebar(
                        viewModel = viewModel,
                        onChannelSelected = { channel ->
                            viewModel.play(channel)
                            // portrait phones rotate to landscape and show the player
                            wantsPlayer = true
                        },
                        onOpenSources = { sourcesOpen = true },
                        onOpenSettings = onOpenSettings,
                        onFullscreen = { wantsPlayer = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    if (sidebarVisible && !fullscreen) {
                        ChannelSidebar(
                            viewModel = viewModel,
                            onOpenSources = { sourcesOpen = true },
                            onOpenSettings = onOpenSettings,
                            onFullscreen = { fullscreen = true },
                            modifier = Modifier
                                .width(256.dp)
                                .fillMaxHeight(),
                        )
                    }
                    Box(Modifier.fillMaxSize()) {
                        PlayerPane(
                            viewModel = viewModel,
                            fullscreen = fullscreen,
                            onToggleFullscreen = { fullscreen = !fullscreen },
                            onOpenChannels = { channelMenuOpen = true },
                            onOpenMenu = { menuOpen = true },
                            onOpenSources = { sourcesOpen = true },
                            onOpenQuality = { qualityOpen = true },
                        )
                    }
                }
            }
        }

        if (menuOpen) {
            MainMenuDialog(
                channelCount = viewModel.channels.size,
                onDismiss = { menuOpen = false },
                onSources = {
                    menuOpen = false
                    sourcesOpen = true
                },
                onChannels = {
                    menuOpen = false
                    if (compact) wantsPlayer = false else sidebarVisible = true
                },
                onSettings = {
                    menuOpen = false
                    onOpenSettings()
                },
                onShortcuts = {
                    menuOpen = false
                    shortcutsOpen = true
                },
                onAppearance = {
                    menuOpen = false
                    onOpenSettings()
                },
                onClose = { menuOpen = false },
            )
        }

        if (channelMenuOpen) {
            ChannelMenuDialog(
                viewModel = viewModel,
                onDismiss = {
                    channelMenuOpen = false
                    if (compact && viewModel.selected != null) wantsPlayer = true
                },
            )
        }

        if (qualityOpen) {
            QualityDialog(viewModel = viewModel, onDismiss = { qualityOpen = false })
        }

        if (sourcesOpen) {
            SourcesDialog(viewModel = viewModel, onDismiss = { sourcesOpen = false })
        }

        if (shortcutsOpen) {
            ShortcutsDialog(onDismiss = { shortcutsOpen = false })
        }

        if (quitOpen) {
            ConfirmDialog(
                title = zt("ZeltaTV kapatılsın mı?", "Close ZeltaTV?"),
                body = zt("Programı kapatmak istediğinize emin misiniz?", "Do you want to close the program?"),
                confirmLabel = zt("Kapat", "Close"),
                onDismiss = { quitOpen = false },
                onConfirm = {
                    quitOpen = false
                    activity?.finish()
                },
            )
        }
    }
}

@Composable
private fun TopBar(
    hasPlaylist: Boolean,
    sidebarVisible: Boolean,
    compact: Boolean,
    onHome: () -> Unit,
    onMenu: () -> Unit,
    onQuit: () -> Unit,
    onToggleSidebar: () -> Unit,
    onShortcuts: () -> Unit,
    onGithub: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZeltaRiButton(Ri.Home, onHome, zt("Ana ekran", "Home"), tint = muted)
        ZeltaRiButton(Ri.Settings, onMenu, zt("Menü", "Menu"), tint = muted)
        ZeltaRiButton(Ri.ShutDown, onQuit, zt("Uygulamayı kapat", "Close the application"), tint = muted)
        if (hasPlaylist) {
            ZeltaRiButton(
                if (sidebarVisible) Ri.SidebarFold else Ri.SidebarUnfold,
                onToggleSidebar,
                if (compact) {
                    if (sidebarVisible) zt("Oynatıcı", "Player") else zt("Kanallar", "Channels")
                } else {
                    if (sidebarVisible) zt("Kenar çubuğunu gizle", "Hide sidebar") else zt("Kenar çubuğunu göster", "Show sidebar")
                },
                tint = muted,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "ZeltaTV",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        )
        Spacer(Modifier.weight(1f))
        ZeltaRiButton(Ri.Info, onShortcuts, zt("Klavye ve kumanda kısayolları", "Keyboard and remote shortcuts"), tint = muted)
        if (hasPlaylist) {
            ZeltaRiButton(Ri.Github, onGithub, "GitHub", tint = muted)
        }
        Spacer(Modifier.width(2.dp))
    }
}
