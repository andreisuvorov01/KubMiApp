package com.example.kubmi.data.remote

import com.example.kubmi.data.remote.dto.NewsResponse
import com.example.kubmi.data.remote.dto.ScheduleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KubMiApi {
    @GET("news")
    suspend fun getNews(): NewsResponse

    @GET("schedule/students")
    suspend fun getStudentSchedule(
        @Query("faculty") faculty: String,
        @Query("course") course: Int,
        @Query("group") group: String
    ): ScheduleResponse

    @GET("schedule/teachers")
    suspend fun getTeacherSchedule(
        @Query("department") department: String,
        @Query("teacher") teacher: String
    ): ScheduleResponse
}
