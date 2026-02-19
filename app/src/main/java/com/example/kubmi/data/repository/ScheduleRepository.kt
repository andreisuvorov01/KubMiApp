package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.ScheduleDao
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.util.ParserCache
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
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val webScraper: WebScraper,
    private val parserCache: ParserCache
) : DomainScheduleRepository {
    // Removed getScheduleByGroup and getScheduleByTeacher as they are no longer used for detailed schedules
    // Removed debugStudentPage as it's no longer relevant
    // Removed refreshStudentSchedule and refreshTeacherSchedule as they used the old ScheduleItem model

    override suspend fun refreshStudentScheduleByUrl(groupTitle: String, url: String, forceNetwork: Boolean): List<WeeklyScheduleData> = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "refreshStudentScheduleByUrl: group=$groupTitle, url=$url forceNetwork=$forceNetwork")
        try {
            if (!forceNetwork) {
                val cached = parserCache.readSchedule(ownerType = "group", ownerTitle = groupTitle)
                if (cached != null && cached.isNotEmpty()) {
                    Log.i("KubMI_Repo", "Serving group schedule from cache: ${cached.size} weekly blocks")
                    return@withContext cached
                }
            }

            val schedule = webScraper.scrapeStudentScheduleByUrl(groupTitle, url)
            Log.i("KubMI_Repo", "Parsed ${schedule.size} weekly schedules for group $groupTitle")

            if (schedule.isEmpty()) {
                Log.w("KubMI_Repo", "No weekly schedules found for group $groupTitle")
                return@withContext emptyList()
        }
            parserCache.writeSchedule(ownerType = "group", ownerTitle = groupTitle, data = schedule)
            // Removed database operations (deleteScheduleByGroup, insertAll) for detailed schedules
            schedule
        } catch (e: Exception) {
            Log.e("KubMI_Repo", "Error refreshing student schedule: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun refreshTeacherScheduleByUrl(teacherTitle: String, url: String, forceNetwork: Boolean): List<WeeklyScheduleData> = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "refreshTeacherScheduleByUrl: teacher=$teacherTitle, url=$url forceNetwork=$forceNetwork")
        try {
            if (!forceNetwork) {
                val cached = parserCache.readSchedule(ownerType = "teacher", ownerTitle = teacherTitle)
                if (cached != null && cached.isNotEmpty()) {
                    Log.i("KubMI_Repo", "Serving teacher schedule from cache: ${cached.size} weekly blocks")
                    return@withContext cached
                }
            }

            val schedule = webScraper.scrapeTeacherScheduleByUrl(teacherTitle, url)
            Log.i("KubMI_Repo", "Parsed ${schedule.size} weekly schedules for teacher $teacherTitle")

            if (schedule.isEmpty()) {
                Log.w("KubMI_Repo", "No weekly schedules found for teacher $teacherTitle")
                return@withContext emptyList()
            }
            parserCache.writeSchedule(ownerType = "teacher", ownerTitle = teacherTitle, data = schedule)
            // Removed database operations (deleteScheduleByTeacher, insertAll) for detailed schedules
            schedule
        } catch (e: Exception) {
            Log.e("KubMI_Repo", "Error refreshing teacher schedule: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getStudentGroupsTable(forceNetwork: Boolean): StudentGroupsTable = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "getStudentGroupsTable() called forceNetwork=$forceNetwork")
        
        try {
            if (!forceNetwork) {
                parserCache.readGroupsTable()?.let {
                    Log.i("KubMI_Repo", "Returning student groups from cache: headers=${it.headers.size} rows=${it.rows.size}")
                    return@withContext it
                }
            }

            Log.i("KubMI_Repo", "Connecting to kubmi.ru/raspisanie-zanyatij-studentov/")
            val doc = org.jsoup.Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-studentov/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            
            val table = webScraper.parseStudentGroupsTableFromPage(doc)
            val nonEmpty = table.rows.sumOf { r -> r.count { it != null } }
            parserCache.writeGroupsTable(table)

            Log.i("KubMI_Repo", "getStudentGroupsTable(): headers=${table.headers.size}, rows=${table.rows.size}, nonEmptyCells=$nonEmpty")
            table
        } catch (e: Exception) {
            Log.e("KubMI_Repo", "Error parsing student groups table from web: ${e.message}", e)
            StudentGroupsTable(headers = emptyList(), rows = emptyList())
        }
    }

    /**
     * Получает список всех групп с веб-сайта
     */
    override suspend fun getAllGroups(forceNetwork: Boolean): List<ScheduleIndexEntry> = withContext(Dispatchers.IO) {
        try {
            if (!forceNetwork) {
                // Сначала пытаемся прочитать из полноценной таблицы
                parserCache.readGroupsTable()?.let { table ->
                    val cached = table.rows.flatten().filterNotNull()
                    if (cached.isNotEmpty()) {
                        Timber.d("Parsed ${cached.size} group entries from cached table")
                        return@withContext cached
                    }
                }
            }

            val doc = org.jsoup.Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-studentov/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            
            val groups = webScraper.parseGroupEntriesFromPage(doc)
            
            // НЕ перезаписываем основную таблицу плоским списком, 
            // так как это портит отображение в UI.
            // Вместо этого просто возвращаем список.
            
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
    override suspend fun getAllTeachers(forceNetwork: Boolean): List<ScheduleIndexEntry> = withContext(Dispatchers.IO) {
        Log.i("KubMI_Repo", "getAllTeachers() called forceNetwork=$forceNetwork")
        // #region agent log
        Log.d("KubMI_Debug", "[A] getAllTeachers: Starting network request")
        // #endregion
        try {
            if (!forceNetwork) {
                parserCache.readTeachers()?.takeIf { it.isNotEmpty() }?.let {
                    Log.i("KubMI_Repo", "Returning ${it.size} teachers from cache")
                    return@withContext it
                }
            }

            Log.i("KubMI_Repo", "Connecting to kubmi.ru/raspisanie-zanyatij-prepodavatelej/")
            val doc = org.jsoup.Jsoup.connect("https://kubmi.ru/raspisanie-zanyatij-prepodavatelej/")
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            // #region agent log
            Log.d("KubMI_Debug", "[A] getAllTeachers: Page fetched successfully, HTML length=${doc.html().length}")
            // #endregion
            Log.i("KubMI_Repo", "Teacher page fetched, parsing...")
            
            val teachers = webScraper.parseTeacherEntriesFromPage(doc)
            parserCache.writeTeachers(teachers)
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
