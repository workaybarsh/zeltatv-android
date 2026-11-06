package tv.zelta.app.ui

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    // Requests coming from Settings need to survive the route switch and be
    // picked up by HomeShell, which owns the sources dialog.
    var openSourcesRequest by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = route == ROUTE_SETTINGS) { route = ROUTE_HOME }

    // A TV box must not keep stream audio alive while the app is in the
    // background (home screen, file picker, another app).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.pauseForBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                onOpenSources = {
                    route = ROUTE_HOME
                    openSourcesRequest = true
                },
            )
            else -> HomeShell(
                settings = settings,
                viewModel = viewModel,
                openSourcesRequest = openSourcesRequest,
                onOpenSourcesRequestHandled = { openSourcesRequest = false },
                onOpenSettings = { route = ROUTE_SETTINGS },
            )
        }
    }
}

@Composable
private fun HomeShell(
    settings: AppSettings,
    viewModel: PlayerViewModel,
    openSourcesRequest: Boolean,
    onOpenSourcesRequestHandled: () -> Unit,
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
    var sourcesOpen by remember { mutableStateOf(false) }
    var shortcutsOpen by remember { mutableStateOf(false) }
    var quitOpen by remember { mutableStateOf(false) }
    var libraryOpen by rememberSaveable { mutableStateOf(false) }
    var wantsPlayer by rememberSaveable { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    // A restored session opens straight into the player. On phones the split
    // view is the portrait home, so only a TV starts fullscreen.
    LaunchedEffect(viewModel.openPlayerOnLaunch) {
        if (viewModel.openPlayerOnLaunch) {
            wantsPlayer = true
            fullscreen = isTelevision
            viewModel.consumeOpenPlayerOnLaunch()
        }
    }

    LaunchedEffect(openSourcesRequest) {
        if (openSourcesRequest) {
            sourcesOpen = true
            onOpenSourcesRequestHandled()
        }
    }

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
        // sidebar, so Android does the same. The home button opens the same
        // library on demand, even when channels are already loaded.
        val libraryOnly = viewModel.channels.isEmpty() && !playing
        val libraryVisible = libraryOpen || libraryOnly

        // Portrait always shows the split view (player above the channel list).
        // Rotating back from landscape therefore leaves fullscreen
        // automatically instead of trapping the viewer in the player.
        LaunchedEffect(portrait) {
            if (portrait) fullscreen = false
        }

        // Rotation follows the device sensor, except that entering fullscreen
        // on a phone locks the app to landscape; leaving fullscreen returns to
        // the sensor so upright devices go back to the split view.
        androidx.compose.runtime.LaunchedEffect(isTelevision, fullscreen) {
            if (isTelevision) return@LaunchedEffect
            val target = activity ?: return@LaunchedEffect
            val wanted = if (fullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            if (target.requestedOrientation != wanted) {
                target.requestedOrientation = wanted
            }
        }

        // While fullscreen, turning the phone back upright leaves fullscreen,
        // so the app returns to the split view. The listener only reacts after
        // the device has actually been in landscape, which keeps the forced
        // landscape from cancelling itself right after the tap.
        if (!isTelevision) {
            DisposableEffect(fullscreen) {
                if (!fullscreen) return@DisposableEffect onDispose {}
                val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (manager == null || sensor == null) return@DisposableEffect onDispose {}
                var sawLandscape = false
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values[0]
                        val y = event.values[1]
                        if (abs(x) > abs(y)) {
                            sawLandscape = true
                        } else if (sawLandscape && y > 3f) {
                            fullscreen = false
                            viewModel.resumePlayback()
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                onDispose { manager.unregisterListener(listener) }
            }
        }

        // Priority matters: close the library first, then leave the player
        // (pausing it), then leave fullscreen, and finally ask before quitting
        // instead of dropping the viewer out of the app.
        BackHandler {
            when {
                libraryOpen -> libraryOpen = false
                // On a phone, Back first leaves the fullscreen player and
                // returns to the split view (still playing); a second Back
                // pauses and shows the channel list.
                compact && playerVisible && fullscreen -> {
                    fullscreen = false
                    viewModel.resumePlayback()
                }
                compact && playerVisible -> {
                    fullscreen = false
                    wantsPlayer = false
                    viewModel.pausePlayback()
                }
                fullscreen -> fullscreen = false
                playerVisible -> {
                    wantsPlayer = false
                    viewModel.pausePlayback()
                }
                else -> quitOpen = true
            }
        }

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
                    onHome = {
                        wantsPlayer = false
                        fullscreen = false
                        libraryOpen = true
                        viewModel.pausePlayback()
                    },
                    onMenu = { menuOpen = true },
                    onQuit = { quitOpen = true },
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

            if (libraryVisible) {
                HomeLibraryScreen(
                    viewModel = viewModel,
                    onPlaylistOpened = { libraryOpen = false },
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (compact && portraitPlayback && !fullscreen) {
                // portrait phone: 16:9 player on top, channel list underneath
                Column(Modifier.fillMaxSize()) {
                    PlayerPane(
                        viewModel = viewModel,
                        onOpenChannels = { channelMenuOpen = true },
                        onOpenMenu = { menuOpen = true },
                        onOpenSources = { sourcesOpen = true },
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
                        onFullscreen = { fullscreen = true; viewModel.resumePlayback() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            } else if (compact) {
                if (playerVisible) {
                    PlayerPane(
                        viewModel = viewModel,
                        onOpenChannels = { channelMenuOpen = true },
                        onOpenMenu = { menuOpen = true },
                        onOpenSources = { sourcesOpen = true },
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
                        onFullscreen = { wantsPlayer = true; viewModel.resumePlayback() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    if (!fullscreen) {
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
                            onOpenChannels = { channelMenuOpen = true },
                            onOpenMenu = { menuOpen = true },
                            onOpenSources = { sourcesOpen = true },
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
                    libraryOpen = false
                    wantsPlayer = false
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


        if (sourcesOpen) {
            SourcesDialog(viewModel = viewModel, onDismiss = { sourcesOpen = false })
        }

        if (shortcutsOpen) {
            ShortcutsDialog(onDismiss = { shortcutsOpen = false })
        }

        if (quitOpen) {
            ConfirmDialog(
                title = zt("ZeltaTV", "ZeltaTV"),
                body = zt("Uygulamadan çıkmak istediğinize emin misiniz?", "Are you sure you want to exit the app?"),
                confirmLabel = zt("Çık", "Exit"),
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
    onHome: () -> Unit,
    onMenu: () -> Unit,
    onQuit: () -> Unit,
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
