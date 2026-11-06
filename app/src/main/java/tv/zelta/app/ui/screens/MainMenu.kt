package tv.zelta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tv.zelta.app.ui.components.Keycap
import tv.zelta.app.ui.components.PanelSurface
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.clockText
import tv.zelta.app.ui.components.dateText
import tv.zelta.app.ui.components.channelCountText
import tv.zelta.app.ui.components.zt
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon
import tv.zelta.app.ui.theme.zeltaColors

/** Desktop main menu: six tiles in a 3x2 grid, live clock and remote hints. */
@Composable
fun MainMenuDialog(
    channelCount: Int,
    onDismiss: () -> Unit,
    onSources: () -> Unit,
    onChannels: () -> Unit,
    onSettings: () -> Unit,
    onShortcuts: () -> Unit,
    onAppearance: () -> Unit,
    onClose: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .focusProperties { canFocus = false }
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 768.dp)
                    .fillMaxWidth(0.96f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column {
                    // header: px-5 py-3
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "ZeltaTV",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                clockText(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(dateText(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }

                    BoxWithConstraints(Modifier.padding(16.dp)) {
                        val narrow = maxWidth < 540.dp
                        val tiles = listOf(
                            MenuTile("sources", Ri.PlayList, zt("Kaynaklar", "Sources"), zt("Oynatma listelerini ekle ve yönet", "Add and manage playlists"), onSources),
                            MenuTile("channels", Ri.ListCheck, zt("Kanal listesi", "Channel list"), channelCountText(channelCount), onChannels),
                            MenuTile("settings", Ri.Settings, zt("Ayarlar", "Settings"), zt("Oynatma, tema, dil", "Playback, theme, language"), onSettings),
                            MenuTile("shortcuts", Ri.Keyboard, zt("Kısayollar", "Shortcuts"), zt("Kumanda ve klavye tuşları", "Remote and keyboard keys"), onShortcuts),
                            MenuTile("theme", Ri.Palette, zt("Tema", "Theme"), zt("Renk paletleri", "Colour palettes"), onAppearance),
                            MenuTile("close", Ri.CloseCircle, zt("Kapat", "Close"), zt("Oynatıcıya dön", "Back to the player"), onClose),
                        )
                        val columns = if (narrow) 2 else 3
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            tiles.chunked(columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEachIndexed { index, tile ->
                                        MenuTileView(
                                            tile = tile,
                                            modifier = Modifier
                                                .weight(1f)
                                                .then(
                                                    if (tile.key == "live") {
                                                        Modifier
                                                            .focusRequester(firstFocus)
                                                            .autoFocus(firstFocus)
                                                    } else Modifier,
                                                ),
                                        )
                                    }
                                    repeat(columns - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Keycap("OK")
                        Text(zt("Seç", "Select"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Keycap("↑↓←→")
                        Text(zt("Gezin", "Navigate"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Keycap("Esc")
                        Text(zt("Kapat", "Close"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private data class MenuTile(
    val key: String,
    val glyph: String,
    val label: String,
    val hint: String,
    val action: () -> Unit,
)

@Composable
private fun MenuTileView(tile: MenuTile, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier
            .height(116.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) zeltaColors.focus.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) zeltaColors.focus else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                RoundedCornerShape(8.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = tile.action)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        RiIcon(tile.glyph, size = 24.dp, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(
                tile.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tile.hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
