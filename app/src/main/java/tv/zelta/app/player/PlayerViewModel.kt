@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package tv.zelta.app.player

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import tv.zelta.app.R
import tv.zelta.app.data.PlaylistRepository
import tv.zelta.app.data.PlaylistCategory
import tv.zelta.app.data.PlaylistSource
import tv.zelta.app.data.PlaylistStore
import tv.zelta.app.data.SourceType
import tv.zelta.app.settings.AppSettings
import tv.zelta.app.settings.I18n
import tv.zelta.core.playlist.Channel
import java.util.UUID

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val store = PlaylistStore(application)
    private val repository = PlaylistRepository(application)
    private val settings = AppSettings(application)

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val dataSourceFactory = DefaultDataSource.Factory(
        application,
        OkHttpDataSource.Factory(httpClient),
    )

    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
        .apply { playWhenReady = true }

    var sources by mutableStateOf<List<PlaylistSource>>(emptyList())
        private set

    var categories by mutableStateOf<List<PlaylistCategory>>(emptyList())
        private set

    var activeSource by mutableStateOf<PlaylistSource?>(null)
        private set

    var channels by mutableStateOf<List<Channel>>(emptyList())
        private set

    var selected by mutableStateOf<Channel?>(null)
        private set

    var query by mutableStateOf("")
        private set

    var favoritesOnly by mutableStateOf(false)
        private set

    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set

    var history by mutableStateOf<List<String>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Technical ExoPlayer code, shown under the friendly message. */
    var errorDetail by mutableStateOf<String?>(null)
        private set

    var isBuffering by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var highestQuality by mutableStateOf(settings.highestQuality)
        private set

    var videoHeights by mutableStateOf<List<Int>>(emptyList())
        private set

    private var loadJob: Job? = null

    val visibleChannels: List<Channel>
        get() {
            val text = query.trim().lowercase()
            return channels.filter { channel ->
                (!favoritesOnly || favorites.contains(channel.url)) &&
                    (text.isEmpty() || channel.name.lowercase().contains(text))
            }
        }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorDetail = "${error.errorCodeName}: ${error.message ?: str(R.string.error_unknown_playback)}"
                errorMessage = friendlyError(error.errorCode)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null
                    errorDetail = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                videoHeights = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_VIDEO }
                    .flatMap { group ->
                        (0 until group.length).mapNotNull { index ->
                            group.getTrackFormat(index).height.takeIf { height -> height > 0 }
                        }
                    }
                    .distinct()
                    .sortedDescending()
            }
        })

        viewModelScope.launch {
            val stored = store.sources()
            // The built-in demo playlist is gone; drop any leftover copy so the
            // library only ever holds the viewer's own sources.
            val saved = stored.filterNot { it.type == SourceType.DEMO }
            if (saved.size != stored.size) store.saveSources(saved)
            sources = saved
            categories = store.categories()
            favorites = store.favorites()
            history = store.history()
            // Only reopen a source the viewer picked before: on a fresh install
            // the tile library stands on its own, exactly like the desktop app.
            val source = saved.firstOrNull { it.id == store.lastSourceId() }
            if (source != null) loadSource(source, restoreLastChannel = true)
        }
    }

    fun loadSource(source: PlaylistSource, restoreLastChannel: Boolean = false) {
        loadJob?.cancel()
        activeSource = source
        selected = null
        errorMessage = null
        errorDetail = null
        loadError = null
        player.stop()
        player.clearMediaItems()
        channels = emptyList()
        loading = true
        loadJob = viewModelScope.launch {
            try {
                val parsed = repository.load(source)
                channels = parsed
                store.rememberSource(source.id)
                favorites = store.favorites()
                history = store.history()
                if (restoreLastChannel) {
                    val lastUrl = store.lastChannel()
                    val last = parsed.firstOrNull { it.url == lastUrl }
                    if (last != null) play(last)
                }
            } catch (cause: Exception) {
                loadError = cause.message ?: str(R.string.load_failed)
            } finally {
                loading = false
            }
        }
    }

    fun play(channel: Channel) {
        selected = channel
        errorMessage = null
        errorDetail = null
        store.rememberChannel(channel.url)
        history = store.history()
        player.setMediaItem(MediaItem.fromUri(channel.url))
        player.prepare()
        player.play()
    }

    fun retry() {
        selected?.let(::play)
    }

    fun reloadActive() {
        activeSource?.let { loadSource(it) }
    }

    /** Dismisses the last playlist / import error shown on the home screen. */
    fun clearLoadError() {
        loadError = null
    }

    fun stopPlayback() {
        selected = null
        errorMessage = null
        errorDetail = null
        isBuffering = false
        player.stop()
        player.clearMediaItems()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun applyHighestQuality(value: Boolean) {
        settings.updateHighestQuality(value)
        highestQuality = value
        if (value) {
            val highest = videoHeights.maxOrNull()
            if (highest != null) selectQuality(highest)
        } else {
            selectQuality(null)
        }
    }

    fun selectQuality(height: Int?) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (height == null) {
            builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            builder.setMinVideoSize(0, 0)
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, height)
            builder.setMinVideoSize(0, height)
        }
        player.trackSelectionParameters = builder.build()
    }

    fun toggleFavorite(channel: Channel) {
        favorites = store.toggleFavorite(channel.url)
    }

    fun isFavorite(channel: Channel): Boolean = favorites.contains(channel.url)

    fun updateQuery(value: String) {
        query = value
    }

    fun toggleFavoritesOnly() {
        favoritesOnly = !favoritesOnly
    }

    fun addUrlSource(url: String, alias: String, categoryId: String? = null): Boolean {
        val value = url.trim()
        if (!repository.isValidUrl(value)) {
            loadError = str(R.string.error_invalid_url)
            return false
        }
        val source = PlaylistSource(
            id = UUID.randomUUID().toString(),
            name = alias.trim().ifBlank { repository.suggestName(value) },
            type = SourceType.URL,
            value = value,
            categoryIds = listOfNotNull(categoryId),
        )
        store.addOrReplace(source)
        sources = store.sources()
        loadSource(source)
        return true
    }

    fun importFile(uri: Uri, categoryId: String? = null) {
        loadJob?.cancel()
        loadError = null
        loading = true
        loadJob = viewModelScope.launch {
            try {
                val source = repository.importFile(uri)
                val categorized = source.copy(categoryIds = listOfNotNull(categoryId))
                store.addOrReplace(categorized)
                sources = store.sources()
                loadJob = null
                loadSource(categorized)
            } catch (cause: Exception) {
                loadError = cause.message ?: str(R.string.error_import_failed)
                loading = false
            }
        }
    }

    fun addCategory(name: String): PlaylistCategory? {
        val category = store.addCategory(name)
        categories = store.categories()
        return category
    }

    fun deleteCategory(id: String) {
        store.deleteCategory(id)
        categories = store.categories()
        sources = store.sources()
    }

    fun setSourceCategories(id: String, categoryIds: List<String>) {
        store.setSourceCategories(id, categoryIds)
        sources = store.sources()
    }

    fun renameSource(source: PlaylistSource, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val updated = source.copy(name = trimmed)
        store.addOrReplace(updated)
        sources = store.sources()
        if (activeSource?.id == source.id) activeSource = updated
    }

    /** Rename a source and, for link sources, change its address too. */
    fun updateSource(source: PlaylistSource, name: String, value: String) {
        val trimmed = name.trim().ifBlank { source.name }
        val address = value.trim()
        if (source.type == SourceType.URL) {
            if (!repository.isValidUrl(address)) {
                loadError = str(R.string.error_invalid_url)
                return
            }
        }
        val updated = source.copy(
            name = trimmed,
            value = if (source.type == SourceType.URL) address else source.value,
            fileName = if (source.type == SourceType.URL) null else source.fileName,
        )
        store.addOrReplace(updated)
        sources = store.sources()
        if (activeSource?.id == source.id) activeSource = updated
    }

    fun categoryNames(source: PlaylistSource): String =
        categories.filter { it.id in source.categoryIds }.joinToString(", ") { it.name }

    fun removeSource(source: PlaylistSource) {
        store.removeSource(source.id)
        sources = store.sources()
        if (activeSource?.id == source.id) {
            stopPlayback()
            channels = emptyList()
            val next = sources.firstOrNull()
            activeSource = next
            if (next != null) loadSource(next) else loading = false
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        player.release()
        super.onCleared()
    }

    private fun str(id: Int, vararg args: Any): String = getApplication<Application>().getString(id, *args)

    /** Turns an ExoPlayer code into a sentence a viewer can act on. */
    private fun friendlyError(code: Int): String = when (code) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        -> localize(
            "Bu kanala ulaşılamıyor. Adresi veya bağlantınızı kontrol edin.",
            "This channel is not reachable. Check the address or your connection.",
        )

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        -> localize(
            "Bu yayın biçimi desteklenmiyor.",
            "This stream format is not supported.",
        )

        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        PlaybackException.ERROR_CODE_DRM_UNSPECIFIED,
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
        -> localize(
            "Yayın, kullanılamayan bir DRM anahtarı gerektiriyor.",
            "The stream needs a DRM key that is not available.",
        )

        else -> localize(
            "Oynatma başarısız oldu. Başka bir kanal deneyin.",
            "Playback failed. Try another channel.",
        )
    }

    private fun localize(turkish: String, english: String): String {
        val language = settings.language
        return I18n.translate(getApplication(), language, english)
            ?: if (language == "tr") turkish else english
    }
}
