package com.example.kubmi.domain.model

data class ScheduleCellContent(
    val text: String,
    val rowSpan: Int = 1,
    val colSpan: Int = 1
)

data class ScheduleTableRow(
    val isHeader: Boolean,
    val cells: List<ScheduleCellContent>
)

data class WeeklyScheduleData(
    val weekTitle: String?,
    val rows: List<ScheduleTableRow>
)
