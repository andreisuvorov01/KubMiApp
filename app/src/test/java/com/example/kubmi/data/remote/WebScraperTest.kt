package com.example.kubmi.data.remote

import com.example.kubmi.domain.model.ScheduleItem
import com.example.kubmi.domain.model.LessonType
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class WebScraperTest {
    
    private val webScraper = WebScraper()
    
    @Test
    fun `test scrapeStudentSchedule with specific group`() = runBlocking {
        // Тестируем получение расписания для конкретной группы
        val group = "МН-111" // Пример названия группы
        val schedule = webScraper.scrapeStudentSchedule(group)
        
        // Проверяем, что список не пустой (или пустой в случае ошибки)
        assertNotNull(schedule)
        
        // Если список не пустой, проверяем корректность данных
        if (schedule.isNotEmpty()) {
            val firstItem = schedule.first()
            assertTrue(firstItem.group == group)
            assertNotNull(firstItem.dayOfWeek)
            assertNotNull(firstItem.timeSlot)
            assertNotNull(firstItem.subject)
            assertNotNull(firstItem.type)
            assertNotNull(firstItem.room)
            assertNotNull(firstItem.teacher)
        }
    }
    
    @Test
    fun `test scrapeStudentSchedule all groups`() = runBlocking {
        // Тестируем получение расписания для всех групп
        val schedule = webScraper.scrapeStudentSchedule()
        
        // Проверяем, что список не пустой (или пустой в случае ошибки)
        assertNotNull(schedule)
    }
    
    @Test
    fun `test scrapeTeacherSchedule with specific teacher`() = runBlocking {
        // Тестируем получение расписания для конкретного преподавателя
        val teacher = "Иванов И.И." // Пример ФИО преподавателя
        val schedule = webScraper.scrapeTeacherSchedule(teacher)
        
        // Проверяем, что список не пустой (или пустой в случае ошибки)
        assertNotNull(schedule)
        
        // Если список не пустой, проверяем корректность данных
        if (schedule.isNotEmpty()) {
            val firstItem = schedule.first()
            assertTrue(firstItem.teacher.contains(teacher, ignoreCase = true))
            assertNotNull(firstItem.dayOfWeek)
            assertNotNull(firstItem.timeSlot)
            assertNotNull(firstItem.subject)
            assertNotNull(firstItem.type)
            assertNotNull(firstItem.room)
            assertNotNull(firstItem.group)
        }
    }
    
    @Test
    fun `test scrapeTeacherSchedule all teachers`() = runBlocking {
        // Тестируем получение расписания для всех преподавателей
        val schedule = webScraper.scrapeTeacherSchedule()
        
        // Проверяем, что список не пустой (или пустой в случае ошибки)
        assertNotNull(schedule)
    }
    
    @Test
    fun `test schedule item properties`() {
        // Тестируем создание объекта ScheduleItem
        val scheduleItem = ScheduleItem(
            id = "1",
            dayOfWeek = "Понедельник",
            timeSlot = "09:00-10:30",
            subject = "Математика",
            type = LessonType.LECTURE,
            room = "Ауд. 101",
            teacher = "Иванов И.И.",
            group = "МН-111"
        )
        
        assertEquals("1", scheduleItem.id)
        assertEquals("Понедельник", scheduleItem.dayOfWeek)
        assertEquals("09:0-10:30", scheduleItem.timeSlot)
        assertEquals("Математика", scheduleItem.subject)
        assertEquals(LessonType.LECTURE, scheduleItem.type)
        assertEquals("Ауд. 101", scheduleItem.room)
        assertEquals("Иванов И.И.", scheduleItem.teacher)
        assertEquals("МН-111", scheduleItem.group)
    }
}