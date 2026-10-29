package tv.zelta.app.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import tv.zelta.app.R
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon
import tv.zelta.app.ui.theme.isBrightColor
import tv.zelta.app.ui.theme.zeltaColors
import tv.zelta.core.playlist.Channel

private enum class SidebarFilter { ALL, FAVORITES, RECENT }
private enum class SidebarView { GROUPS, DEFAULT, ALPHA }

private sealed interface SidebarRow {
    val key: String
    data class Header(val label: String, override val key: String) : SidebarRow
    data class Entry(val channel: Channel, override val key: String) : SidebarRow
}

/**
 * Desktop channel rail: 256dp wide, 36dp header, view toggles, fullscreen button,
 * search row and 40/48dp rows. Matches Sidebar.svelte.
 */
@Composable
fun ChannelSidebar(
    viewModel: PlayerViewModel,
    onChannelSelected: (Channel) -> Unit = viewModel::play,
    onOpenSources: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(SidebarFilter.ALL) }
    var view by remember { mutableStateOf(SidebarView.DEFAULT) }
    val listState = rememberLazyListState()
    val ungroupedLabel = zt("Grupsuz", "Ungrouped")

    val rows = remember(
        viewModel.channels,
        viewModel.query,
        viewModel.favorites,
        viewModel.history,
        filter,
        view,
        ungroupedLabel,
    ) {
        buildRows(viewModel, filter, view, ungroupedLabel)
    }
    val firstEntryKey = rows.firstOrNull { it is SidebarRow.Entry }?.key
    val channelCount = rows.count { it is SidebarRow.Entry }
    val firstFocus = remember { FocusRequester() }
    var hasFocused by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.channels, viewModel.selected) {
        if (!hasFocused && rows.any { it is SidebarRow.Entry }) {
            hasFocused = true
            runCatching { firstFocus.requestFocus() }
        }
        val selectedIndex = viewModel.selected?.let { selected ->
            rows.indexOfFirst { it is SidebarRow.Entry && it.channel.id == selected.id }
        } ?: -1
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }

    Column(
        modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    ) {
        // header (h-9 px-3)
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                zt("Kanallar", "Channels"),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (channelCount != viewModel.channels.size) "$channelCount/${viewModel.channels.size}"
                else channelCount.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // view modes (px-1.5 py-1, three flex-1 toggles)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ViewModeButton(Ri.Folders, zt("Kategoriye göre grupla", "Group by category"), view == SidebarView.GROUPS, Modifier.weight(1f)) {
                view = SidebarView.GROUPS
            }
            ViewModeButton(Ri.ListUnordered, zt("Varsayılan sıra (grupsuz)", "Default order (no groups)"), view == SidebarView.DEFAULT, Modifier.weight(1f)) {
                view = SidebarView.DEFAULT
            }
            ViewModeButton(Ri.SortAlpha, zt("Alfabetik sıra", "Alphabetical order"), view == SidebarView.ALPHA, Modifier.weight(1f)) {
                view = SidebarView.ALPHA
            }
        }

        // fullscreen (p-1.5, h-8 button)
        Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
            SidebarAction(
                glyph = Ri.Fullscreen,
                label = zt("Tam ekran", "Fullscreen"),
                onClick = onFullscreen,
            )
        }

        // search + quick filters (p-1.5 gap-1.5)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.weight(1f)) {
                ZeltaTextField(
                    value = viewModel.query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = zt("Kanal ara…", "Search channels…"),
                    modifier = Modifier.fillMaxWidth(),
                    leadingGlyph = Ri.Search,
                )
            }
            QuickFilter(
                glyph = if (filter == SidebarFilter.FAVORITES) Ri.HeartFill else Ri.Heart,
                label = if (viewModel.favorites.isEmpty()) null else viewModel.favorites.size.toString(),
                active = filter == SidebarFilter.FAVORITES,
                description = zt("Favorileri göster", "Show favourites"),
            ) {
                filter = if (filter == SidebarFilter.FAVORITES) SidebarFilter.ALL else SidebarFilter.FAVORITES
            }
            QuickFilter(
                glyph = Ri.History,
                label = if (viewModel.history.isEmpty()) null else viewModel.history.size.toString(),
                active = filter == SidebarFilter.RECENT,
                description = zt("Son izlenenleri göster", "Show recently watched"),
            ) {
                filter = if (filter == SidebarFilter.RECENT) SidebarFilter.ALL else SidebarFilter.RECENT
            }
        }

        // body
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
        ) {
            when {
                viewModel.loading -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(zt("Liste yükleniyor…", "Loading playlist…"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }

                viewModel.loadError != null -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(zt("Liste yüklenemedi", "Could not load playlist"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text(viewModel.loadError ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ZeltaButton(zt("Tekrar dene", "Try again"), viewModel::reloadActive)
                        ZeltaButton(zt("Kaynaklar", "Sources"), onOpenSources, glyph = Ri.PlayList)
                    }
                }

                rows.isEmpty() -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        when {
                            viewModel.channels.isEmpty() -> zt("Henüz kanal yok", "No channels")
                            filter == SidebarFilter.FAVORITES -> zt("Favori kanal yok", "No favourites yet")
                            filter == SidebarFilter.RECENT -> zt("Henüz izlenen kanal yok", "No recently watched channels yet")
                            else -> zt("Kanal bulunamadı", "No channels found")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (viewModel.channels.isEmpty()) {
                        ZeltaButton(zt("Kaynak ekle", "Add source"), onOpenSources, primary = true, glyph = Ri.Add)
                    } else {
                        ZeltaButton(zt("Tüm kanallar", "Show all"), { filter = SidebarFilter.ALL })
                    }
                }

                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            is SidebarRow.Header -> Text(
                                row.label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .padding(start = 8.dp, top = 8.dp),
                            )

                            is SidebarRow.Entry -> {
                                val channel = row.channel
                                ChannelRow(
                                    channel = channel,
                                    selected = viewModel.selected?.id == channel.id,
                                    favorite = viewModel.isFavorite(channel),
                                    onSelect = { onChannelSelected(channel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                                    modifier = if (row.key == firstEntryKey) {
                                        Modifier.focusRequester(firstFocus)
                                    } else Modifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeButton(
    glyph: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else if (focused) zeltaColors.focus.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) zeltaColors.focus else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RiIcon(
            glyph,
            size = 14.dp,
            tint = if (selected || focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SidebarAction(glyph: String, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) zeltaColors.focus.copy(alpha = 0.10f) else MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) zeltaColors.focus else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        RiIcon(glyph, size = 14.dp, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(7.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickFilter(
    glyph: String,
    label: String?,
    active: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (active) MaterialTheme.colorScheme.surfaceVariant
                else if (focused) zeltaColors.focus.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) zeltaColors.focus else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiIcon(
            glyph,
            size = 14.dp,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (label != null) {
            Spacer(Modifier.width(4.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.surfaceVariant
                    focused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    else -> Color.Transparent
                }
            )
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) zeltaColors.focus else Color.Transparent,
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onSelect)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelLogo(channel, Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center,
        ) {
            RiIcon(
                if (favorite) Ri.HeartFill else Ri.Heart,
                size = 14.dp,
                tint = if (favorite) Color(0xFFFF6B81) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun ChannelLogo(channel: Channel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(4.dp)
    if (channel.logo.isNullOrBlank()) {
        LogoFallback(channel.name, modifier, shape)
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(channel.logo).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
            loading = { LogoFallback(channel.name, Modifier.fillMaxSize(), shape) },
            error = { LogoFallback(channel.name, Modifier.fillMaxSize(), shape) },
        )
    }
}

@Composable
private fun LogoFallback(name: String, modifier: Modifier, shape: RoundedCornerShape) {
    val initials = remember(name) {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].first().toString() + words[1].first().toString()).uppercase()
        }
    }
    val hash = remember(name) { name.fold(0) { acc, ch -> (acc * 31 + ch.code) % 1000 } }
    val tints = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        Color(0xFF7C8CFF).copy(alpha = 0.20f),
        Color(0xFF5CE0A0).copy(alpha = 0.18f),
        Color(0xFFD0A2FF).copy(alpha = 0.18f),
    )
    Box(
        modifier
            .clip(shape)
            .background(tints[hash % tints.size])
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

private fun buildRows(
    viewModel: PlayerViewModel,
    filter: SidebarFilter,
    view: SidebarView,
    ungroupedLabel: String,
): List<SidebarRow> {
    val query = viewModel.query.trim().lowercase()
    val historyChannels = viewModel.history.mapNotNull { url -> viewModel.channels.firstOrNull { it.url == url } }
    val base = when (filter) {
        SidebarFilter.ALL -> viewModel.channels
        SidebarFilter.FAVORITES -> viewModel.channels.filter { it.url in viewModel.favorites }
        SidebarFilter.RECENT -> historyChannels
    }.filter { query.isEmpty() || it.name.lowercase().contains(query) || it.group?.lowercase()?.contains(query) == true }

    return when (view) {
        SidebarView.DEFAULT -> base.map { SidebarRow.Entry(it, it.id) }
        SidebarView.ALPHA -> base.sortedBy { it.name.lowercase() }.map { SidebarRow.Entry(it, it.id) }
        SidebarView.GROUPS -> {
            val grouped = base.groupBy { it.group?.trim()?.takeIf { g -> g.isNotEmpty() } ?: ungroupedLabel }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            buildList {
                grouped.forEach { (group, channels) ->
                    add(SidebarRow.Header(group, "h-$group"))
                    channels.forEach { add(SidebarRow.Entry(it, it.id)) }
                }
            }
        }
    }
}
