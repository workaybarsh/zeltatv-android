package tv.zelta.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.zelta.app.BuildConfig
import tv.zelta.app.ui.components.BrandLockup
import tv.zelta.app.ui.components.PanelSurface
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaButton
import tv.zelta.app.ui.components.ZeltaIconButton
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.zt

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val firstFocus = remember { FocusRequester() }
    fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, context.getString(android.R.string.no), Toast.LENGTH_SHORT).show() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZeltaIconButton(Icons.Filled.ArrowBack, onBack, zt("Geri", "Back"))
            Spacer(Modifier.width(6.dp))
            Text(zt("Hakkında", "About"), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PanelSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        BrandLockup()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            zt(
                                "Sürüm {version} · Android",
                                "Version {version} · Android",
                            ).replace("{version}", BuildConfig.VERSION_NAME),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            zt(
                                "ZeltaTV, kendi M3U listelerinizi TV, tablet ve telefonda izlemeniz için tasarlanmış açık kaynak bir IPTV oynatıcıdır.",
                                "ZeltaTV is an open-source IPTV player built to watch your own M3U playlists on TV, tablet and phone.",
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ZeltaButton(
                                "GitHub",
                                { open("https://github.com/zeltatv/android") },
                                modifier = Modifier
                                    .focusRequester(firstFocus)
                                    .autoFocus(firstFocus),
                                primary = true,
                                icon = Icons.Filled.Code,
                            )
                            ZeltaButton(zt("Lisans", "Licence"), { open("https://www.gnu.org/licenses/gpl-3.0.html") }, icon = Icons.Filled.Description)
                        }
                    }
                }

                PanelSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        SectionLabel(zt("ÖZELLİKLER", "FEATURES"))
                        Spacer(Modifier.height(9.dp))
                        Feature(Icons.Filled.LiveTv, zt("M3U / M3U8", "M3U / M3U8"), zt("URL, yerel dosya ve demo liste desteği.", "URL, local file and demo playlist support."))
                        Feature(Icons.Filled.Favorite, zt("Favoriler ve geçmiş", "Favourites and history"), zt("Hızlı erişim için kanalları kaydedin.", "Save channels for quick access."))
                        Feature(Icons.Filled.Security, zt("Yerel veri", "Local data"), zt("Listeler ve tercihler yalnızca bu cihazda tutulur.", "Playlists and preferences stay on this device."))
                    }
                }

                PanelSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        SectionLabel(zt("LİSANS", "LICENCE"))
                        Spacer(Modifier.height(7.dp))
                        Text(
                            zt(
                                "Bu uygulama GNU Genel Kamu Lisansı v3.0 (GPL-3.0-only) koşulları altında dağıtılır. Kaynak kodu GitHub deposunda bulunur.",
                                "This app is distributed under the GNU General Public License v3.0 (GPL-3.0-only). Source code is available in the GitHub repository.",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "© 2026 ZeltaTV contributors",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}
