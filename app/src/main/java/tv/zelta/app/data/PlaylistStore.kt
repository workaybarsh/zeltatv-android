package tv.zelta.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Room-backed library. The M1 UI is intentionally synchronous: these lists are
 * small and every write is user-driven. Moving DAO calls onto Dispatchers.IO is
 * part of the M2 repository split.
 */
class PlaylistStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dao = ZeltaDatabase.get(appContext).libraryDao()

    init {
        migrateLegacyPreferences()
    }

    fun sources(): List<PlaylistSource> = dao.sources().map { entity ->
        PlaylistSource(
            id = entity.id,
            name = entity.name,
            type = runCatching { SourceType.valueOf(entity.type) }.getOrDefault(SourceType.URL),
            value = entity.value,
            fileName = entity.fileName.ifBlank { null },
            categoryIds = parseIds(entity.categoryIds),
            createdAt = entity.createdAt,
        )
    }

    fun saveSources(value: List<PlaylistSource>) {
        dao.clearSources()
        value.forEach { source ->
            dao.upsertSource(
                SourceEntity(
                    id = source.id,
                    name = source.name,
                    type = source.type.name,
                    value = source.value,
                    fileName = source.fileName ?: "",
                    categoryIds = JSONArray(source.categoryIds).toString(),
                    createdAt = source.createdAt,
                ),
            )
        }
    }

    fun addOrReplace(source: PlaylistSource) {
        val next = sources().toMutableList()
        val sameId = next.indexOfFirst { it.id == source.id }
        val sameUrl = if (source.type == SourceType.URL) {
            next.indexOfFirst { it.type == SourceType.URL && it.value == source.value }
        } else {
            -1
        }
        val index = if (sameId >= 0) sameId else sameUrl
        if (index >= 0) {
            val current = next[index]
            next[index] = source.copy(
                id = current.id,
                categoryIds = (current.categoryIds + source.categoryIds).distinct(),
            )
        } else {
            next.add(0, source)
        }
        saveSources(next)
    }

    fun removeSource(id: String) {
        dao.deleteSource(id)
        if (lastSourceId() == id) prefs.edit().remove(KEY_LAST_SOURCE).apply()
    }

    fun setSourceCategories(id: String, categoryIds: List<String>) {
        saveSources(
            sources().map { source ->
                if (source.id == id) source.copy(categoryIds = categoryIds.distinct()) else source
            },
        )
    }

    fun categories(): List<PlaylistCategory> =
        dao.categories().map { PlaylistCategory(it.id, it.name) }

    fun addCategory(name: String): PlaylistCategory? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (categories().any { it.name.equals(trimmed, ignoreCase = true) }) return null
        val category = PlaylistCategory(UUID.randomUUID().toString(), trimmed)
        dao.upsertCategory(CategoryEntity(category.id, category.name, System.currentTimeMillis()))
        return category
    }

    fun deleteCategory(id: String) {
        categories().firstOrNull { it.id == id }?.let {
            dao.deleteCategory(CategoryEntity(it.id, it.name, 0))
        }
        saveSources(sources().map { it.copy(categoryIds = it.categoryIds.filterNot { c -> c == id }) })
    }

    fun lastSourceId(): String? = prefs.getString(KEY_LAST_SOURCE, null)

    fun rememberSource(id: String) {
        prefs.edit().putString(KEY_LAST_SOURCE, id).apply()
    }

    fun lastChannel(): String? = prefs.getString(KEY_LAST_CHANNEL, null)

    fun rememberChannel(url: String) {
        dao.addHistory(HistoryEntity(url, System.currentTimeMillis()))
        dao.pruneHistory()
        prefs.edit().putString(KEY_LAST_CHANNEL, url).apply()
    }

    fun favorites(): Set<String> = dao.favorites().map { it.url }.toSet()

    fun toggleFavorite(url: String): Set<String> {
        val current = favorites()
        if (current.contains(url)) {
            dao.removeFavorite(url)
        } else {
            dao.addFavorite(FavoriteEntity(url, System.currentTimeMillis()))
        }
        return favorites()
    }

    fun history(): List<String> = dao.history().map { it.url }

    private fun parseIds(raw: String): List<String> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { array.optString(it).takeIf { value -> value.isNotBlank() } }
    }.getOrDefault(emptyList())

    /**
     * Carry over the pre-Room JSON library from early M1 installs. Existing users
     * keep their sources, favourites and history after upgrading.
     */
    private fun migrateLegacyPreferences() {
        if (dao.sourceCount() > 0) return
        val legacySources = runCatching {
            val raw = prefs.getString(LEGACY_SOURCES, null) ?: return
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val objectValue = array.optJSONObject(index) ?: return@mapNotNull null
                val type = runCatching {
                    SourceType.valueOf(objectValue.optString("type", SourceType.URL.name))
                }.getOrDefault(SourceType.URL)
                PlaylistSource(
                    id = objectValue.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = objectValue.optString("name", "Playlist"),
                    type = type,
                    value = objectValue.optString("value", ""),
                    fileName = objectValue.optString("fileName").ifBlank { null },
                )
            }
        }.getOrDefault(emptyList())

        if (legacySources.isNotEmpty()) {
            legacySources.forEachIndexed { index, source ->
                dao.upsertSource(
                    SourceEntity(
                        id = source.id,
                        name = source.name,
                        type = source.type.name,
                        value = source.value,
                        fileName = source.fileName ?: "",
                        categoryIds = "[]",
                        createdAt = index.toLong(),
                    ),
                )
            }
        }

        runCatching {
            val favorites = JSONArray(prefs.getString(LEGACY_FAVORITES, "[]") ?: "[]")
            (0 until favorites.length()).forEach { index ->
                favorites.optString(index).takeIf { it.isNotBlank() }?.let {
                    dao.addFavorite(FavoriteEntity(it, System.currentTimeMillis()))
                }
            }
        }
        runCatching {
            val history = JSONArray(prefs.getString(LEGACY_HISTORY, "[]") ?: "[]")
            (0 until history.length()).forEach { index ->
                history.optString(index).takeIf { it.isNotBlank() }?.let {
                    dao.addHistory(HistoryEntity(it, System.currentTimeMillis() - index))
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "zeltatv"
        private const val KEY_LAST_SOURCE = "last_source"
        private const val KEY_LAST_CHANNEL = "last_channel"
        private const val LEGACY_SOURCES = "sources"
        private const val LEGACY_FAVORITES = "favorites"
        private const val LEGACY_HISTORY = "history"
    }
}
