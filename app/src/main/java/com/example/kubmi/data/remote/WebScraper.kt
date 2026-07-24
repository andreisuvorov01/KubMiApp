package com.example.kubmi.data.remote

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.kubmi.domain.model.News
import com.example.kubmi.domain.model.LessonType
import com.example.kubmi.domain.model.AboutContent
import com.example.kubmi.domain.model.ManagementPerson
import com.example.kubmi.domain.model.AboutFaculty
import com.example.kubmi.domain.model.ContactInfo
import com.example.kubmi.domain.model.Achievement
import com.example.kubmi.domain.model.NewsContentBlock
import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.ScheduleCellContent
import com.example.kubmi.domain.model.ScheduleTableRow
import com.example.kubmi.domain.model.WeeklyScheduleData
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import java.util.regex.Pattern
import timber.log.Timber
import android.util.Log
import java.util.UUID
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

private const val GO_CHS_URL = "https://kubmi.ru/institut/go-i-chs/"
private const val NEWS_URL = "https://kubmi.ru/novosti/"
private const val STUDENT_SCHEDULE_URL = "https://kubmi.ru/raspisanie-zanyatij-studentov/"
private const val TEACHER_SCHEDULE_URL = "https://kubmi.ru/raspisanie-zanyatij-prepodavatelej/"

@Singleton
class WebScraper @Inject constructor() {
    private companion object {
        const val INSTITUTE_HISTORY_URL = "https://kubmi.ru/institut/istoriya-instituta/"
    }

    // Кэшируем часто используемые селекторы
    private val newsSelector =
        "div.eael-post-block-grid.eael-post-appender.eael-post-appender-53969f8, #elementor-element-7d507b9, .news-container"
    private val newsItemSelector = "article.eael-post-block-item, article, .news-item, .post"
    private val titleSelector = "h2 a, h3 a, .eael-entry-title a, h2, h3, .title"
    private val descSelector = ".eael-entry-content p, .eael-post-excerpt, .excerpt, p"
    private val dateSelector = "time, .date, .eael-entry-meta, .eael-post-meta"
    private val imgSelector = "img"

    private fun normalizeSpaces(input: String): String = input.trim().replace(Regex("\\s+"), " ")
    private fun normalizeGroupKey(group: String): String = normalizeSpaces(group).uppercase(Locale.ROOT)
    private fun normalizeTeacherDisplay(name: String): String = normalizeSpaces(name)
    private fun normalizeTeacherKey(name: String): String = normalizeTeacherDisplay(name).lowercase(Locale.ROOT)
    private fun isTimeText(text: String): Boolean = Regex("\\d{1,2}:\\d{2}").containsMatchIn(text)
    private fun isDayText(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        return listOf("понедель", "вторник", "сред", "четверг", "пятниц", "суббот", "воскрес").any { t.contains(it) }
    }

    private fun looksLikeRoom(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        return t.contains("ауд") || t.contains("каб") || t.contains("аудит") || Regex("\\d{2,4}").containsMatchIn(t)
    }

    private fun collectRaspisanieLinks(doc: org.jsoup.nodes.Document): List<String> {
        return doc.select("a[href]")
            .mapNotNull { a ->
                val href = a.absUrl("href").ifBlank { a.attr("href") }
                if (href.isBlank()) return@mapNotNull null
                val lower = href.lowercase(Locale.ROOT)
                val looksLikeScheduleLeaf =
                    (lower.contains("/raspisanie/") || lower.contains("raspisanie")) &&
                        (lower.endsWith(".html") || lower.endsWith(".htm"))
                if (!looksLikeScheduleLeaf) return@mapNotNull null
                if (lower.endsWith("/raspisanie/1.htm") || lower.endsWith("/raspisanie/1.html")) return@mapNotNull null
                href
            }
            .distinct()
    }

