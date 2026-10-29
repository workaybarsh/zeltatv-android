package tv.zelta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.ui.components.ChannelLogo
import tv.zelta.app.ui.components.PanelSurface
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaTextField
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.zt
import tv.zelta.core.playlist.Channel

/** Satellite-receiver style channel menu opened with OK while watching. */
@Composable
fun ChannelMenuDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val query = search.trim().lowercase()
    val items = remember(viewModel.channels, query) {
        if (query.isEmpty()) viewModel.channels
        else viewModel.channels.filter {
            it.name.lowercase().contains(query) || it.group?.lowercase()?.contains(query) == true
        }
    }
    val listState = rememberLazyListState()
    val currentIndex = items.indexOfFirst { it.id == viewModel.selected?.id }.coerceAtLeast(0)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (items.isNotEmpty()) {
            listState.scrollToItem(currentIndex)
            delay(90)
            runCatching { focusRequester.requestFocus() }
        }
    }

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
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.86f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            zt("Kanallar", "Channels"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${items.size}/${viewModel.channels.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ZeltaTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = zt("Kanal ara…", "Search channels…"),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(9.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f), RoundedCornerShape(7.dp)),
                    ) {
                        if (items.isEmpty()) {
                            Text(
                                zt("Kanal bulunamadı.", "No channels found."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(items, key = { _, channel -> channel.id }) { index, channel ->
                                    val active = channel.id == viewModel.selected?.id
                                    ChannelMenuRow(
                                        channel = channel,
                                        number = viewModel.channels.indexOfFirst { it.id == channel.id } + 1,
                                        active = active,
                                        favorite = viewModel.isFavorite(channel),
                                        onClick = {
                                            viewModel.play(channel)
                                            onDismiss()
                                        },
                                        modifier = if (index == currentIndex) {
                                            Modifier
                                                .focusRequester(focusRequester)
                                                .autoFocus(focusRequester)
                                        } else Modifier,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    SectionLabel(
                        zt("OK oynat · BACK kapat", "OK play · BACK close"),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelMenuRow(
    channel: Channel,
    number: Int,
    active: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                when {
                    active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                    else -> Color.Transparent
                },
            )
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(5.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            number.toString().padStart(2, '0'),
            color = if (active || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(24.dp),
        )
        ChannelLogo(channel, Modifier.size(26.dp))
        Text(
            channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (favorite) {
            Text("★", color = Color(0xFFFFC857), fontSize = 11.sp)
        }
        if (active) {
            Icon(
                Icons.Filled.VolumeUp,
                contentDescription = zt("Şu an oynuyor", "Now playing"),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
