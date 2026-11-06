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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.focusRequester
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

/**
 * Full-bleed Media3 surface with the desktop receiver overlay: channel banner,
 * buffering spinner, playback errors and numeric channel entry. Playback has no
 * on-screen transport controls: Up/Down zap, OK opens the channel menu and Back
 * pauses and leaves the player.
 */
@Composable
fun PlayerPane(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onOpenChannels: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSources: () -> Unit,
) {
    val playerFocus = remember { FocusRequester() }
    var bannerVisible by remember { mutableStateOf(false) }
    var channelDigits by remember { mutableStateOf("") }
    val selected = viewModel.selected

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
            delay(2600)
            bannerVisible = false
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
                    // Left/Right do nothing while watching: no seeking and no
                    // pausing. The broadcast simply keeps playing.
                    event.key == Key.DirectionLeft || event.key == Key.DirectionRight -> true
                    // Up/Down always zaps between channels.
                    event.key == Key.DirectionUp -> {
                        zap(1)
                        true
                    }
                    event.key == Key.DirectionDown -> {
                        zap(-1)
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
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
                val number = remember(channel.id, viewModel.channels) {
                    viewModel.channels.indexOfFirst { it.id == channel.id } + 1
                }
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

