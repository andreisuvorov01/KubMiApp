package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.ScheduleDao
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.StudentGroupsTable
import com.example.kubmi.domain.model.WeeklyScheduleData // Updated import
import com.example.kubmi.domain.repository.ScheduleRepository as DomainScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val webScraper: WebScraper
) : DomainScheduleRepository {
    // Removed getScheduleByGroup and getScheduleByTeacher as they are no longer used for detailed schedules
    // Removed debugStudentPage as it's no longer relevant
    // Removed refreshStudentSchedule and refreshTeacherSchedule as they used the old ScheduleItem model

    override suspend fun refreshStudentScheduleByUrl(groupTitle: String, url: String): List<WeeklyScheduleData> = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "refreshStudentScheduleByUrl: group=$groupTitle, url=$url")
        try {
            val schedule = webScraper.scrapeStudentScheduleByUrl(groupTitle, url)
            Log.i("KubMI_Repo", "Parsed ${schedule.size} weekly schedules for group $groupTitle")

            if (schedule.isEmpty()) {
                Log.w("KubMI_Repo", "No weekly schedules found for group $groupTitle")
                return@withContext emptyList()
        }
            // Removed database operations (deleteScheduleByGroup, insertAll) for detailed schedules
            schedule
        } catch (e: Exception) {
            Log.e("KubMI_Repo", "Error refreshing student schedule: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun refreshTeacherScheduleByUrl(teacherTitle: String, url: String): List<WeeklyScheduleData> = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "refreshTeacherScheduleByUrl: teacher=$teacherTitle, url=$url")
        try {
            val schedule = webScraper.scrapeTeacherScheduleByUrl(teacherTitle, url)
            Log.i("KubMI_Repo", "Parsed ${schedule.size} weekly schedules for teacher $teacherTitle")

            if (schedule.isEmpty()) {
                Log.w("KubMI_Repo", "No weekly schedules found for teacher $teacherTitle")
                return@withContext emptyList()
            }
            // Removed database operations (deleteScheduleByTeacher, insertAll) for detailed schedules
            schedule
        } catch (e: Exception) {
            Log.e("KubMI_Repo", "Error refreshing teacher schedule: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getStudentGroupsTable(): StudentGroupsTable = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "getStudentGroupsTable() called")
        // #region agent log
        Log.d("KubMI_Debug", "[A] getStudentGroupsTable: Starting network request")
        // #endregion
        try {
            Log.i("KubMI_Repo", "Connecting to kubmi.ru/raspisanie-zanyatij-studentov-panel/")
            val doc = org.jsoup.Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-studentov-panel/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            // #region agent log
            Log.d("KubMI_Debug", "[A] getStudentGroupsTable: Page fetched successfully, HTML length=${doc.html().length}")
            // #endregion
            Log.i("KubMI_Repo", "Page fetched, parsing table...")

            val table = webScraper.parseStudentGroupsTableFromPage(doc)
            val nonEmpty = table.rows.sumOf { r -> r.count { it != null } }
            // #region agent log
            Log.d("KubMI_Debug", "[C] getStudentGroupsTable: Parsed table - headers=${table.headers.size}, rows=${table.rows.size}, nonEmpty=$nonEmpty")
            // #endregion
            Log.i("KubMI_Repo", "getStudentGroupsTable(): headers=${table.headers.size}, rows=${table.rows.size}, nonEmptyCells=$nonEmpty")
            table
        } catch (e: Exception) {
            // #region agent log
            Log.e("KubMI_Debug", "[D] getStudentGroupsTable: EXCEPTION - ${e.javaClass.simpleName}: ${e.message}")
            // #endregion
            Log.e("KubMI_Repo", "Error parsing student groups table from web: ${e.message}", e)
            StudentGroupsTable(headers = emptyList(), rows = emptyList())
        }
    }

    /**
     * Получает список всех групп с веб-сайта
     */
    override suspend fun getAllGroups(): List<ScheduleIndexEntry> = withContext(Dispatchers.IO) {
        try {
            val doc = org.jsoup.Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-studentov-panel/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            
            val groups = webScraper.parseGroupEntriesFromPage(doc)
            Timber.d("Parsed ${groups.size} group entries from student schedule page")
            groups
        } catch (e: Exception) {
            Timber.e(e, "Error parsing groups from web")
            emptyList()
        }
    }

    /**
     * Получает список всех преподавателей с веб-сайта
     */
    override suspend fun getAllTeachers(): List<ScheduleIndexEntry> = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "getAllTeachers() called")
        // #region agent log
        Log.d("KubMI_Debug", "[A] getAllTeachers: Starting network request")
        // #endregion
        try {
            Log.i("KubMI_Repo", "Connecting to kubmi.ru/raspisanie-zanyatij-prepodavatelej-panel/")
            val doc = org.jsoup.Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-prepodavatelej-panel/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            // #region agent log
            Log.d("KubMI_Debug", "[A] getAllTeachers: Page fetched successfully, HTML length=${doc.html().length}")
            // #endregion
            Log.i("KubMI_Repo", "Teacher page fetched, parsing...")
            
            val teachers = webScraper.parseTeacherEntriesFromPage(doc)
            // #region agent log
            Log.d("KubMI_Debug", "[C] getAllTeachers: Parsed ${teachers.size} teacher entries")
            // #endregion
            Log.i("KubMI_Repo", "getAllTeachers(): ${teachers.size} entries parsed")
            teachers
        } catch (e: Exception) {
            // #region agent log
            Log.e("KubMI_Debug", "[D] getAllTeachers: EXCEPTION - ${e.javaClass.simpleName}: ${e.message}")
            // #endregion
            Log.e("KubMI_Repo", "Error parsing teachers from web: ${e.message}", e)
            emptyList()
        }
    }
    // Removed ScheduleEntity.toDomain() and ScheduleItem.toEntity() as they are no longer used
}