    private suspend fun fetchDoc(url: String): org.jsoup.nodes.Document {
        require(url.isNotBlank()) { "Schedule/news URL is blank" }
        return Jsoup.connect(url)
            .followRedirects(true)
            .ignoreContentType(true)
            .timeout(30_000)
            .maxBodySize(0)
            .referrer("https://kubmi.ru/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.7,en;q=0.6")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .execute()
            .parse()
    }

    private suspend fun fetchInstituteHistoryDoc(): org.jsoup.nodes.Document = fetchDoc(INSTITUTE_HISTORY_URL)

    private fun buildHtmlFromContent(contentRoot: Element): String {
        contentRoot.select("script, style, nav, header, footer, form, noscript").remove()
        val safelist = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4")
            .removeTags("img")
        val cleaner = org.jsoup.safety.Cleaner(safelist)
        val cleaned = cleaner.clean(contentRoot.ownerDocument()!!)
        val html = cleaned.selectFirst("body")?.html().orEmpty()
        return html.ifBlank { "История института недоступна." }
    }

    suspend fun parseWeeklyScheduleData(doc: org.jsoup.nodes.Document): List<WeeklyScheduleData> {
        val weeklySchedules = mutableListOf<WeeklyScheduleData>()
        val tables = doc.select("table").filter { table ->
            table.select("tr").any { it.select("td, th").size > 2 }
        }
        tables.forEach { table ->
            val tableRows = mutableListOf<ScheduleTableRow>()
            var weekTitle: String? = null
            val titleElement = table.previousElementSibling()
            if (titleElement != null && titleElement.tagName() == "h2") {
                weekTitle = titleElement.text().trim()
            }
            if (weekTitle == null) {
                weekTitle = table.selectFirst("caption, h2, h3")?.text()?.trim()
                if (weekTitle != null && weekTitle.lowercase(Locale.ROOT).startsWith("расписание занятий")) {
                    weekTitle = null
                }
            }
            val trs = table.select("tr")
            trs.forEachIndexed { rowIndex, tr ->
                val cells = tr.select("td, th")
                val scheduleCells = mutableListOf<ScheduleCellContent>()
                cells.forEach { cell ->
                    val text = normalizeSpaces(cell.text())
                    val rowSpan = cell.attr("rowspan").toIntOrNull() ?: 1
                    val colSpan = cell.attr("colspan").toIntOrNull() ?: 1
                    scheduleCells.add(ScheduleCellContent(text, rowSpan, colSpan))
                }
                if (scheduleCells.isNotEmpty()) {
                    val isHeaderRow = tr.selectFirst("th") != null || rowIndex == 0
                    tableRows.add(ScheduleTableRow(isHeaderRow, scheduleCells))
                }
            }
            if (tableRows.isNotEmpty()) {
                weeklySchedules.add(WeeklyScheduleData(weekTitle, tableRows))
            }
        }
        return weeklySchedules
    }
    
    suspend fun fetchStudentScheduleIndexPage(): org.jsoup.nodes.Document = fetchDocWithRetry(STUDENT_SCHEDULE_URL)

    suspend fun fetchTeacherScheduleIndexPage(): org.jsoup.nodes.Document = fetchDocWithRetry(TEACHER_SCHEDULE_URL)

    suspend fun scrapeStudentScheduleByUrl(groupTitle: String, url: String): List<WeeklyScheduleData> {
        if (url.isBlank()) {
            Timber.w("Student schedule URL is blank for group %s", groupTitle)
            return emptyList()
        }
        val doc = fetchDocWithRetry(url)
        return parseWeeklyScheduleData(doc)
    }

    suspend fun scrapeTeacherScheduleByUrl(teacherTitle: String, url: String): List<WeeklyScheduleData> {
        if (url.isBlank()) {
            Timber.w("Teacher schedule URL is blank for teacher %s", teacherTitle)
            return emptyList()
        }
        val doc = fetchDocWithRetry(url)
        return parseWeeklyScheduleData(doc)
    }

