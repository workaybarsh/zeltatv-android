package tv.zelta.core.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun `parses channels with attributes`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="trt1" tvg-logo="https://cdn.example/trt1.png" group-title="News",TRT 1
            https://example.com/trt1/index.m3u8
            #EXTINF:-1 group-title="Sports",TRT Spor
            http://example.com/trtspor.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertEquals("TRT 1", channels[0].name)
        assertEquals("https://example.com/trt1/index.m3u8", channels[0].url)
        assertEquals("trt1", channels[0].tvgId)
        assertEquals("News", channels[0].group)
        assertEquals("https://cdn.example/trt1.png", channels[0].logo)
        assertEquals("Sports", channels[1].group)
    }

    @Test
    fun `strips utf8 bom and handles crlf`() {
        val playlist = "\uFEFF#EXTM3U\r\n#EXTINF:-1,Test\r\nhttp://example.com/a.m3u8\r\n"

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Test", channels[0].name)
        assertEquals("http://example.com/a.m3u8", channels[0].url)
    }

    @Test
    fun `keeps commas inside quoted attributes out of the name`() {
        val playlist = """
            #EXTINF:-1 tvg-name="News, World" group-title="News, World",World News
            http://example.com/news.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("World News", channels[0].name)
        assertEquals("News, World", channels[0].group)
    }

    @Test
    fun `keeps commas inside the display name`() {
        val playlist = "#EXTINF:-1,News, Weather and Sport\nhttp://example.com/n.m3u8"

        val channels = M3uParser.parse(playlist)

        assertEquals("News, Weather and Sport", channels[0].name)
    }

    @Test
    fun `ignores comments blank lines and orphan urls`() {
        val playlist = """
            #EXTM3U
            #EXTVLCOPT:http-user-agent=ZeltaTV
            http://example.com/orphan.m3u8

            #EXTINF:-1,Good
            # a comment between entries
            http://example.com/good.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Good", channels[0].name)
    }

    @Test
    fun `blank optional attributes become null`() {
        val playlist = "#EXTINF:-1 tvg-logo=\"\" group-title=\"\",No Logo\nhttp://example.com/x.m3u8"

        val channels = M3uParser.parse(playlist)

        assertNull(channels[0].logo)
        assertNull(channels[0].group)
        assertNull(channels[0].tvgId)
    }

    @Test
    fun `returns empty list for non playlist content`() {
        assertTrue(M3uParser.parse("<html>not a playlist</html>").isEmpty())
        assertTrue(M3uParser.parse("").isEmpty())
    }

    @Test
    fun `ids are unique per entry`() {
        val playlist = """
            #EXTINF:-1,A
            http://example.com/same.m3u8
            #EXTINF:-1,B
            http://example.com/same.m3u8
        """.trimIndent()

        val ids = M3uParser.parse(playlist).map { it.id }

        assertEquals(ids.size, ids.distinct().size)
    }
}
