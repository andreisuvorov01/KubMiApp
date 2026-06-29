package com.example.kubmi.domain.repository

import com.example.kubmi.domain.model.ScheduleIndexEntry
import com.example.kubmi.domain.model.StudentGroupsTable
import com.example.kubmi.domain.model.WeeklyScheduleData

interface ScheduleRepository {
    suspend fun refreshStudentScheduleByUrl(groupTitle: String, url: String, forceNetwork: Boolean = false): List<WeeklyScheduleData>
    suspend fun refreshTeacherScheduleByUrl(teacherTitle: String, url: String, forceNetwork: Boolean = false): List<WeeklyScheduleData>
    suspend fun getStudentGroupsTable(forceNetwork: Boolean = false): StudentGroupsTable
    suspend fun getAllGroups(forceNetwork: Boolean = false): List<ScheduleIndexEntry>
    suspend fun getAllTeachers(forceNetwork: Boolean = false): List<ScheduleIndexEntry>
}