# Android Application for Krasnodar Medical Institute (KubMI)

This Android application provides students and faculty at Krasnodar Medical Institute with access to schedules, news, and other important information.

## Features

- Student schedule viewing with group selection
- Teacher schedule viewing with teacher selection
- News feed from the institute's website
- About information for the institute

## Architecture

The application follows modern Android development practices:

- **MVVM (Model-View-ViewModel)** pattern for clean architecture
- **Jetpack Compose** for modern UI development
- **Room Database** for local data storage
- **Repository Pattern** for data abstraction
- **Dependency Injection** with Hilt
- **Coroutines** for asynchronous operations

## Web Scraping Implementation

The application uses web scraping to retrieve schedule data from the official Krasnodar Medical Institute website. The implementation has been recently updated to improve reliability and performance:

### Updated Scraping Logic (as of December 2025)

1. **Student Schedule**:
   - First retrieves the list of all student groups from the main schedule page
   - For each group (or specific requested group), navigates to the individual group schedule page
   - Extracts schedule items (day, time, subject, room, teacher) from the individual pages
   - Handles potential server load by adding delays between requests

2. **Teacher Schedule**:
   - First retrieves the list of all teachers from the main teacher schedule page
   - For each teacher (or specific requested teacher), navigates to the individual teacher schedule page
   - Extracts schedule items (day, time, subject, room, group) from the individual pages
   - Handles potential server load by adding delays between requests

### Key Improvements

- **Better Error Handling**: Improved fallback mechanisms when individual pages fail to load
- **Performance Optimization**: Added delays to be respectful to the server
- **Improved Parsing Logic**: More robust parsing that can handle variations in HTML structure
- **Flexible Queries**: Ability to fetch schedules for specific groups/teachers or all at once

## Dependency Injection Architecture

The application uses Hilt for dependency injection. The architecture has been updated to resolve the `error.NonExistentClass` issue:

- Removed conflicting `RepositoryModule.kt` that was manually creating repository instances
- Ensured all repositories (`NewsRepositoryImpl`, `ScheduleRepositoryImpl`, `AboutRepositoryImpl`) are properly annotated with `@Singleton` and `@Inject`
- `WebScraper` is now properly configured as a singleton with constructor injection
- Dependencies are resolved automatically through Hilt's assisted injection

## Data Models

### ScheduleItem
- `id`: Unique identifier for the schedule item
- `dayOfWeek`: Day of the week (e.g., "Понедельник")
- `timeSlot`: Time period (e.g., "09:00-10:30")
- `subject`: Subject name
- `type`: Lesson type (LECTURE, PRACTICE, LAB)
- `room`: Classroom number
- `teacher`: Teacher's name
- `group`: Student group name

## Technologies Used

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit
- **Hilt**: Dependency injection
- **Room**: Local database
- **Coroutines**: Asynchronous programming
- **JSoup**: HTML parsing for web scraping
- **WorkManager**: Background tasks
- **Retrofit/OkHttp**: HTTP client (if needed in future)

## Installation

1. Clone the repository
2. Open in Android Studio
3. Build and run on your device or emulator

## Testing

The application includes unit tests for the scraping functionality and UI tests for the main screens.

## Contributing

We welcome contributions to improve the application. Please fork the repository and submit a pull request with your changes.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
