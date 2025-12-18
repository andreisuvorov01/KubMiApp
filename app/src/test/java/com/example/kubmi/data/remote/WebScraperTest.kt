package com.example.kubmi.data.remote

import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebScraperTest {

    private val scraper = WebScraper()

    @Test
    fun `parseWeeklyScheduleData handles simple table`() = runBlocking {
        val html = """
            <html>
              <body>
                <table>
                  <tr><th>День</th><th>Время</th><th>Предмет</th><th>Тип</th><th>Аудитория</th><th>Преподаватель</th></tr>
                  <tr><td>Понедельник</td><td>09:00-10:30</td><td>Анатомия</td><td>Лекция</td><td>101</td><td>Иванов И.И.</td></tr>
                </table>
              </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val result = scraper.parseWeeklyScheduleData(doc)

        assertEquals(1, result.size)
        assertTrue(result.first().rows.isNotEmpty())
        val row = result.first().rows.last()
        assertEquals("Понедельник", row.cells[0].text)
        assertEquals("09:00-10:30", row.cells[1].text)
    }
}
