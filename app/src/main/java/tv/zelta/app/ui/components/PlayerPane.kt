@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package tv.zelta.app.ui.components

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.core.playlist.Channel
import kotlin.math.abs

/**
 * Full-bleed Media3 surface with the desktop receiver overlay: channel banner,
 * auto-hiding transport controls, quality indicator, seek feedback and numeric
 * channel entry.
 */
@Composable
fun PlayerPane(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    fullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onOpenChannels: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenQuality: () -> Unit,
) {
    val playerFocus = remember { FocusRequester() }
    var controlsVisible by remember { mutableStateOf(true) }
    var bannerVisible by remember { mutableStateOf(false) }
    var seekHint by remember { mutableStateOf<String?>(null) }
    var channelDigits by remember { mutableStateOf("") }
    var muted by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val selected = viewModel.selected

    fun showControls() {
        controlsVisible = true
    }

    fun seek(delta: Long) {
        val target = (viewModel.player.currentPosition + delta * 1000L).coerceAtLeast(0L)
        viewModel.player.seekTo(target)
        seekHint = if (delta > 0) "+${delta}s" else "${delta}s"
        showControls()
    }

    fun zap(delta: Int) {
        val list = viewModel.channels
        if (list.isEmpty()) return
        val current = selected?.let { list.indexOfFirst { ch -> ch.id == it.id } } ?: 0
        val next = (current + delta).mod(list.size)
        viewModel.play(list[next])
    }

    LaunchedEffect(selected?.id) {
        if (selected != null) {
            runCatching { playerFocus.requestFocus() }
            bannerVisible = true
            controlsVisible = true
            delay(2600)
            bannerVisible = false
        }
    }

    LaunchedEffect(controlsVisible, viewModel.isPlaying, selected?.id) {
        if (controlsVisible && viewModel.isPlaying) {
            delay(4200)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekHint) {
        if (seekHint != null) {
            delay(1100)
            seekHint = null
        }
    }

    LaunchedEffect(channelDigits) {
        if (channelDigits.isNotEmpty()) {
            delay(1800)
            val number = channelDigits.toIntOrNull()
            channelDigits = ""
            val channel = number?.let { viewModel.channels.getOrNull(it - 1) }
            if (channel != null) viewModel.play(channel)
        }
    }

    LaunchedEffect(selected?.id, viewModel.isPlaying) {
        while (selected != null) {
            position = viewModel.player.currentPosition.coerceAtLeast(0L)
            duration = viewModel.player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .autoFocus(playerFocus)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = when (event.key) {
                    Key.Zero -> 0
                    Key.One -> 1
                    Key.Two -> 2
                    Key.Three -> 3
                    Key.Four -> 4
                    Key.Five -> 5
                    Key.Six -> 6
                    Key.Seven -> 7
                    Key.Eight -> 8
                    Key.Nine -> 9
                    else -> null
                }
                when {
                    event.key == Key.DirectionCenter || event.key == Key.Enter -> {
                        onOpenChannels()
                        true
                    }
                    digit != null -> {
                        channelDigits += digit.toString()
                        showControls()
                        true
                    }
                    event.key == Key.MediaPlayPause -> {
                        viewModel.togglePlayPause()
                        showControls()
                        true
                    }
                    event.key == Key.MediaNext -> {
                        zap(1)
                        true
                    }
                    event.key == Key.MediaPrevious -> {
                        zap(-1)
                        true
                    }
                    // Physical TV remotes send CHANNEL_UP/DOWN for CH+ / CH-.
                    event.key == Key.ChannelUp -> {
                        zap(1)
                        true
                    }
                    event.key == Key.ChannelDown -> {
                        zap(-1)
                        true
                    }
                    event.key == Key.Menu -> {
                        onOpenMenu()
                        true
                    }
                    event.key == Key.DirectionLeft && fullscreen -> {
                        seek(-10)
                        true
                    }
                    event.key == Key.DirectionRight && fullscreen -> {
                        seek(10)
                        true
                    }
                    event.key == Key.DirectionUp || event.key == Key.DirectionDown -> {
                        showControls()
                        false
                    }
                    else -> false
                }
            }
            .focusable()
            .clickable {
                // touch: first tap reveals the transport bar, second tap hides it
                controlsVisible = !controlsVisible
            },
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    isFocusable = false
                    isClickable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                }
            },
            update = { it.player = viewModel.player },
            modifier = Modifier.fillMaxSize(),
        )

        if (selected == null) {
            EmptyPlayer(onOpenSources, onOpenChannels, Modifier.align(Alignment.Center))
        }

        // channel banner
        AnimatedVisibility(
            visible = bannerVisible && selected != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            selected?.let { channel ->
                val number = viewModel.channels.indexOfFirst { it.id == channel.id } + 1
                Row(
                    Modifier
                        .padding(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.Black.copy(alpha = 0.82f))
                        .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(7.dp)),
                ) {
                    if (number > 0) {
                        Box(
                            Modifier
                                .width(62.dp)
                                .height(54.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                number.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            channel.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!channel.group.isNullOrBlank()) {
                                Text(channel.group ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Spacer(Modifier.width(6.dp))
                            }
                            val quality = viewModel.videoHeights.firstOrNull()
                            if (quality != null) {
                                Text("${quality}p", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            } else if (viewModel.highestQuality) {
                                Text("HQ", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // top-right source/clock
        if (selected != null) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                viewModel.activeSource?.let { source ->
                    Text(
                        source.name,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (viewModel.isBuffering && selected != null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp),
            )
        }

        if (!viewModel.isPlaying && !viewModel.isBuffering && selected != null && viewModel.errorMessage == null) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = zt("Oynat", "Play"), tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }

        seekHint?.let { hint ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(hint, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // numeric channel entry
        if (channelDigits.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    channelDigits.padStart(2, '0'),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        val error = viewModel.errorMessage
        if (error != null) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(zt("Oynatma hatası", "Playback error"), color = MaterialTheme.colorScheme.error, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZeltaButton(zt("Tekrar dene", "Retry"), viewModel::retry, primary = true, icon = Icons.Filled.Refresh)
                    ZeltaButton(zt("Kanallar", "Channels"), onOpenChannels)
                }
                val detail = viewModel.errorDetail
                if (detail != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && selected != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                        ),
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(formatTime(position), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                    Slider(
                        value = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            if (duration > 0) {
                                viewModel.player.seekTo((fraction * duration).toLong())
                                position = (fraction * duration).toLong()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        ),
                    )
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerAction(
                        icon = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        description = if (viewModel.isPlaying) zt("Duraklat", "Pause") else zt("Oynat", "Play"),
                    ) { viewModel.togglePlayPause(); showControls() }
                    PlayerAction(icon = Icons.Filled.Stop, description = zt("Durdur", "Stop")) {
                        viewModel.stopPlayback()
                    }
                    PlayerTextAction("-10", zt("10 sn geri", "Rewind 10s")) { seek(-10) }
                    PlayerTextAction("+10", zt("10 sn ileri", "Forward 10s")) { seek(10) }
                    PlayerAction(
                        icon = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        description = zt("Ses", "Volume"),
                    ) {
                        muted = !muted
                        viewModel.player.volume = if (muted) 0f else 1f
                        showControls()
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        selected?.name ?: "",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    PlayerAction(
                        icon = Icons.Filled.Fullscreen,
                        description = if (fullscreen) zt("Tam ekrandan çık", "Exit fullscreen") else zt("Tam ekran", "Fullscreen"),
                    ) { onToggleFullscreen() }
                    val quality = viewModel.videoHeights.firstOrNull()
                    ZeltaTextButton(
                        if (quality != null) "${quality}p" else zt("Kalite", "Quality"),
                        onOpenQuality,
                        color = Color.White,
                    )
                    PlayerAction(icon = Icons.Filled.List, description = zt("Kanal listesi", "Channel list")) {
                        onOpenChannels()
                    }
                    PlayerAction(icon = Icons.Filled.Favorite, description = zt("Favori", "Favourite")) {
                        selected?.let(viewModel::toggleFavorite)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun PlayerTextAction(text: String, description: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyPlayer(
    onOpenSources: () -> Unit,
    onOpenChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark(markSize = 54.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            zt("Kanal seçin", "Choose a channel"),
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            zt("Kanallar menüsünden yayın seçin.", "Pick a stream from the channels menu."),
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZeltaButton(zt("Kanallar", "Channels"), onOpenChannels, primary = true)
            ZeltaButton(zt("Kaynak ekle", "Add source"), onOpenSources)
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
