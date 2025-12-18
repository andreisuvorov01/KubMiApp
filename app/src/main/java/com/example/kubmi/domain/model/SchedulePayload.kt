package com.example.kubmi.domain.model

enum class ScheduleDataSource {
    CACHE,
    NETWORK
}

data class SchedulePayload(
    val data: List<WeeklyScheduleData>,
    val source: ScheduleDataSource,
    val lastUpdated: Long?
)
