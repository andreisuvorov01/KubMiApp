package com.example.kubmi.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.News
import com.example.kubmi.presentation.navigation.Screen
import com.example.kubmi.ui.components.CoilImage
import com.example.kubmi.ui.theme.KubMiAccentRed
import com.example.kubmi.ui.theme.KubMiAccentRedDark
import com.example.kubmi.ui.theme.KubMiCtaBlue
import com.example.kubmi.ui.theme.KubMiCtaBlueDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val viewModel: MainViewModel = hiltViewModel()
    val news by viewModel.news.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()

    // Trigger initial load so the main screen isn't stuck showing "loading" forever on a fresh install.
    LaunchedEffect(Unit) {
        viewModel.refreshNews()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Admin.route) }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                            contentDescription = stringResource(R.string.admin_panel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                PanelSectionTitle(text = stringResource(R.string.news))
            }

            item {
            if (news.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(news.take(5)) { newsItem ->
                        NewsCard(newsItem, navController)
                    }
                }
                } else {
                    when (val state = refreshState) {
                        is RefreshState.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = stringResource(R.string.loading_news),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is RefreshState.Error -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = { viewModel.refreshNews() }) {
                                    Text(text = stringResource(R.string.refresh))
                                }
                            }
                        }
                        else -> {
                            // Idle/Success but still empty -> show a neutral message + retry.
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.loading_news),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = { viewModel.refreshNews() }) {
                                    Text(text = stringResource(R.string.refresh))
                                }
                            }
                        }
                    }
                }
            }

            item {
                PanelSectionTitle(text = stringResource(R.string.select_schedule))
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScheduleCtaTile(
                        title = stringResource(R.string.student_schedule),
                        gradient = Brush.horizontalGradient(listOf(KubMiCtaBlue, KubMiCtaBlueDark)),
                        onClick = { navController.navigate(Screen.StudentSchedule.route) }
                    )
                    ScheduleCtaTile(
                        title = stringResource(R.string.teacher_schedule),
                        gradient = Brush.horizontalGradient(listOf(KubMiAccentRed, KubMiAccentRedDark)),
                        onClick = { navController.navigate(Screen.TeacherSchedule.route) }
                    )
                }
            }

            item {
                PanelSectionTitle(text = stringResource(R.string.about_university))
            }

            item {
                ScheduleCtaTile(
                    title = stringResource(R.string.about_university),
                    gradient = Brush.horizontalGradient(listOf(KubMiCtaBlueDark, KubMiAccentRedDark)),
                    onClick = { navController.navigate(Screen.About.route) }
                )
            }
        }
    }
}

@Composable
fun NewsCard(news: News, navController: NavController) {
    Card(
        onClick = { navController.navigate(Screen.NewsDetail.createRoute(news.id)) },
        modifier = Modifier
            .width(280.dp)
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Display news image if available
            news.imageUrl?.let { imageUrl ->
                CoilImage(
                    imageUrl = imageUrl,
                    contentDescription = stringResource(R.string.news_image_desc, news.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PanelSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun ScheduleCtaTile(
    title: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}