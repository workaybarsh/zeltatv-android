package tv.zelta.app.data

/** A saved playlist source. FILE sources keep their parsed M3U text in [value]. */
enum class SourceType { DEMO, URL, FILE }

data class PlaylistSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val value: String = "",
    val fileName: String? = null,
    val categoryIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class PlaylistCategory(
    val id: String,
    val name: String,
)

fun demoSource(): PlaylistSource =
    PlaylistSource(
        id = DemoSourceId,
        name = "Demo list",
        type = SourceType.DEMO,
    )

const val DemoSourceId = "demo"
