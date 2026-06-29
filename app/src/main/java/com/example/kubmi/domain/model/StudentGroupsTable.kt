package com.example.kubmi.domain.model

/**
 * Student groups exactly as on the panel page: header columns (e.g. 1 курс..6 курс)
 * and a matrix of cells. Null cell means an empty slot.
 *
 * Important: order and empties are preserved.
 */
data class StudentGroupsTable(
    val headers: List<String>,
    val rows: List<List<ScheduleIndexEntry?>>
)


