package tv.zelta.core.playlist

/**
 * One IPTV channel entry. Mirrors the desktop model
 * (`src/lib/iptv/types.ts`) so the two apps can share fixtures and docs.
 */
data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val tvgId: String? = null,
)

/**
 * Minimal, dependency-free M3U/M3U8 parser.
 *
 * Behaviour is intentionally kept close to the desktop parser, with two
 * hardening fixes that matter on mobile:
 *  - UTF-8 BOM is stripped (files exported on Windows / some providers include it),
 *  - the channel name is taken after the first *unquoted* comma, so a
 *    `group-title="News, World"` attribute no longer corrupts the name.
 *
 * Playlist bodies can be hundreds of thousands of lines; the parser streams
 * over lines and only allocates the resulting list.
 */
object M3uParser {
    private val EXTINF = Regex("""#EXTINF:-?[\d.]*\s*(.*)""", RegexOption.IGNORE_CASE)
    private val ATTR = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(raw: String): List<Channel> {
        val content = raw.removePrefix("\uFEFF")
        val channels = ArrayList<Channel>()
        var pending: Pending? = null

        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                val payload = EXTINF.matchEntire(trimmed)?.groupValues?.get(1) ?: return@forEach
                val attrs = HashMap<String, String>()
                ATTR.findAll(payload).forEach { match ->
                    attrs[match.groupValues[1].lowercase()] = match.groupValues[2]
                }
                pending = Pending(
                    name = nameOf(payload),
                    logo = attrs["tvg-logo"]?.ifBlank { null },
                    group = attrs["group-title"]?.ifBlank { null },
                    tvgId = attrs["tvg-id"]?.ifBlank { null },
                )
            } else if (!trimmed.startsWith("#")) {
                val entry = pending ?: return@forEach
                channels += Channel(
                    id = "${channels.size}-$trimmed",
                    name = entry.name,
                    url = trimmed,
                    logo = entry.logo,
                    group = entry.group,
                    tvgId = entry.tvgId,
                )
                pending = null
            }
        }

        return channels
    }

    /** Everything after the first comma that is not inside a quoted attribute. */
    private fun nameOf(payload: String): String {
        var quoted = false
        for (index in payload.indices) {
            when (payload[index]) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) {
                    return payload.substring(index + 1).trim().ifEmpty { "Unknown" }
                }
            }
        }
        return "Unknown"
    }

    private data class Pending(
        val name: String,
        val logo: String?,
        val group: String?,
        val tvgId: String?,
    )
}
