package tv.zelta.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.ui.components.PanelSurface
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaTextButton
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.zt

@Composable
fun QualityDialog(viewModel: PlayerViewModel, onDismiss: () -> Unit) {
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
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxWidth(0.9f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(zt("Kalite", "Quality"), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        ZeltaTextButton(zt("Kapat", "Close"), onDismiss)
                    }
                    Spacer(Modifier.height(6.dp))
                    SectionLabel(zt("VİDEO İZİ", "VIDEO TRACK"))
                    Spacer(Modifier.height(7.dp))
                    QualityRow(
                        label = zt("Otomatik", "Automatic"),
                        hint = zt("Kaynağın varsayılan akışı", "Source default stream"),
                        selected = !viewModel.highestQuality,
                        modifier = Modifier
                            .focusRequester(firstFocus)
                            .autoFocus(firstFocus),
                    ) {
                        viewModel.applyHighestQuality(false)
                        onDismiss()
                    }
                    val heights = viewModel.videoHeights
                    if (heights.isEmpty()) {
                        Text(
                            zt("Bu yayın için kalite bilgisi henüz yok.", "No quality information for this stream yet."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    } else {
                        heights.forEach { height ->
                            QualityRow(
                                label = "${height}p",
                                hint = when {
                                    height >= 2160 -> "4K"
                                    height >= 1440 -> "QHD"
                                    height >= 1080 -> "Full HD"
                                    height >= 720 -> "HD"
                                    else -> "SD"
                                },
                                selected = viewModel.highestQuality && height == heights.first(),
                            ) {
                                viewModel.applyHighestQuality(false)
                                viewModel.selectQuality(height)
                                onDismiss()
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        zt("Kalite seçimi yayın yeniden başlatıldığında da korunur.", "The quality choice is kept when playback restarts."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityRow(
    label: String,
    hint: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}
