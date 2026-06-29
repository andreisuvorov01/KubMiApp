package com.example.kubmi.domain.model

data class AboutContent(
    val history: String,
    val management: List<ManagementPerson>,
    val faculties: List<AboutFaculty>,
    val contacts: ContactInfo,
    val achievements: List<Achievement>
)

data class ManagementPerson(
    val name: String,
    val position: String,
    val bio: String
)

data class AboutFaculty(
    val name: String,
    val description: String,
    val departments: List<String>
)

data class ContactInfo(
    val address: String,
    val phone: String,
    val email: String,
    val website: String
)

data class Achievement(
    val title: String,
    val description: String,
    val year: Int
)