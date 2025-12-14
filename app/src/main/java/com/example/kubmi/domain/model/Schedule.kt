package com.example.kubmi.domain.model

data class ScheduleFaculty(val id: String, val name: String)
data class Department(val id: String, val name: String)
data class Group(val id: String, val name: String, val course: Int)
data class Teacher(val id: String, val name: String, val department: String)
