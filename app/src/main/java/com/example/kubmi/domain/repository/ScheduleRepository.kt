package com.example.kubmi.domain.repository

import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.StudentGroupsTable
import com.example.kubmi.domain.model.WeeklyScheduleData

interface ScheduleRepository {
    suspend fun refreshStudentScheduleByUrl(groupTitle: String, url: String): List<WeeklyScheduleData>
    suspend fun refreshTeacherScheduleByUrl(teacherTitle: String, url: String): List<WeeklyScheduleData>
    suspend fun getStudentGroupsTable(): StudentGroupsTable
    suspend fun getAllGroups(): List<ScheduleIndexEntry>
    suspend fun getAllTeachers(): List<ScheduleIndexEntry>
}