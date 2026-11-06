package tv.zelta.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.settings.AppSettings
import tv.zelta.app.settings.I18n
import tv.zelta.app.ui.components.BrandLockup
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaButton
import tv.zelta.app.ui.components.ZeltaChip
import tv.zelta.app.ui.components.ZeltaRiButton
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.channelCountText
import tv.zelta.app.ui.components.zt
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon
import tv.zelta.app.ui.theme.ZELTA_PALETTES
import tv.zelta.app.ui.theme.ZeltaTurquoise
import tv.zelta.app.ui.theme.isBrightColor
import tv.zelta.app.ui.theme.zeltaColors

private data class SettingsTab(val id: String, val glyph: String, val label: String)

/** Desktop settings page: 48dp header, tab row and max-w-2xl sections. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenSources: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    // The settings rail is a wide-screen (tablet/TV) pattern; on a phone it would
    // squeeze the content panel, so phones keep the tab strip in both orientations.
    val wideRail = configuration.smallestScreenWidthDp >= 600
    var tab by rememberSaveable { mutableStateOf("playback") }
    var confirmReset by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    val tabs = listOf(
        SettingsTab("shortcuts", Ri.Keyboard, zt("Kısayollar", "Shortcuts")),
        SettingsTab("playback", Ri.PlayCircle, zt("Oynatma", "Playback")),
        SettingsTab("language", Ri.Translate, zt("Dil", "Language")),
        SettingsTab("theme", Ri.Palette, zt("Tema", "Theme")),
        SettingsTab("data", Ri.Database, zt("Veriler", "Data")),
        SettingsTab("about", Ri.Info, zt("Hakkında", "About")),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // header h-12 px-3
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZeltaRiButton(
                Ri.ArrowLeft,
                onBack,
                zt("Geri", "Go back"),
                modifier = Modifier
                    .focusRequester(firstFocus)
                    .autoFocus(firstFocus),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                zt("Ayarlar", "Settings"),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
            )
        }

        val content: @Composable () -> Unit = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 672.dp),
                ) {
                    when (tab) {
                        "shortcuts" -> ShortcutsTab()
                        "language" -> LanguageTab(settings, activity)
                        "theme" -> ThemeTab(settings)
                        "data" -> DataTab(
                            settings = settings,
                            viewModel = viewModel,
                            onOpenSources = onOpenSources,
                            onReset = { confirmReset = true },
                        )
                        "about" -> AboutTab(context)
                        else -> PlaybackTab(settings, viewModel)
                    }
                }
            }
        }

        if (wideRail) {
            // Wide screens keep the vertical remote rail of the desktop settings page.
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    Modifier
                        .width(224.dp)
                        .fillMaxHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        .padding(8.dp),
                ) {
                    Text(
                        zt("AYARLAR", "SETTINGS"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                    tabs.forEach { item ->
                        SettingsRailButton(item, tab == item.id) { tab = item.id }
                    }
                }
                Column(Modifier.weight(1f)) { content() }
            }
        } else {
            // tabs (px-3, border-b)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { item ->
                    SettingsTabButton(
                        tab = item,
                        active = tab == item.id,
                        onClick = { tab = item.id },
                    )
                }
            }
            content()
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = zt("Uygulamayı sıfırla", "Reset app to defaults"),
            body = zt(
                "Tüm oynatma listeleri, favoriler, kısayollar, tema ve dil ayarları silinir. Bu işlem geri alınamaz.",
                "Erase all playlists, favourites, shortcuts, theme and language settings. This cannot be undone.",
            ),
            confirmLabel = zt("Sıfırla", "Reset"),
            danger = true,
            onDismiss = { confirmReset = false },
            onConfirm = {
                confirmReset = false
                viewModel.sources.forEach(viewModel::removeSource)
                settings.resetAll()
                activity?.recreate()
            },
        )
    }
}

@Composable
private fun SettingsRailButton(tab: SettingsTab, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) zeltaColors.focus else Color.Transparent,
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiIcon(
            tab.glyph,
            size = 14.dp,
            tint = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            tab.label,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingsTabButton(tab: SettingsTab, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(if (active) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) zeltaColors.focus else Color.Transparent,
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiIcon(
            tab.glyph,
            size = 14.dp,
            tint = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            tab.label,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PlaybackTab(settings: AppSettings, viewModel: PlayerViewModel) {
    Card {
        SettingRow(
            title = zt("Daima en yüksek kalitede oynat", "Always play at highest quality"),
            body = zt(
                "Açıkken her kanal ve video, oynatma listesinin sunduğu en yüksek çözünürlükte başlar.",
                "Every channel and video starts at the highest available resolution when enabled.",
            ),
        ) {
            Switch(
                checked = settings.highestQuality,
                onCheckedChange = { viewModel.applyHighestQuality(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun LanguageTab(settings: AppSettings, activity: Activity?) {
    Card {
        SectionLabel(zt("ARAYÜZ DİLİ", "INTERFACE LANGUAGE"))
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            I18n.LANGUAGES.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (code, label) ->
                        ZeltaChip(label, settings.language == code, {
                            if (settings.language != code) {
                                settings.updateLanguage(code)
                                activity?.recreate()
                            }
                        })
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            zt("Uygulama metinleri ve menüler için dil.", "Language used by the app UI and menus."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ThemeTab(settings: AppSettings) {
    Card {
        SectionLabel(zt("TEMA", "THEME"))
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ZELTA_PALETTES.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { palette ->
                        PaletteSwatch(
                            name = when (palette.id) {
                                "default" -> zt("Varsayılan", "Default")
                                "mono" -> zt("Siyah beyaz", "Black & white")
                                "light" -> zt("Açık", "Light")
                                "navy" -> zt("Lacivert", "Navy")
                                "forest" -> zt("Orman", "Forest")
                                "plum" -> zt("Mürdüm", "Plum")
                                else -> zt("Sıcak", "Warm")
                            },
                            background = palette.background,
                            accent = palette.accent ?: ZeltaTurquoise,
                            selected = settings.theme == palette.id,
                            onClick = { settings.updateTheme(palette.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DataTab(
    settings: AppSettings,
    viewModel: PlayerViewModel,
    onOpenSources: () -> Unit,
    onReset: () -> Unit,
) {
    Card {
        SettingRow(
            title = zt("Kanal kitaplığı", "Channel library"),
            body = "${channelCountText(viewModel.channels.size)} · ${viewModel.activeSource?.name ?: "-"}",
        ) {
            ZeltaButton(zt("Kaynaklar", "Sources"), onOpenSources, glyph = Ri.PlayList)
        }
        Spacer(Modifier.height(12.dp))
        SettingRow(
            title = zt("Varsayılanlara sıfırla", "Reset app to defaults"),
            body = zt("Tüm verileri siler ve fabrika ayarlarına döner.", "Erases all data and returns to factory defaults."),
        ) {
            ZeltaButton(zt("Sıfırla", "Reset"), onReset, danger = true, glyph = Ri.Restart)
        }
    }
}

@Composable
private fun AboutTab(context: android.content.Context) {
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    Card {
        BrandLockup()
        Spacer(Modifier.height(12.dp))
        Text(
            zt(
                "Sürüm {version} · Android",
                "Version {version} · Android",
            ).replace("{version}", tv.zelta.app.BuildConfig.VERSION_NAME),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            zt(
                "ZeltaTV, kendi M3U listelerinizi TV, tablet ve telefonda izlemek için tasarlanmış açık kaynak bir IPTV oynatıcıdır.",
                "ZeltaTV is an open-source IPTV player built to watch your own M3U playlists on TV, tablet and phone.",
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZeltaButton("GitHub", { open("https://github.com/workaybarsh/zeltatv-android") }, primary = true, glyph = Ri.Github)
            ZeltaButton(zt("Lisans", "Licence"), { open("https://www.gnu.org/licenses/gpl-3.0.html") }, glyph = Ri.FileText)
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel(zt("ÖZELLİKLER", "FEATURES"))
        Spacer(Modifier.height(8.dp))
        AboutFeature(Ri.Live, zt("M3U / M3U8", "M3U / M3U8"), zt("URL, yerel dosya ve demo liste desteği.", "URL, local file and demo playlist support."))
        AboutFeature(Ri.Heart, zt("Favoriler ve geçmiş", "Favourites and history"), zt("Hızlı erişim için kanalları kaydedin.", "Save channels for quick access."))
        AboutFeature(Ri.Smartphone, zt("Yerel veri", "Local data"), zt("Listeler ve tercihler yalnızca bu cihazda tutulur.", "Playlists and preferences stay on this device."))
        Spacer(Modifier.height(8.dp))
        Text(
            "GPL-3.0-only · © 2026 ZeltaTV contributors",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ShortcutsTab() {
    Card {
        SectionLabel(zt("KUMANDA VE KLAVYE", "REMOTE AND KEYBOARD"))
        Spacer(Modifier.height(10.dp))
        listOf(
            "OK / Enter" to zt("Seç", "Select"),
            "↑ ↓ ← →" to zt("Gezin", "Navigate"),
            "BACK" to zt("Geri", "Back"),
            "MENU" to zt("Ana menü", "Main menu"),
            "0-9" to zt("Kanal numarası ile geç", "Zap by channel number"),
            "CH+ / CH-" to zt("Önceki / sonraki kanal", "Previous / next channel"),
            "+ / -" to zt("Ses azalt / artır", "Volume down / up"),
            "M" to zt("Sesi kapat", "Mute"),
            "Space / K" to zt("Oynat / Duraklat", "Play / Pause"),
            "S" to zt("Durdur", "Stop"),
        ).forEach { (keys, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(keys, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(10.dp))
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingRow(title: String, body: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        control()
    }
}

@Composable
private fun PaletteSwatch(
    name: String,
    background: Color,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(
                if (focused || selected) 2.dp else 1.dp,
                if (focused || selected) zeltaColors.focus else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(11.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent),
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                RiIcon(Ri.Check, size = 12.dp, tint = if (background.isBrightColor()) Color(0xFF111111) else Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            color = if (background.isBrightColor()) Color(0xFF111111) else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AboutFeature(glyph: String, title: String, body: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            RiIcon(glyph, size = 16.dp, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
