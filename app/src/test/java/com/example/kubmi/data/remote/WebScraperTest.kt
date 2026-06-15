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

    @Test
    fun `parseStudentGroupsTableFromPage handles public schedule table`() = runBlocking {
        val html = """
            <html>
              <body>
                <main>
                  <h1>Расписание занятий студентов</h1>
                  <table>
                    <tr><td>1 курс</td><td>2 курс</td><td>3 курс</td><td>4 курс</td><td>5 курс</td><td>6 курс</td></tr>
                    <tr>
                      <td><a href="/raspisanie/L251.html">L251</a></td>
                      <td><a href="/raspisanie/L241.html">L241</a></td>
                      <td><a href="/raspisanie/S231.html">S231</a></td>
                      <td><a href="/raspisanie/L221.html">L221</a></td>
                      <td><a href="/raspisanie/L211.html">L211</a></td>
                      <td><a href="/raspisanie/L201.html">L201</a></td>
                    </tr>
                  </table>
                </main>
              </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://kubmi.ru/raspisanie-zanyatij-studentov/")
        val table = scraper.parseStudentGroupsTableFromPage(doc)

        assertEquals(6, table.headers.size)
        assertEquals(1, table.rows.size)
        assertEquals("L251", table.rows.first()[0]?.title)
        assertEquals("https://kubmi.ru/raspisanie/L251.html", table.rows.first()[0]?.url)
    }

    @Test
    fun `parseTeacherEntriesFromPage handles public schedule table`() = runBlocking {
        val html = """
            <html>
              <body>
                <article>
                  <h1>Расписание занятий преподавателей</h1>
                  <table>
                    <tr><td><a href="/raspisanie/ivanov.html">Иванов И.И.</a></td></tr>
                    <tr><td><a href="/raspisanie/petrova.html">Петрова П.П.</a></td></tr>
                  </table>
                </article>
              </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://kubmi.ru/raspisanie-zanyatij-prepodavatelej/")
        val teachers = scraper.parseTeacherEntriesFromPage(doc)

        assertEquals(2, teachers.size)
        assertEquals("Иванов И.И.", teachers.first().title)
        assertEquals("https://kubmi.ru/raspisanie/ivanov.html", teachers.first().url)
    }
}
