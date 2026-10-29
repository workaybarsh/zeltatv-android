package tv.zelta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon


/** Remote/keyboard key map, matching the desktop shortcuts dialog. */
@Composable
fun ShortcutsDialog(onDismiss: () -> Unit) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.62f))
                .focusProperties { canFocus = false }
                .clickable(onClick = onDismiss)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(0.94f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RiIcon(Ri.Keyboard, size = 16.dp, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            zt("Kısayollar", "Shortcuts"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        ZeltaRiButton(Ri.Close, onDismiss, zt("Kapat", "Close"), size = 28.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        ShortcutRow("OK / Enter", zt("Seç", "Select"), firstFocus)
                        ShortcutRow("↑ ↓ ← →", zt("Gezin", "Navigate"))
                        ShortcutRow("BACK", zt("Geri", "Back"))
                        ShortcutRow("MENU", zt("Ana menü", "Main menu"))
                        ShortcutRow("0-9", zt("Kanal numarası ile geç", "Zap by channel number"))
                        ShortcutRow("CH+ / CH-", zt("Önceki / sonraki kanal", "Previous / next channel"))
                        ShortcutRow("+ / -", zt("Ses azalt / artır", "Volume down / up"))
                        ShortcutRow("M", zt("Sesi kapat", "Mute"))
                        ShortcutRow("Space / K", zt("Oynat / Duraklat", "Play / Pause"))
                        ShortcutRow("S", zt("Durdur", "Stop"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(keys: String, label: String, focusRequester: FocusRequester? = null) {
    var focused = false
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Keycap(keys)
        Spacer(Modifier.width(10.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