    suspend fun parseGroupEntriesFromPage(doc: org.jsoup.nodes.Document): List<ScheduleIndexEntry> {
        val links = doc.select("main table a[href], article table a[href], .entry-content table a[href], figure.wp-block-table table a[href]").ifEmpty {
            doc.select("table a[href]")
        }
        return links.mapNotNull { link ->
            val title = link.text().trim()
            val url = link.absUrl("href")
            val lowerUrl = url.lowercase(Locale.ROOT)
            
            // Фильтруем пустые заголовки и технические страницы-заглушки (1.htm, 1.html)
            if (title.isNotEmpty() && 
                !lowerUrl.endsWith("/raspisanie/1.htm") && 
                !lowerUrl.endsWith("/raspisanie/1.html") &&
                !lowerUrl.endsWith("/1.htm") &&
                !lowerUrl.endsWith("/1.html")) {
                ScheduleIndexEntry(title = title, url = url)
            } else {
                Log.d("WebScraper", "Skipping invalid group link: title='$title', url=$url")
                null
            }
        }
    }

    suspend fun parseTeacherEntriesFromPage(doc: org.jsoup.nodes.Document): List<ScheduleIndexEntry> {
        val links = doc.select("main table a[href], article table a[href], .entry-content table a[href], figure.wp-block-table table a[href]").ifEmpty {
            doc.select("table a[href]")
        }
        return links.mapNotNull { link ->
            val title = link.text()
            val url = link.absUrl("href")
            if (title.isNotEmpty() && !url.lowercase(Locale.ROOT).endsWith("/raspisanie/1.htm") && !url.lowercase(Locale.ROOT).endsWith("/raspisanie/1.html")) {
                ScheduleIndexEntry(title = title, url = url)
            } else {
                null
            }
        }
    }

