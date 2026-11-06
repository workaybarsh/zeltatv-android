package tv.zelta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import tv.zelta.app.ui.components.zt
import tv.zelta.core.playlist.Channel

/** Satellite-receiver style channel menu opened with OK while watching. */
@OptIn(ExperimentalFoundationApi::class)
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
    // Pre-compute channel numbers once instead of running indexOfFirst for
    // every visible row (that was O(rows × channels) on large playlists).
    val channelNumbers = remember(viewModel.channels) {
        HashMap<String, Int>(viewModel.channels.size).apply {
            viewModel.channels.forEachIndexed { index, channel -> put(channel.id, index + 1) }
        }
    }
    val currentIndex = remember(items, viewModel.selected?.id) {
        items.indexOfFirst { it.id == viewModel.selected?.id }.coerceAtLeast(0)
    }
    val focusRequester = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (items.isNotEmpty()) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocus),
                    )
                    Spacer(Modifier.height(9.dp))
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
                            // Left/Right from the list jumps back to the search
                            // field, so filtering is one key away on a remote.
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                                ) {
                                    runCatching { searchFocus.requestFocus() }
                                    true
                                } else {
                                    false
                                }
                            },
                    ) {
                        // Centre the playing channel on the very first frame.
                        // The vertical content padding equals half of the empty
                        // space, so scrollToItem(index) puts that row in the
                        // middle of the viewport instead of at the top. The
                        // state starts at the same index, so there is no
                        // top-first-then-scroll jump.
                        val centerPadding = ((maxHeight - 44.dp) / 2).coerceAtLeast(0.dp)
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
                        LaunchedEffect(currentIndex) {
                            listState.scrollToItem(currentIndex)
                        }
                        LaunchedEffect(query) {
                            if (query.isNotEmpty()) listState.scrollToItem(0)
                        }
                        if (items.isEmpty()) {
                            Text(
                                zt("Kanal bulunamadı.", "No channels found."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            // The platform's focus scrolling centres the focused
                            // row inside the whole window, which made the list
                            // slide a few dozen pixels right after opening.
                            // The minimal spec only scrolls when a row would
                            // actually leave the viewport, so the centred
                            // first frame stays put.
                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides MinimalBringIntoViewSpec,
                            ) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(vertical = centerPadding),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    itemsIndexed(
                                        items = items,
                                        key = { _, channel -> channel.id },
                                        contentType = { _, _ -> "channel" },
                                    ) { index, channel ->
                                        val active = channel.id == viewModel.selected?.id
                                        ChannelMenuRow(
                                            channel = channel,
                                            number = channelNumbers[channel.id] ?: 0,
                                            active = active,
                                            favorite = viewModel.isFavorite(channel),
                                            onClick = {
                                                viewModel.play(channel)
                                                onDismiss()
                                            },
                                            // Only attach the requester here. The
                                            // dialog focuses the current row once
                                            // when it opens; re-requesting focus
                                            // from the row would steal it back from
                                            // the search field while filtering and
                                            // close the keyboard mid-word.
                                            modifier = if (index == currentIndex) {
                                                Modifier.focusRequester(focusRequester)
                                            } else Modifier,
                                        )
                                    }
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

/**
 * Bring-into-view spec that only scrolls when the focused row would actually
 * leave the viewport. The TV platform default re-centres the row inside the
 * whole window, which made the list slide right after the menu opened.
 */
@OptIn(ExperimentalFoundationApi::class)
private val MinimalBringIntoViewSpec = object : BringIntoViewSpec {}

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
