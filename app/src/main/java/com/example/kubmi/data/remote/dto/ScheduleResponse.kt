package com.example.kubmi.data.remote.dto

data class ScheduleResponse(
    val schedule: List<ScheduleDto>
)

data class ScheduleDto(
    val id: String,
    val dayOfWeek: String,
    val timeSlot: String,
    val subject: String,
    val type: String,
    val room: String,
    val teacher: String,
    val group: String?
)
