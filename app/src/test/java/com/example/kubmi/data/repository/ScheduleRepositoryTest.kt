package com.example.kubmi.data.repository

import com.example.kubmi.data.local.dao.ScheduleDao
import com.example.kubmi.data.local.entity.ScheduleEntity
import com.example.kubmi.data.remote.WebScraper
import com.example.kubmi.domain.model.LessonType
import com.example.kubmi.domain.model.ScheduleItem
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleRepositoryTest {

    @MockK
    private lateinit var scheduleDao: ScheduleDao

    @MockK
    private lateinit var webScraper: WebScraper

    private lateinit var scheduleRepository: ScheduleRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        scheduleRepository = ScheduleRepository(scheduleDao, webScraper)
    }

    @Test
    fun `getScheduleByGroup returns mapped domain objects`() = runTest {
        // Given
        val entity = ScheduleEntity(
            id = "1",
            dayOfWeek = "Понедельник",
            timeSlot = "09:00-10:30",
            subject = "Математика",
            type = "LECTURE",
            room = "Ауд. 101",
            teacher = "Иванов И.И.",
            group = "ПМ-101"
        )
        
        every { scheduleDao.getScheduleByGroup("ПМ-101") } returns flowOf(listOf(entity))

        // When
        val result = scheduleRepository.getScheduleByGroup("ПМ-101")

        // Then
        result.collect { scheduleItems ->
            assertEquals(1, scheduleItems.size)
            with(scheduleItems[0]) {
                assertEquals("1", id)
                assertEquals("Понедельник", dayOfWeek)
                assertEquals("09:00-10:30", timeSlot)
                assertEquals("Математика", subject)
                assertEquals(LessonType.LECTURE, type)
                assertEquals("Ауд. 101", room)
                assertEquals("Иванов И.И.", teacher)
                assertEquals("ПМ-101", group)
            }
        }
    }

    @Test
    fun `getScheduleByTeacher returns mapped domain objects`() = runTest {
        // Given
        val entity = ScheduleEntity(
            id = "1",
            dayOfWeek = "Вторник",
            timeSlot = "10:45-12:15",
            subject = "Физика",
            type = "PRACTICE",
            room = "Ауд. 102",
            teacher = "Петров П.П.",
            group = "ПМ-101"
        )
        
        every { scheduleDao.getScheduleByTeacher("Петров П.П.") } returns flowOf(listOf(entity))

        // When
        val result = scheduleRepository.getScheduleByTeacher("Петров П.П.")

        // Then
        result.collect { scheduleItems ->
            assertEquals(1, scheduleItems.size)
            with(scheduleItems[0]) {
                assertEquals("1", id)
                assertEquals("Вторник", dayOfWeek)
                assertEquals("10:45-12:15", timeSlot)
                assertEquals("Физика", subject)
                assertEquals(LessonType.PRACTICE, type)
                assertEquals("Ауд. 102", room)
                assertEquals("Петров П.П.", teacher)
                assertEquals("ПМ-101", group)
            }
        }
    }

    @Test
    fun `refreshStudentSchedule scrapes data and updates database`() = runTest {
        // Given
        val scrapedSchedule = listOf(
            ScheduleItem(
                id = "1",
                dayOfWeek = "Среда",
                timeSlot = "09:00-10:30",
                subject = "Информатика",
                type = LessonType.LAB,
                room = "Ауд. 201",
                teacher = "Сидоров С.С.",
                group = "ПМ-101"
            )
        )
        
        coEvery { webScraper.scrapeStudentSchedule("ПМ-101") } returns scrapedSchedule
        coEvery { scheduleDao.deleteScheduleByGroup("ПМ-101") } returns Unit
        coEvery { scheduleDao.insertAll(any()) } returns Unit

        // When
        val result = scheduleRepository.refreshStudentSchedule("ПМ-101")

        // Then
        assertEquals(scrapedSchedule, result)
        assertEquals(1, result.size)
        assertEquals("Информатика", result[0].subject)
        
        // Verify interactions
        coVerify { webScraper.scrapeStudentSchedule("ПМ-101") }
        coVerify { scheduleDao.deleteScheduleByGroup("ПМ-101") }
        coVerify { scheduleDao.insertAll(any()) }
    }

    @Test
    fun `refreshTeacherSchedule scrapes data and updates database`() = runTest {
        // Given
        val scrapedSchedule = listOf(
            ScheduleItem(
                id = "1",
                dayOfWeek = "Четверг",
                timeSlot = "10:45-12:15",
                subject = "Английский язык",
                type = LessonType.PRACTICE,
                room = "Ауд. 103",
                teacher = "Смирнова А.А.",
                group = "ПМ-101"
            )
        )
        
        coEvery { webScraper.scrapeTeacherSchedule("Смирнова А.А.") } returns scrapedSchedule
        coEvery { scheduleDao.deleteScheduleByTeacher("Смирнова А.А.") } returns Unit
        coEvery { scheduleDao.insertAll(any()) } returns Unit

        // When
        val result = scheduleRepository.refreshTeacherSchedule("Смирнова А.А.")

        // Then
        assertEquals(scrapedSchedule, result)
        assertEquals(1, result.size)
        assertEquals("Английский язык", result[0].subject)
        
        // Verify interactions
        coVerify { webScraper.scrapeTeacherSchedule("Смирнова А.А.") }
        coVerify { scheduleDao.deleteScheduleByTeacher("Смирнова А.А.") }
        coVerify { scheduleDao.insertAll(any()) }
    }

    @Test
    fun `refreshTeacherSchedule with empty teacher deletes all schedule`() = runTest {
        // Given
        val scrapedSchedule = listOf(
            ScheduleItem(
                id = "1",
                dayOfWeek = "Понедельник",
                timeSlot = "09:00-10:30",
                subject = "Математика",
                type = LessonType.LECTURE,
                room = "Ауд. 101",
                teacher = "Иванов И.И.",
                group = "ПМ-101"
            )
        )
        coEvery { webScraper.scrapeTeacherSchedule("") } returns scrapedSchedule
        coEvery { scheduleDao.deleteAll() } returns Unit
        coEvery { scheduleDao.insertAll(any()) } returns Unit

        // When
        val result = scheduleRepository.refreshTeacherSchedule("")

        // Then
        assertTrue(result.isNotEmpty())
        assertEquals(1, result.size)
        
        // Verify interactions
        coVerify { webScraper.scrapeTeacherSchedule("") }
        coVerify { scheduleDao.deleteAll() }
        coVerify { scheduleDao.insertAll(any()) }
    }
    }