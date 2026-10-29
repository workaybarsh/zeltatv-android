package tv.zelta.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import tv.zelta.core.playlist.Channel
import tv.zelta.core.playlist.M3uParser
import tv.zelta.app.R
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun load(source: PlaylistSource): List<Channel> = withContext(Dispatchers.IO) {
        val text = when (source.type) {
            SourceType.DEMO -> context.assets.open("demo.m3u").bufferedReader().use { it.readText() }
            SourceType.FILE -> source.value
            SourceType.URL -> fetch(source.value)
        }
        val channels = M3uParser.parse(text)
        if (channels.isEmpty()) throw PlaylistException(context.getString(R.string.error_no_channels_in_list))
        channels
    }

    suspend fun importFile(uri: Uri): PlaylistSource = withContext(Dispatchers.IO) {
        val name = displayName(uri) ?: context.getString(R.string.default_local_list)
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw PlaylistException(context.getString(R.string.error_file_read))
        if (M3uParser.parse(text).isEmpty()) throw PlaylistException(context.getString(R.string.error_no_channels_in_file))
        PlaylistSource(
            id = UUID.randomUUID().toString(),
            name = name,
            type = SourceType.FILE,
            value = text,
            fileName = name,
        )
    }

    fun isValidUrl(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }

    fun suggestName(value: String): String {
        val host = runCatching { Uri.parse(value).host }.getOrNull()
        return host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: context.getString(R.string.default_playlist_name)
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ZeltaTV/1.0 Android")
            .header("Accept", "*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw PlaylistException(context.getString(R.string.error_server_http, response.code))
            }
            return response.body?.string() ?: throw PlaylistException(context.getString(R.string.error_empty_response))
        }
    }

    private fun displayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}

class PlaylistException(message: String) : IOException(message)
