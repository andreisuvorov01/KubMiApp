package com.example.kubmi.presentation.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.domain.repository.ScheduleRepository
import com.example.kubmi.domain.model.WeeklyScheduleData // Updated import
import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.StudentGroupsTable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _scheduleState = MutableStateFlow<List<WeeklyScheduleData>>(emptyList()) // Changed type
    val scheduleState: StateFlow<List<WeeklyScheduleData>> = _scheduleState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _groupsState = MutableStateFlow<StudentGroupsTable>(StudentGroupsTable(emptyList(), emptyList()))
    val groupsState: StateFlow<StudentGroupsTable> = _groupsState.asStateFlow()

    private val _teachersState = MutableStateFlow<List<ScheduleIndexEntry>>(emptyList())
    val teachersState: StateFlow<List<ScheduleIndexEntry>> = _teachersState.asStateFlow()

    private var scheduleCollectionJob: Job? = null

    fun loadAllGroups() {
        Log.i("KubMI_Schedule", "loadAllGroups() called")
        // #region agent log
        Log.d("KubMI_Debug", "[E] loadAllGroups: Function called")
        // #endregion
        viewModelScope.launch {
            Log.i("KubMI_Schedule", "loadAllGroups() coroutine started")
            // #region agent log
            Log.d("KubMI_Debug", "[E] loadAllGroups: Coroutine started, calling repository")
            // #endregion
            try {
                val table = scheduleRepository.getStudentGroupsTable()
                // #region agent log
                Log.d("KubMI_Debug", "[E] loadAllGroups: Repository returned - headers=${table.headers.size}, rows=${table.rows.size}")
                // #endregion
                Log.i("KubMI_Schedule", "getStudentGroupsTable returned: headers=${table.headers.size}, rows=${table.rows.size}")
                if (table.headers.isEmpty() || table.rows.isEmpty()) {
                    Log.w("KubMI_Schedule", "Student groups table is empty; falling back to mock 1x6 row")
                    _groupsState.value = getMockGroupsTable()
                } else {
                    val nonEmpty = table.rows.sumOf { r -> r.count { it != null } }
                    Log.i("KubMI_Schedule", "Student groups table loaded: headers=${table.headers.size}, rows=${table.rows.size}, nonEmptyCells=$nonEmpty")
                    _groupsState.value = table
                }
            } catch (e: Exception) {
                Log.e("KubMI_Schedule", "Error loading groups: ${e.message}", e)
                _groupsState.value = getMockGroupsTable()
            }
        }
    }

    fun loadAllTeachers() {
        Log.i("KubMI_Schedule", "loadAllTeachers() called")
        // #region agent log
        Log.d("KubMI_Debug", "[E] loadAllTeachers: Function called")
        // #endregion
        viewModelScope.launch {
            Log.i("KubMI_Schedule", "loadAllTeachers() coroutine started")
            // #region agent log
            Log.d("KubMI_Debug", "[E] loadAllTeachers: Coroutine started, calling repository")
            // #endregion
            try {
                val teachers = scheduleRepository.getAllTeachers()
                // #region agent log
                Log.d("KubMI_Debug", "[E] loadAllTeachers: Repository returned ${teachers.size} teachers")
                // #endregion
                Log.i("KubMI_Schedule", "getAllTeachers returned: ${teachers.size} entries")
                if (teachers.isEmpty()) {
                    Log.w("KubMI_Schedule", "Teachers list is empty; falling back to mock")
                    _teachersState.value = getMockTeachers()
                } else {
                    _teachersState.value = teachers
                }
            } catch (e: Exception) {
                Log.e("KubMI_Schedule", "Error loading teachers: ${e.message}", e)
                _teachersState.value = getMockTeachers()
            }
        }
    }

    private fun loadSchedule(
        refreshFromWeb: suspend () -> List<WeeklyScheduleData>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val data = refreshFromWeb()
                _scheduleState.value = data
            } catch (e: Exception) {
                _error.value = e.message ?: "Неизвестная ошибка"
                Timber.e(e, "Error refreshing schedule")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Removed old loadGroupSchedule and loadTeacherSchedule which used Flow from DB

    fun loadGroupScheduleByUrl(groupTitle: String, url: String) {
        loadSchedule(
            refreshFromWeb = { scheduleRepository.refreshStudentScheduleByUrl(groupTitle, url) }
        )
    }

    fun loadTeacherScheduleByUrl(teacherTitle: String, url: String) {
        loadSchedule(
            refreshFromWeb = { scheduleRepository.refreshTeacherScheduleByUrl(teacherTitle, url) }
        )
    }

    // Removed debugStudentPage() or modified its return type if it's still needed.
    // Since we are moving to raw table parsing, the old debug method might not be relevant.
    
    private fun getMockGroupsTable(): StudentGroupsTable {
        // Minimal fallback for dev if network/parsing fails
        val headers = listOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс", "6 курс")
        val rows = listOf(
            listOf(
                ScheduleIndexEntry("L251", ""),
                ScheduleIndexEntry("L241", ""),
                ScheduleIndexEntry("S231", ""),
                ScheduleIndexEntry("L221", ""),
                ScheduleIndexEntry("L211", ""),
                ScheduleIndexEntry("L201", "")
            )
        )
        return StudentGroupsTable(headers = headers, rows = rows)
    }
    
    private fun getMockTeachers() = listOf(
        "Иванов И.И.", "Петров П.П.", "Сидоров С.С.", "Смирнова А.А.",
        "Волков В.В.", "Козлова К.К.", "Морозов М.М.", "Николаева Н.Н.",
        "Федоров Ф.Ф.", "Павлова П.П.", "Семенов С.С.", "Григорьева Г.Г.",
        "Тихонов Т.Т.", "Лебедева Л.Л.", "Зайцев З.З.", "Яковлева Я.Я."
    ).map { ScheduleIndexEntry(title = it, url = "") }
}
