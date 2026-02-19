package com.example.kubmi.presentation.screens.main

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import java.io.File
import org.json.JSONObject
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy

// #region agent log helper
private fun agentLog(
    hypothesisId: String,
    location: String,
    message: String,
    data: Map<String, Any?> = emptyMap(),
    runId: String = "debug1"
) {
    try {
        val payload = mapOf(
            "sessionId" to "debug-session",
            "runId" to runId,
            "hypothesisId" to hypothesisId,
            "location" to location,
            "message" to message,
            "data" to data,
            "timestamp" to System.currentTimeMillis()
        )
        File("d:\\AndroidProject\\.cursor\\debug.log").appendText(
            JSONObject(payload).toString() + "\n"
        )
    } catch (_: Exception) {
        // logging must not crash UI
    }
}
// #endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val viewModel: MainViewModel = hiltViewModel()
    val news by viewModel.news.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()

    agentLog(
        hypothesisId = "H1",
        location = "MainScreen",
        message = "Compose start",
        data = mapOf("newsCount" to news.size)
    )

    // Trigger initial load so the main screen isn't stuck showing "loading" forever on a fresh install.
    LaunchedEffect(Unit) {
        agentLog(
            hypothesisId = "H1",
            location = "MainScreen:LaunchedEffect",
            message = "Initial refreshNews",
            data = mapOf("refreshState" to refreshState::class.java.simpleName)
        )
        viewModel.refreshNews()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures {
                                agentLog(
                                    hypothesisId = "H2",
                                    location = "MainScreen:TopAppBarTitle",
                                    message = "Title tapped",
                                    data = mapOf(
                                        "newsCount" to news.size,
                                        "refreshState" to refreshState::class.java.simpleName
                                    )
                                )
                            }
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_ksmu),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.institute_name),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshNews() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
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
                    NewsCarousel(news = news, navController = navController)
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
private fun NewsCarousel(news: List<News>, navController: NavController) {
    val carouselNews = remember(news) { news.take(4) }
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var isUserScrolling by remember { mutableStateOf(false) }
    var pulseIndex by remember { mutableStateOf(0) }
    
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val cardWidthPx = with(density) { 280.dp.toPx() }
    // Offset to center the card: (Screen - Card) / 2
    val centerOffsetPx = (screenWidthPx - cardWidthPx) / 2

    val pulseSequence = remember(carouselNews) {
        val count = carouselNews.size
        if (count <= 1) emptyList()
        else {
            val forward = (0 until count).toList()
            val backward = (count - 2 downTo 1).toList()
            forward + backward // 0, 1, 2, 3, 2, 1
        }
    }

    // Switch active item cycle
    LaunchedEffect(pulseSequence) {
        if (pulseSequence.isEmpty()) return@LaunchedEffect
        var position = 0
        while (isActive) {
            delay(4000)
            if (!isUserScrolling) {
                position = (position + 1) % pulseSequence.size
                pulseIndex = pulseSequence[position]
            }
        }
    }

    // Smooth scroll to the active item
    LaunchedEffect(pulseIndex) {
        if (!isUserScrolling && carouselNews.isNotEmpty()) {
            // Keep first item at the edge, center others
            val targetOffset = if (pulseIndex == 0) 0 else -(centerOffsetPx.toInt())
            listState.animateScrollToItem(pulseIndex, targetOffset)
        }
    }

    // Track user interaction
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isUserScrolling = it }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        flingBehavior = flingBehavior,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(items = carouselNews, key = { _, item -> item.id }) { index, newsItem ->
            val isFocused = pulseIndex == index
            NewsCard(news = newsItem, navController = navController, highlighted = isFocused)
        }
    }
}

@Composable
fun NewsCard(news: News, navController: NavController, highlighted: Boolean = false) {
    val targetScale by animateFloatAsState(
        targetValue = if (highlighted) 1.08f else 1f,
        label = "newsCardScale",
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0.85f,
        label = "newsCardAlpha",
        animationSpec = tween(durationMillis = 600)
    )

    Card(
        onClick = { navController.navigate(Screen.NewsDetail.createRoute(news.id)) },
        modifier = Modifier
            .width(280.dp)
            .height(200.dp)
            .graphicsLayer(
                scaleX = targetScale,
                scaleY = targetScale
            )
            .alpha(targetAlpha),
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