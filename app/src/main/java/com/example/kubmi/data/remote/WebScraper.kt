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
import java.time.format.DateTimeFormatter
import java.net.URI
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebScraper @Inject constructor() {
    private companion object {
        const val INSTITUTE_HISTORY_URL = "https://kubmi.ru/institut/istoriya-instituta/"
    }

    // Кэшируем часто используемые селекторы
    // News grid on the panel page is rendered by Essential Addons (EAEL):
    // <div class="eael-post-block-grid eael-post-appender eael-post-appender-02a6e68"> ... <article> ... </article>
    private val newsSelector =
        "div.eael-post-block-grid.eael-post-appender.eael-post-appender-02a6e68, #elementor-element-7d507b9, .news-container"
    private val newsItemSelector = "article.eael-post-block-item, article, .news-item, .post"
    private val titleSelector = "h2 a, h3 a, .eael-entry-title a, h2, h3, .title"
    private val descSelector = ".eael-entry-content p, .eael-post-excerpt, .excerpt, p"
    private val dateSelector = "time, .date, .eael-entry-meta, .eael-post-meta"
    private val imgSelector = "img"

    private fun normalizeSpaces(input: String): String = input.trim().replace(Regex("\\s+"), " ")
    private fun normalizeGroupKey(group: String): String = normalizeSpaces(group).uppercase(Locale.ROOT)
    // Keep teacher display "as is" (no trimming of commas etc), only normalize whitespace for matching.
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
        // Follow nested links on schedule pages (group/teacher may link to further sub-pages)
        return doc.select("a[href]")
            .mapNotNull { a ->
                val href = a.absUrl("href").ifBlank { a.attr("href") }
                if (href.isBlank()) return@mapNotNull null
                val lower = href.lowercase(Locale.ROOT)
                val looksLikeScheduleLeaf =
                    (lower.contains("/raspisanie/") || lower.contains("raspisanie")) &&
                        (lower.endsWith(".html") || lower.endsWith(".htm"))
                if (!looksLikeScheduleLeaf) return@mapNotNull null
                // ignore placeholder links used on the panel page
                if (lower.endsWith("/raspisanie/1.htm") || lower.endsWith("/raspisanie/1.html")) return@mapNotNull null
                href
            }
            .distinct()
    }

    private suspend fun fetchDoc(url: String): org.jsoup.nodes.Document {
        return Jsoup.connect(url)
            .followRedirects(true)
            .timeout(30000)
            .maxBodySize(0)
            .referrer("https://kubmi.ru/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.7,en;q=0.6")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .get()
    }

    private suspend fun fetchInstituteHistoryDoc(): org.jsoup.nodes.Document = fetchDoc(INSTITUTE_HISTORY_URL)

    private fun buildHtmlFromContent(contentRoot: Element): String {
        // Remove noisy blocks if present to reduce junk
        contentRoot.select("script, style, nav, header, footer, form, noscript").remove()

        // Keep headings, paragraphs, lists, emphasis, links
        val safelist = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4")
            .removeTags("img") // no images

        val cleaner = org.jsoup.safety.Cleaner(safelist)
        val cleaned = cleaner.clean(contentRoot.ownerDocument()!!)

        // Extract HTML of the cleaned body/content section
        val html = cleaned.selectFirst("body")?.html().orEmpty()
        return html.ifBlank { "История института недоступна." }
    }

    private enum class OwnerKind { GROUP, TEACHER }

    suspend fun parseWeeklyScheduleData(doc: org.jsoup.nodes.Document): List<WeeklyScheduleData> {
        Log.i("KubMI_Scraper", "parseWeeklyScheduleData() called")
        val weeklySchedules = mutableListOf<WeeklyScheduleData>()

        // Find all tables that seem to contain schedule data
        val tables = doc.select("table").filter { table ->
            // Heuristic: check if the table has at least one row with multiple columns
            table.select("tr").any { it.select("td, th").size > 2 }
        }

        Log.i("KubMI_Scraper", "Found ${tables.size} potential schedule tables")

        tables.forEach { table ->
            val tableRows = mutableListOf<ScheduleTableRow>()
            var weekTitle: String? = null

            // Try to find a week title from preceding elements or within the table
            val titleElement = table.previousElementSibling()
            if (titleElement != null && titleElement.tagName() == "h2") {
                weekTitle = titleElement.text().trim()
            }
            if (weekTitle == null) {
                // Fallback to searching within the table or nearby if not found before
                weekTitle = table.selectFirst("caption, h2, h3")?.text()?.trim()
                if (weekTitle != null && weekTitle.lowercase(Locale.ROOT).startsWith("расписание занятий")) {
                    // If it's a general schedule title, make it null to not repeat
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

        Log.i("KubMI_Scraper", "Parsed ${weeklySchedules.size} weekly schedules")
        return weeklySchedules
    }
    
    suspend fun scrapeStudentScheduleByUrl(groupTitle: String, url: String): List<WeeklyScheduleData> {
        Log.i("KubMI_Scraper", "scrapeStudentScheduleByUrl: group=$groupTitle, url=$url")
        val doc = fetchDoc(url)
        val result = parseWeeklyScheduleData(doc)
        Log.i("KubMI_Scraper", "scrapeStudentScheduleByUrl: found ${result.size} weekly schedules")
        return result
    }

    suspend fun scrapeTeacherScheduleByUrl(teacherTitle: String, url: String): List<WeeklyScheduleData> {
        Log.i("KubMI_Scraper", "scrapeTeacherScheduleByUrl: teacher=$teacherTitle, url=$url")
        val doc = fetchDoc(url)
        val result = parseWeeklyScheduleData(doc)
        Log.i("KubMI_Scraper", "scrapeTeacherScheduleByUrl: found ${result.size} weekly schedules")
        return result
    }

    suspend fun parseGroupEntriesFromPage(doc: org.jsoup.nodes.Document): List<ScheduleIndexEntry> {
        val links = doc.select("figure.wp-block-table table a[href]").ifEmpty {
            doc.select("table a[href]")
        }

        // Preserve order and duplicates, keep title exactly as on the page (no trimming, no normalization)
        return links.map { link ->
            ScheduleIndexEntry(
                title = link.text(),
                url = link.absUrl("href")
            )
        }
    }

    suspend fun parseTeacherEntriesFromPage(doc: org.jsoup.nodes.Document): List<ScheduleIndexEntry> {
        Log.i("KubMI_Scraper", "parseTeacherEntriesFromPage() called")
        val links = doc.select("figure.wp-block-table table a[href]").ifEmpty {
            doc.select("table a[href]")
        }
        Log.i("KubMI_Scraper", "Found ${links.size} teacher links")

        // Preserve order and duplicates, keep title exactly as on the page (no trimming, no normalization)
        // Filter out empty titles and placeholder links like /raspisanie/1.htm
        val entries = links.mapNotNull { link ->
            val title = link.text()
            val url = link.absUrl("href")
            if (title.isNotEmpty() && !url.lowercase(Locale.ROOT).endsWith("/raspisanie/1.htm") && !url.lowercase(Locale.ROOT).endsWith("/raspisanie/1.html")) {
                ScheduleIndexEntry(title = title, url = url)
            } else {
                null
            }
        }
        Log.i("KubMI_Scraper", "parseTeacherEntriesFromPage: ${entries.size} valid entries")
        return entries
    }

    suspend fun parseStudentGroupsTableFromPage(doc: org.jsoup.nodes.Document): com.example.kubmi.domain.model.StudentGroupsTable {
        Log.i("KubMI_Scraper", "parseStudentGroupsTableFromPage() called")
        // Student panel must be parsed from the *exact* 1-6 course table.
        // Choose the table by header signature (1 курс .. 6 курс) rather than heuristics.
        val candidates = doc.select("figure.wp-block-table table, table")
        Log.i("KubMI_Scraper", "Found ${candidates.size} table candidates")
        
        val table = candidates.firstOrNull { t ->
            val firstRow = t.selectFirst("tr") ?: return@firstOrNull false
            val headers = firstRow.select("th, td").map { it.text() }
            headers.size == 6 && headers.all { it.contains("курс") }
        }
        
        if (table == null) {
            Log.w("KubMI_Scraper", "No table with 6 'курс' headers found!")
            return com.example.kubmi.domain.model.StudentGroupsTable(headers = emptyList(), rows = emptyList())
        }
        Log.i("KubMI_Scraper", "Found target table with 6 course headers")

        val trs = table.select("tr")
        if (trs.isEmpty()) {
            Log.w("KubMI_Scraper", "Table has no rows!")
            return com.example.kubmi.domain.model.StudentGroupsTable(headers = emptyList(), rows = emptyList())
        }

        val headerCells = trs.first()!!.select("th, td")
        val headers = headerCells.map { it.text() } // keep as-is
        Log.i("KubMI_Scraper", "Headers: $headers")

        // Parse data rows. Each cell is nullable independently.
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

        val nonEmpty = rows.sumOf { r -> r.count { it != null } }
        val tableLinks = table.select("a[href]").size
        Log.i("KubMI_Scraper", "StudentGroupsTable parsed: headers=${headers.size}, rows=${rows.size}, nonEmptyCells=$nonEmpty, tableLinks=$tableLinks")

        return com.example.kubmi.domain.model.StudentGroupsTable(headers = headers, rows = rows)
    }

    /**
     * Scrapes news from the main panel page.
     * Extracts news from element with ID "elementor-element-7d507b9"
     *
     * @return List of News items or empty list on error
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scrapeNews(): List<News> {
        return try {
            val doc = Jsoup.connect("https://kubmi.ru/stranicza-dlya-panelej/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            // Prefer EAEL news grid (this is the block the user requested)
            val eaelSelector = "div.eael-post-block-grid.eael-post-appender.eael-post-appender-02a6e68 article"
            val eaelArticles = doc.select(eaelSelector)

            val newsElements = if (eaelArticles.isNotEmpty()) {
                Log.i("KubMI_Scraper", "scrapeNews(): using EAEL selector, articles=${eaelArticles.size}")
                eaelArticles
            } else {
                // Fallback: previous container-based approach (best effort)
                val newsContainer = doc.select(newsSelector).first()
                Log.i(
                    "KubMI_Scraper",
                    "scrapeNews(): EAEL not found, containerFound=${newsContainer != null}, selector=$newsSelector"
                )
                val fallback = newsContainer?.select(newsItemSelector) ?: emptyList()
                Log.i("KubMI_Scraper", "scrapeNews(): fallback found ${fallback.size} elements")
                fallback
            }

            // Используем sequence для ленивых вычислений
            val parsed = newsElements.asSequence()
                .mapNotNull { element ->
                    parseNewsElement(element)
                }
                .toList()

            Log.i("KubMI_Scraper", "scrapeNews(): parsed ${parsed.size} news items")
            parsed

        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape news from kubmi.ru/stranicza-dlya-panelej/")
            emptyList()
        }
    }

    data class NewsArticleScrapeResult(
        val title: String,
        val blocks: List<NewsContentBlock>,
        val fullText: String
    )

    /**
     * Scrape a single news article page and extract content blocks (text + inline images).
     * This is used for the detailed news screen (full article reading).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scrapeNewsArticle(url: String): NewsArticleScrapeResult {
        val doc = fetchDoc(url)

        // Title from page H1.
        val title = normalizeSpaces(doc.selectFirst("h1")?.text().orEmpty())
        
        fun isInsideBadContainer(el: Element): Boolean {
            // Don't pick nav/header/footer/sidebar containers.
            val badTags = setOf("header", "footer", "nav", "aside")
            if (badTags.contains(el.tagName().lowercase(Locale.ROOT))) return true
            if (el.parents().any { badTags.contains(it.tagName().lowercase(Locale.ROOT)) }) return true
            return false
        }
        
        fun scoreCandidate(el: Element): Int {
            val text = normalizeSpaces(el.text())
            val textLen = text.length
            val pCount = el.select("p").size
            val liCount = el.select("li").size
            val quoteCount = el.select("blockquote").size
            val imgCount = el.select("img").size
            val hasBadHeadline = text.contains("Другие посты", ignoreCase = true) ||
                text.contains("Поделиться", ignoreCase = true) ||
                text.contains("Последние новости", ignoreCase = true)
            val looksLikeOnlyImage = textLen < 80 && imgCount > 0 && (pCount + liCount) == 0
            var score = textLen + 60 * (pCount + liCount) + 25 * quoteCount
            if (looksLikeOnlyImage) score -= 800
            if (hasBadHeadline) score -= 400
            // Prefer "content" containers over generic wrappers.
            val cls = el.className().lowercase(Locale.ROOT)
            if (cls.contains("entry-content") || cls.contains("post-content") || cls.contains("post-content")) score += 250
            if (cls.contains("elementor-text-editor")) score += 200
            if (cls.contains("elementor-widget-theme-post-content")) score += 250
            return score
        }
        
        fun selectContentRoot(): Element {
            val scopes = buildList {
                doc.selectFirst("article")?.let { add(it) }
                add(doc)
            }

            // Fast-path: Elementor post content widget is usually the most correct root.
            scopes.asSequence()
                .flatMap { it.select(".elementor-widget-theme-post-content").asSequence() }
                .filterNot { isInsideBadContainer(it) }
                .filter { el ->
                    val txtLen = normalizeSpaces(el.text()).length
                    val blocks = el.select("p, h2, h3, h4, h5, h6, blockquote, li").size
                    txtLen >= 120 || blocks >= 4
                }
                .firstOrNull()
                ?.let { return it }

            val selectors = listOf(
                ".entry-content",
                ".post-content",
                ".elementor-widget-theme-post-content",
                ".elementor-widget-theme-post-content .elementor-widget-container",
                ".elementor-text-editor",
                ".elementor-widget-theme-post-content .elementor-text-editor",
            )
            
            val candidates = LinkedHashSet<Element>()
            for (sel in selectors) {
                for (scope in scopes) {
                    scope.select(sel).forEach { el ->
                        if (!isInsideBadContainer(el)) {
                            candidates.add(el)
                        }
                    }
                }
            }
            
            // Very weak fallback: any container with meaningful amount of text.
            if (candidates.isEmpty()) {
                for (scope in scopes) {
                    scope.select(".elementor-widget-container").forEach { el ->
                        if (!isInsideBadContainer(el) && normalizeSpaces(el.text()).length >= 120) {
                            candidates.add(el)
                        }
                    }
                }
            }
            
            val best = candidates.maxByOrNull { scoreCandidate(it) } ?: (doc.selectFirst("article") ?: doc)

            // Debug: log top candidates and what was selected (helps diagnose "blocks=2" cases).
            runCatching {
                data class Cand(val score: Int, val tag: String, val cls: String, val textLen: Int, val p: Int, val h: Int, val img: Int)
                val top = candidates
                    .asSequence()
                    .map { el ->
                        val score = scoreCandidate(el)
                        val tag = el.tagName()
                        val cls = el.className()
                        val textLen = normalizeSpaces(el.text()).length
                        val p = el.select("p").size
                        val h = el.select("h2,h3,h4,h5,h6").size
                        val img = el.select("img").size
                        Cand(score, tag, cls, textLen, p, h, img)
                    }
                    .sortedByDescending { it.score }
                    .take(5)
                    .toList()
                val bTextLen = normalizeSpaces(best.text()).length
                val bP = best.select("p").size
                val bH = best.select("h2,h3,h4,h5,h6").size
                val bImg = best.select("img").size
                Log.i(
                    "KubMI_Scraper",
                    "scrapeNewsArticle(): selectContentRoot url=$url thread=${Thread.currentThread().name} bestTag=${best.tagName()} bestClass=${best.className()} bestTextLen=$bTextLen bestP=$bP bestH=$bH bestImg=$bImg top=$top"
                )
            }

            return best
        }
        
        val contentRoot = selectContentRoot()
        
        // Remove obvious non-article noise inside the chosen root.
        contentRoot.select("script, style, noscript, header, footer, nav, aside, form, button").remove()
        contentRoot.select(
            ".sharedaddy, .jp-relatedposts, .related, .yarpp-related, .post-navigation, .nav-links, .comments-area, .comment-respond"
        ).remove()

        val blocks = mutableListOf<NewsContentBlock>()

        fun resolveUrl(raw: String): String? {
            val r = raw.trim()
            if (r.isBlank()) return null
            if (r.startsWith("http://") || r.startsWith("https://")) return r
            if (r.startsWith("//")) return "https:$r"
            return try {
                URI(doc.baseUri()).resolve(r).toString()
            } catch (_: Exception) {
                null
            }
        }

        fun extractImageUrl(img: Element): String? {
            fun isPlaceholder(u: String): Boolean {
                val t = u.trim()
                if (t.isBlank()) return true
                val lower = t.lowercase(Locale.ROOT)
                if (lower.startsWith("data:")) return true
                if (lower == "about:blank") return true
                if (lower.startsWith("blob:")) return true
                return false
            }

            fun pickFirstNonPlaceholder(vararg rawCandidates: String): String? {
                for (raw in rawCandidates) {
                    val r = raw.trim()
                    if (r.isBlank()) continue
                    val resolved = resolveUrl(r) ?: continue
                    if (!isPlaceholder(resolved)) return resolved
                }
                return null
            }

            fun bestFromSrcset(srcSetRaw: String): String? {
                val srcSet = srcSetRaw.trim()
                if (srcSet.isBlank()) return null

                data class Candidate(val url: String, val weight: Double)
                val candidates = srcSet.split(",")
                    .mapNotNull { part ->
                        val p = part.trim()
                        if (p.isBlank()) return@mapNotNull null
                        val pieces = p.split(Regex("\\s+"))
                        val u = pieces.getOrNull(0).orEmpty()
                        val d = pieces.getOrNull(1).orEmpty()
                        val weight = when {
                            d.endsWith("w") -> d.removeSuffix("w").toDoubleOrNull() ?: 0.0
                            d.endsWith("x") -> (d.removeSuffix("x").toDoubleOrNull() ?: 0.0) * 1000.0
                            else -> 0.0
                        }
                        if (u.isBlank()) null else Candidate(u, weight)
                    }

                // Prefer a "medium" image to reduce download/decode time:
                // - if we have 3+ candidates, pick the median by weight (usually ~768w/1024w)
                // - if 2 candidates, pick the larger one (usually acceptable)
                // - else pick the only one
                val sorted = candidates.sortedBy { it.weight }
                val pickedUrl = when {
                    sorted.size >= 3 -> sorted[sorted.size / 2].url
                    sorted.size == 2 -> sorted.last().url
                    else -> sorted.firstOrNull()?.url
                } ?: return null

                val resolved = resolveUrl(pickedUrl) ?: return null
                return if (isPlaceholder(resolved)) null else resolved
            }

            // Prefer lazy-load attributes over `src` because many pages use placeholder `src=data:image/svg+xml,...`.
            val dataLazySrc = img.absUrl("data-lazy-src").ifBlank { img.attr("data-lazy-src") }
            val dataSrc = img.absUrl("data-src").ifBlank { img.attr("data-src") }
            val dataOriginal = img.absUrl("data-original").ifBlank { img.attr("data-original") }
            val src = img.absUrl("src").ifBlank { img.attr("src") }

            pickFirstNonPlaceholder(dataLazySrc, dataSrc, dataOriginal, src)?.let { return it }

            // srcset variants (including lazy-load srcsets)
            bestFromSrcset(img.attr("data-lazy-srcset"))?.let { return it }
            bestFromSrcset(img.attr("data-srcset"))?.let { return it }
            bestFromSrcset(img.attr("srcset"))?.let { return it }

            return null
        }

        fun extractVideoUrl(el: Element): String? {
            fun isVideoUrl(u: String): Boolean {
                val lower = u.lowercase(Locale.ROOT)
                return lower.contains("youtube.com") ||
                    lower.contains("youtu.be") ||
                    lower.contains("rutube.") ||
                    lower.contains("vk.com/video") ||
                    lower.contains("vimeo.com") ||
                    lower.contains("ok.ru/video") ||
                    lower.contains("dzen.ru/video") ||
                    lower.contains("/embed") ||
                    lower.contains("player")
            }

            fun candidateFromAttr(attr: String): String? {
                val v = el.absUrl(attr).ifBlank { el.attr(attr) }.trim()
                if (v.isBlank()) return null
                val resolved = resolveUrl(v) ?: return null
                return if (isVideoUrl(resolved)) resolved else null
            }

            return when (el.tagName().lowercase(Locale.ROOT)) {
                "iframe" -> {
                    candidateFromAttr("data-lazy-src")
                        ?: candidateFromAttr("data-src")
                        ?: candidateFromAttr("src")
                }
                "video" -> {
                    val direct = candidateFromAttr("src")
                    if (direct != null) direct
                    else {
                        val source = el.selectFirst("source[src]")
                        if (source != null) {
                            val v = source.absUrl("src").ifBlank { source.attr("src") }.trim()
                            val resolved = resolveUrl(v) ?: return null
                            if (isVideoUrl(resolved)) resolved else null
                        } else null
                    }
                }
                else -> null
            }
        }

        var lastTextBlock: String? = null
        
        fun addTextBlock(text: String) {
            val t = normalizeSpaces(text)
            if (t.isBlank()) return
            if (t == lastTextBlock) return
            blocks.add(NewsContentBlock(type = NewsContentBlock.TYPE_TEXT, text = t))
            lastTextBlock = t
        }
        
        fun addImageBlock(img: Element) {
            val imageUrl = extractImageUrl(img)
            if (!imageUrl.isNullOrBlank()) {
                blocks.add(
                    NewsContentBlock(
                        type = NewsContentBlock.TYPE_IMAGE,
                        imageUrl = imageUrl,
                        alt = img.attr("alt").trim().ifBlank { null }
                    )
                )
            }
        }

        fun addVideoBlock(url: String) {
            val u = url.trim()
            if (u.isBlank()) return
            blocks.add(
                NewsContentBlock(
                    type = NewsContentBlock.TYPE_VIDEO,
                    videoUrl = u
                )
            )
        }

        fun walk(el: Element) {
            val tag = el.tagName().lowercase(Locale.ROOT)

            when (tag) {
                "script", "style", "noscript" -> return
                "iframe", "video" -> {
                    val v = extractVideoUrl(el)
                    if (!v.isNullOrBlank()) addVideoBlock(v)
                    return
                }
                "img" -> {
                    addImageBlock(el)
                    return
                }
                "p", "h2", "h3", "h4", "h5", "h6", "blockquote" -> {
                    addTextBlock(el.text())
                    // Capture inline images, but avoid descending to nested text nodes to prevent duplicates.
                    el.select("img").forEach { img -> addImageBlock(img) }
                    return
                }
                "li" -> {
                    // treat list items as text blocks to preserve bullet semantics
                    addTextBlock("• ${el.text()}")
                    return
                }
            }

            el.children().forEach { child -> walk(child) }
        }

        walk(contentRoot)

        // Combine text blocks into plain full text (best-effort)
        val fullText = blocks
            .asSequence()
            .filter { it.type == NewsContentBlock.TYPE_TEXT }
            .mapNotNull { it.text?.trim()?.takeIf { t -> t.isNotBlank() } }
            .joinToString("\n\n")

        Log.i("KubMI_Scraper", "scrapeNewsArticle(): url=$url blocks=${blocks.size} fullTextLen=${fullText.length}")
        return NewsArticleScrapeResult(title = title, blocks = blocks, fullText = fullText)
    }

    private fun parseNewsElement(element: Element): News? {
        // EAEL structure: <p class="eael-entry-title"><a ...>TITLE</a>
        // Fallback: previous heuristics.
        val titleLink = element.selectFirst(".eael-entry-title a[href], p.eael-entry-title a[href], p a[href], $titleSelector")
        val title = titleLink?.text()?.trim().orEmpty()
        if (title.isBlank()) return null

        val url = titleLink?.absUrl("href").orEmpty()

        // Panel page structure: first <p> contains title link, second <p> contains excerpt, then <time>
        val paragraphs = element.select("p")
        val description = paragraphs.getOrNull(1)?.text()?.trim()
            ?: element.selectFirst(".eael-entry-content p, .eael-grid-post-excerpt p, .eael-post-excerpt, .excerpt")?.text()?.trim()
            ?: ""

        val date = element.selectFirst("time")?.text()?.trim()
            ?: element.selectFirst(dateSelector)?.text()?.trim()
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val imageUrl = element.selectFirst(imgSelector)?.absUrl("src")?.ifBlank { null }

        // Use stable ID based on URL when available (prevents empty detail screen after refresh)
        val id = if (url.isNotBlank()) url else UUID.randomUUID().toString()

        val content = buildString {
            if (description.isNotBlank()) append(description)
            // keep full text for details as best-effort without fetching the article page
            val extra = element.text().trim()
            if (extra.isNotBlank() && extra != description && extra != title) {
                if (isNotEmpty()) append("\n\n")
                append(extra)
            }
        }

        return News(
            id = id,
            title = title,
            description = description,
            content = content,
            date = date,
            imageUrl = imageUrl
        )
    }

    /**
     * Scrapes student schedule from the panel page.
     * First gets the list of all groups, then scrapes schedule from individual group pages.
     *
     * @return List of ScheduleItem or empty list on error
     */
    suspend fun scrapeStudentSchedule(group: String = ""): List<WeeklyScheduleData> {
        return try {
            val doc = Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-studentov-panel/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val scheduleItems = mutableListOf<WeeklyScheduleData>()
            
            // If a specific group is requested, get its schedule from the individual page
            if (group.isNotEmpty()) {
                val groupUrl = findGroupUrl(doc, group)
                if (groupUrl != null) {
                    return getScheduleFromGroupPage(groupUrl, group)
                } else {
                    Timber.w("Group $group not found on the main page")
                    return emptyList()
                }
            } else {
                // Get all groups and their schedules
                val allGroups = parseGroupsFromPage(doc)
                Timber.d("Found ${allGroups.size} groups to process")
                
                for (groupName in allGroups) {
                    val groupUrl = findGroupUrl(doc, groupName)
                    if (groupUrl != null) {
                        val groupSchedule = getScheduleFromGroupPage(groupUrl, groupName)
                        scheduleItems.addAll(groupSchedule)
                        Timber.d("Retrieved ${groupSchedule.size} schedule items for group $groupName")
                        // Add delay to be respectful to the server
                        delay(500)
                    }
                }
            }

            scheduleItems

        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape student schedule")
            emptyList()
        }
    }
    
    /**
     * Finds the URL for a specific group from the main student schedule page
     */
    private suspend fun findGroupUrl(doc: org.jsoup.nodes.Document, group: String): String? {
        val targetKey = normalizeGroupKey(group)

        // Prefer the panel structure: a WordPress block table with links
        val links = doc.select("figure.wp-block-table table a[href]").ifEmpty {
            doc.select("table a[href]")
        }

        links.forEach { link ->
            val linkText = link.text().trim()
            if (linkText.isNotBlank() && normalizeGroupKey(linkText) == targetKey) {
                return link.absUrl("href")
            }
        }

        return null
    }
    
    /**
     * Gets schedule from a specific group page
     */
    private suspend fun getScheduleFromGroupPage(url: String, group: String): List<WeeklyScheduleData> {
        return try {
            Timber.d("Fetching schedule from: $url for group: $group")
            val doc = Jsoup.connect(url)
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val scheduleItems = mutableListOf<WeeklyScheduleData>()

            // Parse the schedule table from the individual group page
            fun parseTables(fromDoc: org.jsoup.nodes.Document) {
                fromDoc.select("table").forEach { table ->
                val headerRow = table.select("tr").firstOrNull { row ->
                    isHeaderRow(row.select("td, th"))
                }
                val headerTexts = headerRow?.select("td, th")?.map { it.text().trim().lowercase(Locale.ROOT) }.orEmpty()

                fun headerIndex(predicate: (String) -> Boolean): Int? {
                    val idx = headerTexts.indexOfFirst(predicate)
                    return if (idx >= 0) idx else null
                }

                val dayIdx = headerIndex { it.contains("день") }
                val timeIdx = headerIndex { it.contains("время") }
                val subjectIdx = headerIndex { it.contains("предмет") || it.contains("дисцип") }
                val roomIdx = headerIndex { it.contains("ауд") || it.contains("каб") || it.contains("аудит") }
                val teacherIdx = headerIndex { it.contains("преподав") }
                val typeIdx = headerIndex { it.contains("тип") || it.contains("вид") }

                var lastDay = ""

                table.select("tr").forEach { row ->
                    val cells = row.select("td, th")
                    // Skip header rows
                    if (cells.isNotEmpty() && !isHeaderRow(cells)) {
                        val texts = cells.map { normalizeSpaces(it.text()) }
                        if (texts.all { it.isBlank() }) return@forEach

                        fun at(idx: Int?): String = if (idx != null && idx in texts.indices) texts[idx] else ""

                        var dayOfWeek = at(dayIdx)
                        if (dayOfWeek.isBlank() && texts.isNotEmpty() && isDayText(texts[0])) dayOfWeek = texts[0]
                        if (dayOfWeek.isBlank()) dayOfWeek = lastDay else lastDay = dayOfWeek

                        val timeSlot = at(timeIdx).ifBlank { texts.firstOrNull { isTimeText(it) }.orEmpty() }

                        val subject = at(subjectIdx).ifBlank {
                            texts.firstOrNull { t ->
                                t.isNotBlank() && !isDayText(t) && !isTimeText(t) && !looksLikeRoom(t)
                            }.orEmpty()
                        }

                        val room = at(roomIdx).ifBlank { texts.firstOrNull { looksLikeRoom(it) }.orEmpty() }

                        val teacher = at(teacherIdx).ifBlank {
                            texts.firstOrNull { t ->
                                t.isNotBlank() &&
                                    t != subject &&
                                    !looksLikeRoom(t) &&
                                    !isDayText(t) &&
                                    !isTimeText(t) &&
                                    (t.contains(" ") || t.contains("."))
                            }.orEmpty()
                        }

                        val typeText = at(typeIdx)
                        val lessonType = if (typeText.isNotBlank()) determineLessonType(typeText) else determineLessonType(subject)

                        // Keep rows "as is": do not drop rows just because some columns are missing
                        val finalDay = dayOfWeek.ifBlank { lastDay.ifBlank { "—" } }
                        val finalTime = timeSlot.ifBlank { "—" }
                        val finalSubject = subject.ifBlank { texts.filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "—" } }

                                scheduleItems.add(
                            WeeklyScheduleData(
                                weekTitle = null, // No specific week title for individual group pages
                                rows = listOf(
                                    ScheduleTableRow(
                                        isHeader = false,
                                        cells = listOf(
                                            ScheduleCellContent(finalDay, 1, 1),
                                            ScheduleCellContent(finalTime, 1, 1),
                                            ScheduleCellContent(finalSubject, 1, 1),
                                            ScheduleCellContent(lessonType.name, 1, 1),
                                            ScheduleCellContent(room, 1, 1),
                                            ScheduleCellContent(teacher, 1, 1)
                                        )
                                    )
                                )
                            )
                        )
                    }
                }
                }
            }

            parseTables(doc)

            // If this page is just an index to nested schedule pages, follow them
            if (scheduleItems.isEmpty()) {
                val nestedUrls = collectRaspisanieLinks(doc).filter { it != url }
                for (nestedUrl in nestedUrls) {
                    try {
                        val nestedDoc = Jsoup.connect(nestedUrl)
                            .timeout(15000)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .get()
                        parseTables(nestedDoc)
                        delay(250)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch nested group schedule page: $nestedUrl")
                    }
                }
            }

            scheduleItems
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape schedule from group page: $url")
            emptyList()
        }
    }
    
    /**
     * Checks if a row is a header row
     */
    private fun isHeaderRow(cells: org.jsoup.select.Elements): Boolean {
        // Check if any cell is a header cell (th) or contains typical header text
        cells.forEach { cell ->
            if (cell.tagName() == "th") return true
            val text = cell.text().trim().lowercase(Locale.ROOT)
            if (text.contains("день") || text.contains("время") || text.contains("предмет") || text.contains("дисцип") ||
                text.contains("ауд") || text.contains("каб") || text.contains("аудит") ||
                text.contains("преподав") || text.contains("группа")) {
                return true
            }
        }
        return false
    }

    /**
     * Scrapes teacher schedule from the panel page.
     * First gets the list of all teachers, then scrapes schedule from individual teacher pages.
     *
     * @return List of ScheduleItem or empty list on error
     */
    suspend fun scrapeTeacherSchedule(teacher: String = ""): List<WeeklyScheduleData> {
        return try {
            val doc = Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-prepodavatelej-panel/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val scheduleItems = mutableListOf<WeeklyScheduleData>()
            
            // If a specific teacher is requested, get their schedule from the individual page
            if (teacher.isNotEmpty()) {
                val teacherUrl = findTeacherUrl(doc, teacher)
                if (teacherUrl != null) {
                    return getScheduleFromTeacherPage(teacherUrl, teacher)
                } else {
                    Timber.w("Teacher $teacher not found on the main page")
                    return emptyList()
                }
            } else {
                // Get all teachers and their schedules
                val allTeachers = parseTeachersFromPage(doc)
                Timber.d("Found ${allTeachers.size} teachers to process")
                
                for (teacherName in allTeachers) {
                    val teacherUrl = findTeacherUrl(doc, teacherName)
                    if (teacherUrl != null) {
                        val teacherSchedule = getScheduleFromTeacherPage(teacherUrl, teacherName)
                        scheduleItems.addAll(teacherSchedule)
                        Timber.d("Retrieved ${teacherSchedule.size} schedule items for teacher $teacherName")
                        // Add delay to be respectful to the server
                        delay(500)
                    }
                }
            }

            scheduleItems

        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape teacher schedule")
            emptyList()
        }
    }
    
    /**
     * Finds the URL for a specific teacher from the main teacher schedule page
     */
    private suspend fun findTeacherUrl(doc: org.jsoup.nodes.Document, teacher: String): String? {
        val targetKey = normalizeTeacherKey(teacher)

        // Prefer the panel structure: a WordPress block table with links
        val links = doc.select("figure.wp-block-table table a[href]").ifEmpty {
            doc.select("table a[href]")
        }

        links.forEach { link ->
            val display = normalizeTeacherDisplay(link.text())
            if (display.isNotBlank() && normalizeTeacherKey(display) == targetKey) {
                return link.absUrl("href")
            }
        }

        return null
    }
    
    /**
     * Gets schedule from a specific teacher page
     */
    private suspend fun getScheduleFromTeacherPage(url: String, teacher: String): List<WeeklyScheduleData> {
        return try {
            Timber.d("Fetching schedule from: $url for teacher: $teacher")
            val doc = Jsoup.connect(url)
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val scheduleItems = mutableListOf<WeeklyScheduleData>()

            // Parse the schedule table from the individual teacher page
            fun parseTables(fromDoc: org.jsoup.nodes.Document) {
                fromDoc.select("table").forEach { table ->
                val headerRow = table.select("tr").firstOrNull { row ->
                    isHeaderRow(row.select("td, th"))
                }
                val headerTexts = headerRow?.select("td, th")?.map { it.text().trim().lowercase(Locale.ROOT) }.orEmpty()

                fun headerIndex(predicate: (String) -> Boolean): Int? {
                    val idx = headerTexts.indexOfFirst(predicate)
                    return if (idx >= 0) idx else null
                }

                val dayIdx = headerIndex { it.contains("день") }
                val timeIdx = headerIndex { it.contains("время") }
                val subjectIdx = headerIndex { it.contains("предмет") || it.contains("дисцип") }
                val roomIdx = headerIndex { it.contains("ауд") || it.contains("каб") || it.contains("аудит") }
                val groupIdx = headerIndex { it.contains("групп") }
                val typeIdx = headerIndex { it.contains("тип") || it.contains("вид") }

                var lastDay = ""

                table.select("tr").forEach { row ->
                    val cells = row.select("td, th")
                    // Skip header rows
                    if (cells.isNotEmpty() && !isHeaderRow(cells)) {
                        val texts = cells.map { normalizeSpaces(it.text()) }
                        if (texts.all { it.isBlank() }) return@forEach

                        fun at(idx: Int?): String = if (idx != null && idx in texts.indices) texts[idx] else ""

                        var dayOfWeek = at(dayIdx)
                        if (dayOfWeek.isBlank() && texts.isNotEmpty() && isDayText(texts[0])) dayOfWeek = texts[0]
                        if (dayOfWeek.isBlank()) dayOfWeek = lastDay else lastDay = dayOfWeek

                        val timeSlot = at(timeIdx).ifBlank { texts.firstOrNull { isTimeText(it) }.orEmpty() }

                        val subject = at(subjectIdx).ifBlank {
                            texts.firstOrNull { t ->
                                t.isNotBlank() && !isDayText(t) && !isTimeText(t) && !looksLikeRoom(t)
                            }.orEmpty()
                        }

                        val room = at(roomIdx).ifBlank { texts.firstOrNull { looksLikeRoom(it) }.orEmpty() }

                        val group = at(groupIdx).ifBlank {
                            // Best-effort: pick remaining value (not day/time/subject/room)
                            texts.firstOrNull { t ->
                                t.isNotBlank() &&
                                    t != subject &&
                                    !looksLikeRoom(t) &&
                                    !isDayText(t) &&
                                    !isTimeText(t)
                            }.orEmpty()
                        }

                        val typeText = at(typeIdx)
                        val lessonType = if (typeText.isNotBlank()) determineLessonType(typeText) else determineLessonType(subject)

                        val finalDay = dayOfWeek.ifBlank { lastDay.ifBlank { "—" } }
                        val finalTime = timeSlot.ifBlank { "—" }
                        val finalSubject = subject.ifBlank { texts.filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "—" } }

                                scheduleItems.add(
                            WeeklyScheduleData(
                                weekTitle = null, // No specific week title for individual teacher pages
                                rows = listOf(
                                    ScheduleTableRow(
                                        isHeader = false,
                                        cells = listOf(
                                            ScheduleCellContent(finalDay, 1, 1),
                                            ScheduleCellContent(finalTime, 1, 1),
                                            ScheduleCellContent(finalSubject, 1, 1),
                                            ScheduleCellContent(lessonType.name, 1, 1),
                                            ScheduleCellContent(room, 1, 1),
                                            ScheduleCellContent(group, 1, 1)
                                        )
                                    )
                                )
                            )
                        )
                    }
                }
                }
            }

            parseTables(doc)

            if (scheduleItems.isEmpty()) {
                val nestedUrls = collectRaspisanieLinks(doc).filter { it != url }
                for (nestedUrl in nestedUrls) {
                    try {
                        val nestedDoc = Jsoup.connect(nestedUrl)
                            .timeout(15000)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .get()
                        parseTables(nestedDoc)
                        delay(250)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch nested teacher schedule page: $nestedUrl")
                    }
                }
            }

            scheduleItems
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape schedule from teacher page: $url")
            emptyList()
        }
    }

    /**
     * Debug function to inspect student panel page content.
     */
    suspend fun debugStudentPage(): String {
        return try {
            val doc = Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-studentov-panel/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val tableContent = StringBuilder()
            doc.select("table").forEach { table ->
                table.select("tr").forEach { row ->
                    val cells = row.select("td, th")
                    tableContent.append("Row: ${cells.map { it.text().trim() }}\n")
                }
            }

            "Page Title: ${doc.title()}\nURL: ${doc.location()}\n\nTable Content:\n$tableContent"

        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Returns mock schedule data for fallback.
     */
    private fun getMockSchedule(): List<WeeklyScheduleData> = listOf(
        WeeklyScheduleData(
            weekTitle = "Неделя 1",
            rows = listOf(
                ScheduleTableRow(
                    isHeader = false,
                    cells = listOf(
                        ScheduleCellContent("Понедельник", 1, 1),
                        ScheduleCellContent("09:00-10:30", 1, 1),
                        ScheduleCellContent("Математика", 1, 1),
                        ScheduleCellContent("Лекция", 1, 1),
                        ScheduleCellContent("Ауд. 101", 1, 1),
                        ScheduleCellContent("Иванов И.И.", 1, 1)
                    )
                ),
                ScheduleTableRow(
                    isHeader = false,
                    cells = listOf(
                        ScheduleCellContent("Вторник", 1, 1),
                        ScheduleCellContent("10:45-12:15", 1, 1),
                        ScheduleCellContent("Физика", 1, 1),
                        ScheduleCellContent("Практика", 1, 1),
                        ScheduleCellContent("Ауд. 102", 1, 1),
                        ScheduleCellContent("Петров П.П.", 1, 1)
                    )
                )
            )
        )
    )

    /**
     * Scrapes basic about information from the main page.
     */
    suspend fun scrapeAboutContent(): AboutContent {
        return try {
            val doc = Jsoup.connect("https://kubmi.ru/")
                .timeout(15000)
                .get()

            val aboutText = doc.selectFirst("div.entry-content p:contains(Кубанский)")
                ?.text()?.trim() ?: "Информация о институте недоступна."

            val address = doc.select("p:contains(г. Краснодар)")
                .firstOrNull()?.text()?.trim() ?: ""

            AboutContent(
                history = aboutText,
                management = emptyList(), // Would require separate page
                faculties = emptyList(),  // Would require separate page
                contacts = ContactInfo(
                    address = address,
                    phone = "",
                    email = "",
                    website = "https://kubmi.ru/"
                ),
                achievements = emptyList() // Would require separate page
            )
        } catch (e: Exception) {
            Timber.e(e, "Error scraping about content")
            getFallbackAboutContent()
        }
    }

    private fun getFallbackAboutContent(): AboutContent {
        return AboutContent(
            history = "Ошибка загрузки информации.",
            management = emptyList(),
            faculties = emptyList(),
            contacts = ContactInfo(
                address = "Информация недоступна",
                phone = "",
                email = "",
                website = ""
            ),
            achievements = emptyList()
        )
    }
    
    /**
     * Scrapes history information from the about page.
     */
    suspend fun scrapeHistory(): String {
        return try {
            val doc = fetchInstituteHistoryDoc()
            val contentRoot = doc.selectFirst("article .entry-content, main .entry-content, .entry-content")
                ?: doc.selectFirst("article, main")
                ?: doc.body()

            buildHtmlFromContent(contentRoot)
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape history")
            "Ошибка загрузки истории."
        }
    }
    
    /**
     * Scrapes management information from the management page.
     */
    suspend fun scrapeManagement(): List<ManagementPerson> {
        return try {
            val doc = fetchInstituteHistoryDoc()
            val managementList = mutableListOf<ManagementPerson>()

            val keywords = listOf("ректор", "проректор", "декан", "завед", "профессор", "доцент", "доктор", "кандидат", "директор", "преподавател")
            val namePattern = Pattern.compile("([А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+)")

            // Prefer bold/strong markers for names, then fall back to paragraph heuristics
            val boldBlocks = doc.select("article .entry-content strong, article .entry-content b, .entry-content strong, .entry-content b")
            boldBlocks.forEach { el ->
                val name = normalizeSpaces(el.text())
                val parentText = normalizeSpaces(el.parent()?.text() ?: "")
                val lower = parentText.lowercase(Locale.ROOT)
                if (name.length in 5..80 && keywords.any { lower.contains(it) }) {
                    // Remove name from the rest to avoid duplication
                    val rest = parentText.removePrefix(name).trim().trim(',')
                    managementList.add(ManagementPerson(name = name, position = "", bio = rest))
                }
            }

            // Paragraph fallback
            val candidates = doc.select("article .entry-content p, .entry-content p")
                .map { normalizeSpaces(it.text()) }
                .filter { it.length in 20..240 }

            candidates.forEach { line ->
                val lower = line.lowercase(Locale.ROOT)
                if (keywords.any { lower.contains(it) }) {
                    val matcher = namePattern.matcher(line)
                    val name = if (matcher.find()) matcher.group(1) else line.split(",", "—", "-", "–").firstOrNull().orEmpty()
                    val rest = line.removePrefix(name).trim().trim(',', '—', '-', '–')
                    if (name.length in 5..120) {
                        managementList.add(ManagementPerson(name = name, position = "", bio = rest))
                    }
                }
            }

            managementList.distinctBy { normalizeSpaces(it.name).lowercase(Locale.ROOT) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape management")
            emptyList()
        }
    }
    
    /**
     * Scrapes faculties information from the faculties page.
     */
    suspend fun scrapeFaculties(): List<AboutFaculty> {
        return try {
            val doc = fetchInstituteHistoryDoc()
            val facultyList = mutableListOf<AboutFaculty>()

            // The history page includes navigation lists: "Факультеты" and "Кафедры" in menus.
            // Try to extract faculty-like items from the first menu block.
            val menu = doc.selectFirst("nav, .menu, .navbar, header") ?: doc
            val facultyNames = menu.select("a")
                .map { normalizeSpaces(it.text()) }
                .filter { it.isNotBlank() }
                .filter { it.lowercase(Locale.ROOT) in listOf("лечебное дело", "педиатрия", "стоматология") }
                .distinct()

            facultyNames.forEach { name ->
                facultyList.add(AboutFaculty(name = name, description = "", departments = emptyList()))
            }

            facultyList
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape faculties")
            emptyList()
        }
    }
    
    /**
     * Scrapes contact information from the contacts page.
     */
    suspend fun scrapeContacts(): ContactInfo {
        return try {
            val doc = fetchInstituteHistoryDoc()

            val text = normalizeSpaces(doc.text())
            val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(text)?.value ?: ""
            val phone = Regex("\\+7\\s*\\(\\d{3}\\)\\s*\\d{3}-\\d{2}-\\d{2}").find(text)?.value ?: ""
            val address = Regex("\\d{6}\\s*г\\.?\\s*Краснодар[^,]{0,80},\\s*ул\\.?\\s*[^,]{0,60},\\s*\\d+[А-Яа-яA-Za-z0-9/-]*")
                .find(text)?.value
                ?: Regex("г\\.?\\s*Краснодар[^,]{0,120},\\s*ул\\.?\\s*[^,]{0,60},\\s*\\d+[А-Яа-яA-Za-z0-9/-]*")
                    .find(text)?.value
                ?: ""

            ContactInfo(
                address = address,
                phone = phone,
                email = email,
                website = "https://kubmi.ru/"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape contacts")
            ContactInfo("", "", "", "https://kubmi.ru/")
        }
    }
    
    /**
     * Determines lesson type based on subject content.
     */
    private fun determineLessonType(subject: String): LessonType {
        val lowerSubject = subject.lowercase()
        return when {
            lowerSubject.contains("лаб") || lowerSubject.contains("лабораторн") -> LessonType.LAB
            lowerSubject.contains("прак") || lowerSubject.contains("практич") -> LessonType.PRACTICE
            else -> LessonType.LECTURE
        }
    }
    
    /**
     * Scrapes achievements information from the achievements page.
     */
    suspend fun scrapeAchievements(): List<Achievement> {
        return try {
            val achievementList = mutableListOf<Achievement>()
            val doc = fetchInstituteHistoryDoc()
            val contentRoot = doc.selectFirst("article .entry-content, main .entry-content, .entry-content") ?: doc
            val contentText = normalizeSpaces(contentRoot.text())

            // Extract a few key numbers if present (the page often lists them in a single paragraph).
            val patterns = listOf(
                "кафедр" to Regex("(\\d+)\\s+кафедр"),
                "преподавателей" to Regex("(\\d+)\\s+преподавател"),
                "патентов" to Regex("(\\d+)\\s+патент"),
                "монографий" to Regex("(\\d+)\\s+монограф"),
                "кандидатских диссертаций" to Regex("(\\d+)\\s+кандидатск"),
                "докторских диссертаций" to Regex("(\\d+)\\s+докторск")
            )

            patterns.forEach { (label, re) ->
                val v = re.find(contentText)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (v != null) {
                    achievementList.add(
                        Achievement(
                            title = label.replaceFirstChar { it.uppercase() },
                            description = v.toString(),
                            year = 0
                        )
                    )
                }
            }

            achievementList
        } catch (e: Exception) {
            Timber.e(e, "Failed to scrape achievements")
            emptyList()
        }
    }

    /**
     * Парсит список групп с веб-страницы
     */
    suspend fun parseGroupsFromPage(doc: org.jsoup.nodes.Document): List<String> {
        // Fast path for panel pages: groups are links inside a WP block table
        run {
            val links = doc.select("figure.wp-block-table table a[href]").ifEmpty {
                doc.select("table a[href]")
            }

            val ordered = LinkedHashSet<String>()
            links.forEach { link ->
                val text = normalizeSpaces(link.text())
                if (text.isNotBlank()) {
                    ordered.add(text)
                }
            }

            if (ordered.isNotEmpty()) {
                return ordered.toList()
                    }
                }
                
        // Fallback: no filters, collect elements as-is
        val ordered = LinkedHashSet<String>()
        doc.select("select option, table td, table th, a").forEach { el ->
            val text = normalizeSpaces(el.text())
            if (text.isNotBlank()) ordered.add(text)
        }
        return ordered.toList()
    }

    // Метод isGroupName больше не нужен, так как используем extension-функцию

    /**
     * Извлекает потенциальные названия групп из текста
     */
    private fun extractPotentialGroupsFromText(text: String): List<String> {
        val potentialGroups = mutableSetOf<String>()
        
        // Регулярные выражения для поиска потенциальных названий групп
        val groupPatterns = listOf(
            Regex("[A-Z]{1,2}\\d{2,3}[A-Z]*", RegexOption.IGNORE_CASE), // Буквы-цифры-буквы
            Regex("\\d{2,3}[A-Z]{1,2}", RegexOption.IGNORE_CASE), // Цифры-буквы
            Regex("[A-Z]{1,2}-?\\d{2,3}", RegexOption.IGNORE_CASE), // Буквы-цифры (может быть с тире)
            Regex("\\d{2,3}-?[A-Z]{1,2}", RegexOption.IGNORE_CASE) // Цифры-буквы (может быть с тире)
        )
        
        groupPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val group = match.value.trim()
                if (group.length in 3..10) { // Дополнительная проверка длины
                    potentialGroups.add(group)
                }
            }
        }
        
        return potentialGroups.toList()
    }

    /**
     * Парсит список преподавателей с веб-страницы
     */
    suspend fun parseTeachersFromPage(doc: org.jsoup.nodes.Document): List<String> {
        // Fast path for panel pages: teachers are links inside a WP block table
        run {
            val links = doc.select("figure.wp-block-table table a[href]").ifEmpty {
                doc.select("table a[href]")
            }

            val byKey = LinkedHashMap<String, String>() // keep order + dedupe
            links.forEach { link ->
                val display = normalizeTeacherDisplay(link.text())
                if (display.isNotBlank()) {
                    val key = normalizeTeacherKey(display)
                    if (!byKey.containsKey(key)) {
                        byKey[key] = display
                    }
                }
            }

            if (byKey.isNotEmpty()) {
                return byKey.values.toList()
                    }
                }
                
        // Fallback: no filters, collect elements as-is
        val ordered = LinkedHashSet<String>()
        doc.select("select option, table td, table th, a").forEach { el ->
            val text = normalizeSpaces(el.text())
            if (text.isNotBlank()) ordered.add(text)
        }
        return ordered.toList()
    }

    // Метод isTeacherName больше не нужен, так как используем extension-функцию

    /**
     * Извлекает потенциальные имена преподавателей из текста
     */
    private fun extractPotentialTeachersFromText(text: String): List<String> {
        val potentialTeachers = mutableSetOf<String>()
        
        // Регулярные выражения для поиска потенциальных ФИО
        val teacherPatterns = listOf(
            Regex("[А-ЯЁ][а-яё]+\\s+[А-ЯЁ]\\.[А-ЯЁ]\\.", RegexOption.IGNORE_CASE), // Иванов И.И.
            Regex("[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+\\s+[А-ЯЁ]\\.", RegexOption.IGNORE_CASE), // Иванов Иван И.
            Regex("[A-Z][a-z]+\\s+[A-Z]\\.[A-Z]\\.", RegexOption.IGNORE_CASE), // Ivanov I.I.
            Regex("[A-Z][a-z]+\\s+[A-Z][a-z]+\\s+[A-Z]\\.", RegexOption.IGNORE_CASE) // Ivanov Ivan I.
        )
        
        teacherPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val teacher = match.value.trim()
                potentialTeachers.add(teacher)
            }
        }
        
        return potentialTeachers.toList()
    }
}
