package com.example.kubmi.presentation.screens.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.WeeklyScheduleData
import com.example.kubmi.domain.model.ScheduleTableRow
import com.example.kubmi.domain.model.ScheduleCellContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    navController: NavController,
    type: String,
    title: String,
    url: String,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val weeklyScheduleData by viewModel.scheduleState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(type, title, url) {
        when (type) {
            "group" -> viewModel.loadGroupScheduleByUrl(title, url)
            "teacher" -> viewModel.loadTeacherScheduleByUrl(title, url)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (type) {
                            "group" -> "$title - ${stringResource(R.string.student_schedule)}"
                            "teacher" -> "$title - ${stringResource(R.string.teacher_schedule)}"
                            else -> stringResource(R.string.student_schedule)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.error) + ": $error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        when (type) {
                            "group" -> viewModel.loadGroupScheduleByUrl(title, url)
                            "teacher" -> viewModel.loadTeacherScheduleByUrl(title, url)
                        }
                    }) {
                        Text(stringResource(R.string.refresh))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (weeklyScheduleData.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_schedule_found),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    // Reverse order to show nearest weeks first (from current to past)
                    weeklyScheduleData.reversed().forEachIndexed { weeklyScheduleIndex, weeklySchedule ->
                        if (weeklySchedule.weekTitle != null) {
                            item {
                                Text(
                                    text = weeklySchedule.weekTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        item {
                            val maxCols = remember(weeklySchedule) {
                                weeklySchedule.rows.maxOfOrNull { row -> row.cells.sumOf { it.colSpan } } ?: 0
                            }
                            val baseCellWidth = when {
                                maxCols >= 10 -> 110.dp
                                maxCols >= 8 -> 120.dp
                                else -> 140.dp
                            }
                            val ruDayNames = remember {
                                mapOf(
                                    "понедельник" to DayOfWeek.MONDAY, "пн" to DayOfWeek.MONDAY,
                                    "вторник" to DayOfWeek.TUESDAY, "вт" to DayOfWeek.TUESDAY,
                                    "среда" to DayOfWeek.WEDNESDAY, "ср" to DayOfWeek.WEDNESDAY,
                                    "четверг" to DayOfWeek.THURSDAY, "чт" to DayOfWeek.THURSDAY,
                                    "пятница" to DayOfWeek.FRIDAY, "пт" to DayOfWeek.FRIDAY,
                                    "суббота" to DayOfWeek.SATURDAY, "сб" to DayOfWeek.SATURDAY,
                                    "воскресенье" to DayOfWeek.SUNDAY, "вс" to DayOfWeek.SUNDAY
                                )
                            }
                            val today = remember { LocalDate.now() }
                            val isCurrentWeek = remember(weeklySchedule) {
                                isDateInWeek(weeklySchedule.weekTitle, today)
                            }
                            val todayRowIndex = remember(weeklySchedule, ruDayNames, today, isCurrentWeek) {
                                if (!isCurrentWeek) return@remember -1
                                val normalizedToday = today.dayOfWeek.getDisplayName(
                                    java.time.format.TextStyle.FULL, Locale("ru")
                                ).lowercase(Locale.ROOT)
                                weeklySchedule.rows.indexOfFirst { row ->
                                    row.cells.any { cell ->
                                        val text = cell.text.trim().lowercase(Locale.ROOT)
                                        ruDayNames.any { (name, day) ->
                                            day == today.dayOfWeek && text.startsWith(name)
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                val spannedColumns = mutableMapOf<Int, Int>()
                                weeklySchedule.rows.forEachIndexed { rowIndex, row ->
                                    Row(
                                        modifier = Modifier.wrapContentWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        var colIdx = 0
                                        for (cell in row.cells) {
                                            while (spannedColumns.containsKey(colIdx)) {
                                                Spacer(modifier = Modifier.width(baseCellWidth))
                                                colIdx++
                                            }
                                            val isTodayRow = rowIndex == todayRowIndex
                                            TableCell(
                                                cellContent = cell,
                                                isHeader = row.isHeader,
                                                isToday = isTodayRow,
                                                modifier = Modifier
                                                    .width(baseCellWidth * cell.colSpan)
                                                    .height(64.dp)
                                            )
                                            if (cell.rowSpan > 1) {
                                                for (c in colIdx until colIdx + cell.colSpan) {
                                                    spannedColumns[c] = cell.rowSpan - 1
                                                }
                                            }
                                            colIdx += cell.colSpan
                                        }
                                        while (spannedColumns.containsKey(colIdx)) {
                                            Spacer(modifier = Modifier.width(baseCellWidth))
                                            colIdx++
                                        }
                                    }
                                    for ((col, remaining) in spannedColumns) {
                                        spannedColumns[col] = remaining - 1
                                    }
                                    spannedColumns.entries.removeAll { it.value <= 0 }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(cellContent: ScheduleCellContent, isHeader: Boolean, isToday: Boolean = false, modifier: Modifier = Modifier) {
    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        isHeader -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val textStyle = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall

    Surface(
        modifier = modifier.border(1.dp, if (isToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant),
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cellContent.text.ifBlank { "-" },
                style = textStyle,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
                maxLines = if (isHeader) 2 else 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun isDateInWeek(weekTitle: String?, today: LocalDate): Boolean {
    if (weekTitle == null) return false
    val datePattern = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
    val dates = datePattern.findAll(weekTitle).map { matchResult ->
        val (day, month, year) = matchResult.destructured
        LocalDate.of(year.toInt(), month.toInt(), day.toInt())
    }.toList()
    if (dates.isEmpty()) return false
    val startDate = dates.first()
    val endDate = dates.last()
    return today >= startDate && today <= endDate
}