    suspend fun parseStudentGroupsTableFromPage(doc: org.jsoup.nodes.Document): com.example.kubmi.domain.model.StudentGroupsTable {
        val candidates = doc.select("main table, article table, .entry-content table, figure.wp-block-table table, table")
        val table = candidates.firstOrNull { t ->
            val firstRow = t.selectFirst("tr") ?: return@firstOrNull false
            val headers = firstRow.select("th, td").map { it.text() }
            headers.size == 6 && headers.all { it.contains("курс") }
        }
        if (table == null) return com.example.kubmi.domain.model.StudentGroupsTable(headers = emptyList(), rows = emptyList())
        val trs = table.select("tr")
        if (trs.isEmpty()) return com.example.kubmi.domain.model.StudentGroupsTable(headers = emptyList(), rows = emptyList())
        val headerCells = trs.first()!!.select("th, td")
        val headers = headerCells.map { it.text() }
        val rows = mutableListOf<List<ScheduleIndexEntry?>>()
        for (tr in trs.drop(1)) {
            val tds = tr.select("td, th")
            val rowCells = mutableListOf<ScheduleIndexEntry?>()
            for (idx in 0 until headers.size) {
                val cell = tds.getOrNull(idx)
                if (cell == null) {
                    rowCells.add(null)
                    continue
                }
                val a = cell.selectFirst("a[href]")
                if (a == null) {
                    rowCells.add(null)
                    continue
                }
                val title = a.text()
                val url = a.absUrl("href").ifBlank { a.attr("href") }
                if (title.isEmpty()) {
                    rowCells.add(null)
                    continue
                }
                val lowerUrl = url.lowercase(Locale.ROOT)
                if (lowerUrl.endsWith("/raspisanie/1.htm") || lowerUrl.endsWith("/raspisanie/1.html")) {
                    rowCells.add(null)
                    continue
                }
                rowCells.add(ScheduleIndexEntry(title = title, url = url))
            }
            rows.add(rowCells)
        }
        return com.example.kubmi.domain.model.StudentGroupsTable(headers = headers, rows = rows)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scrapeNews(): List<News> {
        return try {
            Timber.i("Scraping news list from %s", NEWS_URL)
            val doc = fetchDocWithRetry(NEWS_URL)
            val newsElements = selectNewsElements(doc)
            val parsed = newsElements
                .asSequence()
                .mapNotNull { parseNewsElement(it) }
                .distinctBy { it.id.ifBlank { it.title.lowercase(Locale.ROOT) } }
                .sortedWith(compareByDescending<News> { parseNewsDateMillis(it.date) ?: 0L }.thenBy { it.title })
                .toList()

            if (parsed.isEmpty()) {
                Timber.w("No news items parsed from %s; page structure may have changed", NEWS_URL)
            } else {
                Timber.i("Parsed %d news items from %s", parsed.size, NEWS_URL)
            }
            parsed
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape news from %s", NEWS_URL)
            emptyList()
        }
    }

    private suspend fun fetchDocWithRetry(url: String, attempts: Int = 3): org.jsoup.nodes.Document {
        var lastError: Exception? = null
        repeat(attempts) { attempt ->
            try {
                Timber.d("HTTP GET %s (attempt %d/%d)", url, attempt + 1, attempts)
                return fetchDoc(url)
            } catch (e: Exception) {
                lastError = e
                Timber.w(e, "HTTP GET failed for %s (attempt %d/%d)", url, attempt + 1, attempts)
                if (attempt < attempts - 1) delay(750L * (attempt + 1))
            }
        }
        throw lastError ?: IOException("Failed to fetch $url")
    }

    private fun selectNewsElements(doc: org.jsoup.nodes.Document): List<Element> {
        val selectors = listOf(
            "article.elementor-post",
            ".elementor-posts-container article",
            ".eael-post-grid article, .eael-post-block article, .eael-grid-post",
            "article.type-post",
            "article",
            ".post, .news-item"
        )
        return selectors.asSequence()
            .map { doc.select(it) }
            .firstOrNull { it.isNotEmpty() }
            ?.filter { it.selectFirst("a[href]") != null && it.text().isNotBlank() }
            ?: emptyList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseNewsDateMillis(date: String): Long? {
        val cleaned = normalizeSpaces(date).lowercase(Locale.ROOT)
            .replace("года", "")
            .replace("г.", "")
            .trim()
        val ruMonths = mapOf(
            "января" to "01", "февраля" to "02", "марта" to "03", "апреля" to "04",
            "мая" to "05", "июня" to "06", "июля" to "07", "августа" to "08",
            "сентября" to "09", "октября" to "10", "ноября" to "11", "декабря" to "12"
        )
        val normalized = ruMonths.entries.fold(cleaned) { acc, (month, number) -> acc.replace(month, number) }
        val candidates = Regex("""\d{1,2}[.\-/\s]\d{1,2}[.\-/\s]\d{4}""").find(normalized)?.value ?: normalized
        val formatters = listOf("d.M.yyyy", "dd.MM.yyyy", "d M yyyy", "d-M-yyyy", "yyyy-MM-dd").map {
            DateTimeFormatterBuilder().appendPattern(it).parseDefaulting(ChronoField.HOUR_OF_DAY, 0).toFormatter(Locale("ru"))
        }
        return formatters.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(candidates, formatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) { null }
        }
    }

    data class NewsArticleScrapeResult(val title: String, val blocks: List<NewsContentBlock>, val fullText: String, val coverImageUrl: String? = null)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scrapeNewsArticle(url: String): NewsArticleScrapeResult {
        val doc = fetchDoc(url)
        val title = normalizeSpaces(doc.selectFirst("h1")?.text().orEmpty())
        
        // Try to find a featured image or cover image
        val coverImageUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".entry-thumb img, .post-thumbnail img, .elementor-image img")?.let { img ->
                val src = img.absUrl("data-lazy-src").ifBlank { img.attr("data-lazy-src") }
                    .ifBlank { img.absUrl("data-src").ifBlank { img.attr("data-src") } }
                    .ifBlank { img.absUrl("src").ifBlank { img.attr("src") } }
                src.takeIf { it.isNotBlank() && !it.startsWith("data:image") }
            }

        fun isInsideBadContainer(el: Element): Boolean {
            val badTags = setOf("header", "footer", "nav", "aside")
            return badTags.contains(el.tagName().lowercase(Locale.ROOT)) || el.parents().any { badTags.contains(it.tagName().lowercase(Locale.ROOT)) }
        }
        fun scoreCandidate(el: Element): Int {
            val text = normalizeSpaces(el.text())
            val pCount = el.select("p").size
            val liCount = el.select("li").size
            val imgCount = el.select("img").size
            val looksLikeOnlyImage = text.length < 80 && imgCount > 0 && (pCount + liCount) == 0
            var score = text.length + 60 * (pCount + liCount)
            if (looksLikeOnlyImage) score -= 800
            val cls = el.className().lowercase(Locale.ROOT)
            if (cls.contains("entry-content") || cls.contains("post-content")) score += 250
            return score
        }
        fun selectContentRoot(): Element {
            val candidates = doc.select(".entry-content, .post-content, .elementor-widget-theme-post-content, .elementor-text-editor")
                .filter { !isInsideBadContainer(it) }
            return candidates.maxByOrNull { scoreCandidate(it) } ?: (doc.selectFirst("article") ?: doc)
        }
        val contentRoot = selectContentRoot()
        contentRoot.select("script, style, noscript, header, footer, nav, aside, form, button").remove()
        val blocks = mutableListOf<NewsContentBlock>()
        fun resolveUrl(raw: String): String? = try { URI(doc.baseUri()).resolve(raw.trim()).toString() } catch (_: Exception) { null }
        fun walk(el: Element) {
            val tag = el.tagName().lowercase(Locale.ROOT)
            when (tag) {
                "script", "style", "noscript" -> return
                "img" -> {
                    fun resolve(raw: String): String? {
                        val r = raw.trim()
                        if (r.isBlank()) return null
                        if (r.startsWith("http://") || r.startsWith("https://")) return r
                        if (r.startsWith("//")) return "https:$r"
                        return try { URI(doc.baseUri()).resolve(r).toString() } catch (_: Exception) { null }
                    }

                    val candidates = listOf(
                        el.absUrl("data-lazy-src").ifBlank { el.attr("data-lazy-src") },
                        el.absUrl("data-src").ifBlank { el.attr("data-src") },
                        el.absUrl("src").ifBlank { el.attr("src") }
                    )
                    val src = candidates.mapNotNull { resolve(it) }.firstOrNull { it.isNotBlank() && !it.startsWith("data:image") }
                    
                    if (src != null) {
                        blocks.add(NewsContentBlock(type = NewsContentBlock.TYPE_IMAGE, imageUrl = src))
                    }
                }
                "p", "h2", "h3", "h4", "h5", "h6", "blockquote" -> {
                    val text = normalizeSpaces(el.text())
                    if (text.isNotBlank()) blocks.add(NewsContentBlock(type = NewsContentBlock.TYPE_TEXT, text = text))
                    el.select("img").forEach { walk(it) }
                }
                "li" -> {
                    val text = normalizeSpaces(el.text())
                    if (text.isNotBlank()) blocks.add(NewsContentBlock(type = NewsContentBlock.TYPE_TEXT, text = "• $text"))
                }
                else -> el.children().forEach { walk(it) }
            }
        }
        walk(contentRoot)
        val fullText = blocks.filter { it.type == NewsContentBlock.TYPE_TEXT }.mapNotNull { it.text }.joinToString("\n\n")
        return NewsArticleScrapeResult(title, blocks, fullText, coverImageUrl)
    }

