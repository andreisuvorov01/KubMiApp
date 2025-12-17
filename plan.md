# Plan

## Overview
This plan addresses two main requests: modifying the main screen to remove the "Help" section and add an "About the University" button that displays parsed content from a specific URL, and adjusting schedule table cell sizes for better display.

## 1. Modify Main Screen and Add "About the University" Section

*   **Remove "Help" Section:**
    *   Identify and remove the UI component corresponding to the "Help" section in [`app/src/main/java/com/example/kubmi/presentation/screens/main/MainScreen.kt`](app/src/main/java/com/example/kubmi/presentation/screens/main/MainScreen.kt).
    *   Update any associated navigation or business logic in [`app/src/main/java/com/example/kubmi/presentation/navigation/NavGraph.kt`](app/src/main/java/com/example/kubmi/presentation/navigation/NavGraph.kt) and [`app/src/main/java/com/example/kubmi/presentation/navigation/Screen.kt`](app/src/main/java/com/example/kubmi/presentation/navigation/Screen.kt) if necessary.

*   **Add "About the University" Button:**
    *   Add a new button for "About the University" to [`app/src/main/java/com/example/kubmi/presentation/screens/main/MainScreen.kt`](app/src/main/java/com/example/kubmi/presentation/screens/main/MainScreen.kt).
    *   Define a new navigation route in [`app/src/main/java/com/example/kubmi/presentation/navigation/Screen.kt`](app/src/main/java/com/example/kubmi/presentation/navigation/Screen.kt) for the "About the University" screen.
    *   Integrate this new route into [`app/src/main/java/com/example/kubmi/presentation/navigation/NavGraph.kt`](app/src/main/java/com/example/kubmi/presentation/navigation/NavGraph.kt).

*   **Implement Web Scraping for "About the University" Content:**
    *   Create a new function in [`app/src/main/java/com/example/kubmi/data/remote/WebScraper.kt`](app/src/main/java/com/example/kubmi/data/remote/WebScraper.kt) to parse the content from `https://kubmi.ru/institut/istoriya-instituta/`. This function will extract relevant text and structure it for display.
    *   Define a new data model ([`app/src/main/java/com/example/kubmi/domain/model/AboutContent.kt`](app/src/main/java/com/example/kubmi/domain/model/AboutContent.kt)) to represent the parsed "About the University" information.
    *   Implement data access objects ([`app/src/main/java/com/example/kubmi/data/local/dao/AboutDao.kt`](app/src/main/java/com/example/kubmi/data/local/dao/AboutDao.kt)) and an entity ([`app/src/main/java/com/example/kubmi/data/local/entity/AboutEntity.kt`](app/src/main/java/com/example/kubmi/data/local/entity/AboutEntity.kt)) for local caching of this content.
    *   Create a repository interface ([`app/src/main/java/com/example/kubmi/domain/repository/AboutRepository.kt`](app/src/main/java/com/example/kubmi/domain/repository/AboutRepository.kt)) and its implementation ([`app/src/main/java/com/example/kubmi/data/repository/AboutRepositoryImpl.kt`](app/src/main/java/com/example/kubmi/data/repository/AboutRepositoryImpl.kt)) to manage the fetching, parsing, and caching of "About the University" data.
    *   Update [`app/src/main/java/com/example/kubmi/di/RepositoryModule.kt`](app/src/main/java/com/example/kubmi/di/RepositoryModule.kt) and potentially other DI modules to provide the new repository.

*   **Create "About the University" Screen:**
    *   Develop a new Composable screen ([`app/src/main/java/com/example/kubmi/presentation/screens/about/AboutScreen.kt`](app/src/main/java/com/example/kubmi/presentation/screens/about/AboutScreen.kt)) to display the parsed information.
    *   Create an associated ViewModel ([`app/src/main/java/com/example/kubmi/presentation/screens/about/AboutViewModel.kt`](app/src/main/java/com/example/kubmi/presentation/screens/about/AboutViewModel.kt)) to handle the logic for fetching and presenting the "About the University" content.

## 2. Adjust Table Cell Sizes in Schedule Detail Windows

*   **Modify Schedule Detail Screen:**
    *   Examine [`app/src/main/java/com/example/kubmi/presentation/screens/schedule/ScheduleDetailScreen.kt`](app/src/main/java/com/example/kubmi/presentation/screens/schedule/ScheduleDetailScreen.kt) to locate the table rendering logic.
    *   Adjust the `Modifier` properties of the table cells (e.g., `weight`, `width`, `fillMaxHeight`, `fillMaxWidth`) within the Composable functions to ensure uniform cell sizes and prevent text overflow. This may involve using `Column` and `Row` with appropriate `weight` modifiers or fixed `width` and `height` modifiers.

## Todos

*   **main-screen-update**: Remove "Help" section and add "About the University" button in `MainScreen.kt`.
*   **navigation-setup**: Define new navigation route for "About the University" and integrate into `NavGraph.kt`.
*   **web-scraping-about**: Implement web scraping logic for `https://kubmi.ru/institut/istoriya-instituta/` in `WebScraper.kt`.
*   **data-model-about**: Create data model, entity, DAO, and repository for "About the University" content.
*   **about-screen-impl**: Create `AboutScreen.kt` and `AboutViewModel.kt` to display the parsed information.
*   **schedule-table-fix**: Adjust table cell sizes in `ScheduleDetailScreen.kt` for consistent layout.
