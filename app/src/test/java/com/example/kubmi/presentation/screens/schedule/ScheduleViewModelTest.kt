package com.example.kubmi.presentation.screens.schedule

import app.cash.turbine.test
import com.example.kubmi.data.repository.ScheduleRepository
import com.example.kubmi.domain.model.LessonType
import com.example.kubmi.domain.model.ScheduleItem
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleViewModelTest {

    @MockK
    private lateinit var scheduleRepository: ScheduleRepository

    private lateinit var viewModel: ScheduleViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = ScheduleViewModel(scheduleRepository)
    }

    @Test
    fun `loadStudentSchedule updates state correctly when data is available`() = runTest {
        // Given
        val testSchedule = listOf(
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
        
        every { scheduleRepository.getScheduleByGroup("ПМ-101") } returns flowOf(testSchedule)
        coEvery { scheduleRepository.refreshStudentSchedule("ПМ-101") } returns testSchedule

        // When
        viewModel.loadStudentSchedule("ПМ-101")

        // Then
        viewModel.scheduleState.test {
            val emittedSchedule = awaitItem()
            assertEquals(testSchedule, emittedSchedule)
            assertEquals(1, emittedSchedule.size)
            assertEquals("Математика", emittedSchedule[0].subject)
        }

        viewModel.isLoading.test {
            val loadingState = awaitItem()
            assertFalse(loadingState)
        }

        // Verify repository methods were called
        verify { scheduleRepository.getScheduleByGroup("ПМ-101") }
        coVerify { scheduleRepository.refreshStudentSchedule("ПМ-101") }
    }

    @Test
    fun `loadStudentSchedule handles empty schedule correctly`() = runTest {
        // Given
        every { scheduleRepository.getScheduleByGroup("") } returns flowOf(emptyList())
        coEvery { scheduleRepository.refreshStudentSchedule("") } returns emptyList()

        // When
        viewModel.loadStudentSchedule()

        // Then
        viewModel.scheduleState.test {
            val emittedSchedule = awaitItem()
            assertTrue(emittedSchedule.isEmpty())
        }

        viewModel.isLoading.test {
            val loadingState = awaitItem()
            assertFalse(loadingState)
        }
    }

    @Test
    fun `loadStudentSchedule handles repository errors gracefully`() = runTest {
        // Given
        every { scheduleRepository.getScheduleByGroup("error") } throws RuntimeException("Database error")
        coEvery { scheduleRepository.refreshStudentSchedule("error") } returns emptyList()

        // When
        viewModel.loadStudentSchedule("error")

        // Then
        viewModel.error.test {
            val errorState = awaitItem()
            assertTrue(errorState?.contains("error") ?: false)
        }

        viewModel.isLoading.test {
            val loadingState = awaitItem()
            assertFalse(loadingState)
        }
    }

    @Test
    fun `loadTeacherSchedule updates state correctly when data is available`() = runTest {
        // Given
        val testSchedule = listOf(
            ScheduleItem(
                id = "1",
                dayOfWeek = "Вторник",
                timeSlot = "10:45-12:15",
                subject = "Физика",
                type = LessonType.PRACTICE,
                room = "Ауд. 102",
                teacher = "Петров П.П.",
                group = "ПМ-101"
            )
        )
        
        every { scheduleRepository.getScheduleByTeacher("Петров П.П.") } returns flowOf(testSchedule)
        coEvery { scheduleRepository.refreshTeacherSchedule("Петров П.П.") } returns testSchedule

        // When
        viewModel.loadTeacherSchedule("Петров П.П.")

        // Then
        viewModel.scheduleState.test {
            val emittedSchedule = awaitItem()
            assertEquals(testSchedule, emittedSchedule)
            assertEquals(1, emittedSchedule.size)
            assertEquals("Физика", emittedSchedule[0].subject)
        }

        viewModel.isLoading.test {
            val loadingState = awaitItem()
            assertFalse(loadingState)
        }

        // Verify repository methods were called
        verify { scheduleRepository.getScheduleByTeacher("Петров П.П.") }
        coVerify { scheduleRepository.refreshTeacherSchedule("Петров П.П.") }
    }

    @Test
    fun `loadTeacherSchedule handles empty schedule correctly`() = runTest {
        // Given
        every { scheduleRepository.getScheduleByTeacher("") } returns flowOf(emptyList())
        coEvery { scheduleRepository.refreshTeacherSchedule("") } returns emptyList()

        // When
        viewModel.loadTeacherSchedule()

        // Then
        viewModel.scheduleState.test {
            val emittedSchedule = awaitItem()
            assertTrue(emittedSchedule.isEmpty())
        }

        viewModel.isLoading.test {
            val loadingState = awaitItem()
            assertFalse(loadingState)
        }
    }

    @Test
    fun `loadTeacherSchedule handles repository errors gracefully`() = runTest {
        // Given
        every { scheduleRepository.getScheduleByTeacher("error") } throws RuntimeException("Database error")
        coEvery { scheduleRepository.refreshTeacherSchedule("error") } returns emptyList()

        // When
        viewModel.loadTeacherSchedule("error")

        // Then
        viewModel.error.test {
            val errorState = awaitItem()
            assertTrue(errorState?.contains("error") ?: false)
        }

        viewModel.isLoading.test {
            val loadingState = awaitItem()
            assertFalse(loadingState)
        }
    }
}