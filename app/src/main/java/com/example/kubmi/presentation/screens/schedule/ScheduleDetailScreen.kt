package com.example.kubmi.presentation.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.WeeklyScheduleData
import com.example.kubmi.domain.model.ScheduleTableRow
import com.example.kubmi.domain.model.ScheduleCellContent

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
                    IconButton(onClick = { navController.popBackStack() }) {
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
                    weeklyScheduleData.forEachIndexed { weeklyScheduleIndex, weeklySchedule ->
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                weeklySchedule.rows.forEachIndexed { rowIndex, row ->
                                    Row(
                                        // Important: inside a horizontalScroll container, width constraints can be unbounded.
                                        // Using `weight()` here can lead to zero-width cells / blank content.
                                        // Fixed column widths keeps alignment stable and matches the website-style tables.
                                        modifier = Modifier.wrapContentWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp) // Maintain spacing between cells
                                    ) {
                                        row.cells.forEach { cell ->
                                            TableCell(
                                                cellContent = cell,
                                                isHeader = row.isHeader,
                                                modifier = Modifier
                                                    .width(baseCellWidth * cell.colSpan)
                                                    .heightIn(min = 40.dp)
                                            )
                                        }
                                    }
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
fun RowScope.TableCell(cellContent: ScheduleCellContent, isHeader: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = if (isHeader) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val textStyle = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall

    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant), // Apply border to the passed modifier
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
                text = cellContent.text.ifBlank { "-" }, // Display "-" for empty fields
                style = textStyle,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center
            )
        }
    }
}