    private fun parseNewsElement(element: Element): News? {
        val titleLink = element.selectFirst(".elementor-post__title a[href], .entry-title a[href], .eael-entry-title a[href], p.eael-entry-title a[href], h1 a[href], h2 a[href], h3 a[href], a[href]")
        val title = normalizeSpaces(titleLink?.text().orEmpty()).ifBlank {
            normalizeSpaces(element.selectFirst(".elementor-post__title, .entry-title, .eael-entry-title, h1, h2, h3")?.text().orEmpty())
        }
        if (title.isBlank()) return null

        val url = titleLink?.absUrl("href").orEmpty()

        fun previewImage(img: Element): String? {
            fun resolve(raw: String): String? {
                val r = raw.trim()
                if (r.isBlank()) return null
                if (r.startsWith("http://") || r.startsWith("https://")) return r
                if (r.startsWith("//")) return "https:$r"
                val base = img.ownerDocument()?.baseUri().orEmpty()
                return try {
                    URI(base).resolve(r).toString()
                } catch (_: Exception) {
                    null
                }
            }

            // Prefer lazy attributes; ignore placeholders.
            val candidates = listOf(
                img.absUrl("data-lazy-src").ifBlank { img.attr("data-lazy-src") },
                img.absUrl("data-src").ifBlank { img.attr("data-src") },
                img.absUrl("data-original").ifBlank { img.attr("data-original") },
                img.absUrl("src").ifBlank { img.attr("src") }
            )
            val fromAttrs = candidates
                .mapNotNull { resolve(it) }
                .firstOrNull { it.isNotBlank() && !it.startsWith("data:image", ignoreCase = true) }
            if (!fromAttrs.isNullOrBlank()) return fromAttrs

            // srcset fallbacks (take first candidate)
            fun pickFromSrcSet(raw: String): String? {
                if (raw.isBlank()) return null
                return raw.split(",")
                    .mapNotNull { part ->
                        val url = part.trim().split("\\s+".toRegex()).firstOrNull().orEmpty()
                        resolve(url)
                    }
                    .firstOrNull { it.isNotBlank() && !it.startsWith("data:image", ignoreCase = true) }
            }
            return pickFromSrcSet(img.attr("data-lazy-srcset"))
                ?: pickFromSrcSet(img.attr("data-srcset"))
                ?: pickFromSrcSet(img.attr("srcset"))
        }

        val description = normalizeSpaces(element.selectFirst(".elementor-post__excerpt, .eael-entry-content p, .eael-grid-post-excerpt p, .eael-post-excerpt, .entry-summary, .excerpt, p")?.text().orEmpty())
        val dateElement = element.selectFirst("time[datetime], time, .elementor-post-date, .elementor-post__meta-data, .posted-on, .entry-date, .date, .eael-entry-meta, .eael-post-meta")
        val date = normalizeSpaces(dateElement?.attr("datetime")?.takeIf { it.isNotBlank() } ?: dateElement?.text().orEmpty())
            .ifBlank { LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
        val imageUrl = element.selectFirst(imgSelector)?.let { previewImage(it) }

        return News(
            id = if (url.isNotBlank()) url else UUID.randomUUID().toString(),
            title = title,
            description = description,
            content = description,
            date = date,
            imageUrl = imageUrl
        )
    }

    suspend fun scrapeStudentSchedule(group: String = ""): List<WeeklyScheduleData> {
        return try {
            val doc = fetchDocWithRetry(STUDENT_SCHEDULE_URL)
            if (group.isNotEmpty()) {
                val groupUrl = findGroupUrl(doc, group)
                if (groupUrl != null) return getScheduleFromGroupPage(groupUrl, group)
                return emptyList()
            }
            val allGroups = parseGroupsFromPage(doc)
            val scheduleItems = mutableListOf<WeeklyScheduleData>()
            for (groupName in allGroups) {
                findGroupUrl(doc, groupName)?.let { url ->
                    scheduleItems.addAll(getScheduleFromGroupPage(url, groupName))
                    delay(500)
                }
            }
            scheduleItems
        } catch (e: Exception) { Timber.e(e, "Failed to scrape student schedule"); emptyList() }
    }
    
    private suspend fun findGroupUrl(doc: org.jsoup.nodes.Document, group: String): String? {
        val targetKey = normalizeGroupKey(group)
        val links = doc.select("main table a[href], article table a[href], .entry-content table a[href], figure.wp-block-table table a[href]").ifEmpty { doc.select("table a[href]") }
        return links.firstOrNull { normalizeGroupKey(it.text()) == targetKey }?.absUrl("href")
    }
    
    private suspend fun getScheduleFromGroupPage(url: String, group: String): List<WeeklyScheduleData> {
        return try {
            val doc = fetchDocWithRetry(url)
            val scheduleItems = mutableListOf<WeeklyScheduleData>()
            fun parseTables(fromDoc: org.jsoup.nodes.Document) {
                fromDoc.select("table").forEach { table ->
                    val rows = table.select("tr")
                    val tableRows = mutableListOf<ScheduleTableRow>()
                    rows.forEach { row ->
                        val cells = row.select("td, th")
                        if (cells.isNotEmpty()) {
                            val scheduleCells = cells.map { ScheduleCellContent(normalizeSpaces(it.text()), it.attr("rowspan").toIntOrNull() ?: 1, it.attr("colspan").toIntOrNull() ?: 1) }
                            tableRows.add(ScheduleTableRow(row.selectFirst("th") != null, scheduleCells))
                        }
                    }
                    if (tableRows.isNotEmpty()) scheduleItems.add(WeeklyScheduleData(null, tableRows))
                }
            }
            parseTables(doc)
            if (scheduleItems.isEmpty()) {
                collectRaspisanieLinks(doc).filter { it != url }.forEach { nestedUrl ->
                    parseTables(fetchDocWithRetry(nestedUrl))
                    delay(250)
                }
            }
            scheduleItems
        } catch (e: Exception) { Timber.e(e, "Failed to scrape group schedule"); emptyList() }
    }

    suspend fun scrapeTeacherSchedule(teacher: String = ""): List<WeeklyScheduleData> {
        return try {
            val doc = fetchDocWithRetry(TEACHER_SCHEDULE_URL)
            if (teacher.isNotEmpty()) {
                findTeacherUrl(doc, teacher)?.let { return getScheduleFromTeacherPage(it, teacher) }
                return emptyList()
            }
            val allTeachers = parseTeachersFromPage(doc)
            val scheduleItems = mutableListOf<WeeklyScheduleData>()
            for (teacherName in allTeachers) {
                findTeacherUrl(doc, teacherName)?.let { url ->
                    scheduleItems.addAll(getScheduleFromTeacherPage(url, teacherName))
                    delay(500)
                }
            }
            scheduleItems
        } catch (e: Exception) { Timber.e(e, "Failed to scrape teacher schedule"); emptyList() }
    }
    
    private suspend fun findTeacherUrl(doc: org.jsoup.nodes.Document, teacher: String): String? {
        val targetKey = normalizeTeacherKey(teacher)
        val links = doc.select("main table a[href], article table a[href], .entry-content table a[href], figure.wp-block-table table a[href]").ifEmpty { doc.select("table a[href]") }
        return links.firstOrNull { normalizeTeacherKey(it.text()) == targetKey }?.absUrl("href")
    }
    
    private suspend fun getScheduleFromTeacherPage(url: String, teacher: String): List<WeeklyScheduleData> {
        return try {
            val doc = fetchDocWithRetry(url)
            val scheduleItems = mutableListOf<WeeklyScheduleData>()
            fun parseTables(fromDoc: org.jsoup.nodes.Document) {
                fromDoc.select("table").forEach { table ->
                    val rows = table.select("tr")
                    val tableRows = mutableListOf<ScheduleTableRow>()
                    rows.forEach { row ->
                        val cells = row.select("td, th")
                        if (cells.isNotEmpty()) {
                            val scheduleCells = cells.map { ScheduleCellContent(normalizeSpaces(it.text()), it.attr("rowspan").toIntOrNull() ?: 1, it.attr("colspan").toIntOrNull() ?: 1) }
                            tableRows.add(ScheduleTableRow(row.selectFirst("th") != null, scheduleCells))
                        }
                    }
                    if (tableRows.isNotEmpty()) scheduleItems.add(WeeklyScheduleData(null, tableRows))
                }
            }
            parseTables(doc)
            if (scheduleItems.isEmpty()) {
                collectRaspisanieLinks(doc).filter { it != url }.forEach { nestedUrl ->
                    parseTables(fetchDocWithRetry(nestedUrl))
                    delay(250)
                }
            }
            scheduleItems
        } catch (e: Exception) { Timber.e(e, "Failed to scrape teacher schedule"); emptyList() }
    }

    suspend fun scrapeAboutContent(): AboutContent {
        return try {
            val doc = Jsoup.connect("https://kubmi.ru/").timeout(15000).get()
            val aboutText = doc.selectFirst("div.entry-content p:contains(Кубанский)")?.text()?.trim() ?: "Информация о институте недоступна."
            val address = doc.select("p:contains(г. Краснодар)").firstOrNull()?.text()?.trim() ?: ""
            AboutContent(history = aboutText, management = emptyList(), faculties = emptyList(), contacts = ContactInfo(address = address, phone = "", email = "", website = "https://kubmi.ru/"), achievements = emptyList())
        } catch (e: Exception) { Timber.e(e, "Error scraping about content"); AboutContent("Ошибка загрузки.", emptyList(), emptyList(), ContactInfo("", "", "", ""), emptyList()) }
    }

    suspend fun scrapeGoChsPdfs(): List<News> {
        Log.d("ScreensaverDebug", "Scraping GO CHS PDFs from $GO_CHS_URL")
        return try {
            val doc = fetchDoc(GO_CHS_URL)
            val container = doc.select(".ecom-inner").first() ?: doc.body()
            Log.d("ScreensaverDebug", "Container found: ${container.className()}")
            val pdfLinks = container.select("a[href$=.pdf]")
            Log.d("ScreensaverDebug", "Found ${pdfLinks.size} PDF links")
            pdfLinks.map { link ->
                val title = link.text().trim().ifBlank { "Памятка ГО и ЧС" }
                val url = link.absUrl("href")
                Log.d("ScreensaverDebug", "PDF Link: $title -> $url")
                News(id = url, title = title, description = "Материалы ГО и ЧС", content = "Информационный материал в формате PDF", date = "ГО и ЧС", imageUrl = url)
            }
        } catch (e: Exception) { 
            Log.e("ScreensaverDebug", "Failed to scrape GO i CHS PDFs", e)
            Timber.e(e, "Failed to scrape GO i CHS PDFs"); emptyList() 
        }
    }

    suspend fun parseGroupsFromPage(doc: org.jsoup.nodes.Document): List<String> {
        val links = doc.select("main table a[href], article table a[href], .entry-content table a[href], figure.wp-block-table table a[href]").ifEmpty { doc.select("table a[href]") }
        return links.map { normalizeSpaces(it.text()) }.filter { it.isNotBlank() }.distinct()
    }

    suspend fun parseTeachersFromPage(doc: org.jsoup.nodes.Document): List<String> {
        val links = doc.select("main table a[href], article table a[href], .entry-content table a[href], figure.wp-block-table table a[href]").ifEmpty { doc.select("table a[href]") }
        return links.map { normalizeTeacherDisplay(it.text()) }.filter { it.isNotBlank() }.distinct()
    }

    private fun determineLessonType(subject: String): LessonType {
        val lower = subject.lowercase()
        return when {
            lower.contains("лаб") -> LessonType.LAB
            lower.contains("прак") -> LessonType.PRACTICE
            else -> LessonType.LECTURE
        }
    }
}
