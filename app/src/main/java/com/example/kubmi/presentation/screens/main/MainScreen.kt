package com.example.kubmi.presentation.screens.main

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.News
import com.example.kubmi.presentation.navigation.Screen
import com.example.kubmi.presentation.components.BellScheduleCard
import com.example.kubmi.presentation.components.CurrentDateTime
import com.example.kubmi.ui.components.CoilImage
import com.example.kubmi.ui.theme.KubMiAccentRed
import com.example.kubmi.ui.theme.KubMiAccentRedDark
import com.example.kubmi.ui.theme.KubMiCtaBlue
import com.example.kubmi.ui.theme.KubMiCtaBlueDark
import kotlinx.coroutines.delay
import java.io.File
import org.json.JSONObject

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
                    CurrentDateTime()
                    IconButton(onClick = { viewModel.refreshNews() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Admin.route) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
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
        PullToRefreshBox(
            isRefreshing = refreshState is RefreshState.Loading,
            onRefresh = { viewModel.refreshNews() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            val bellText by viewModel.bellScheduleText.collectAsState()
            val scheduleSectionTitle by viewModel.mainScreenTitle.collectAsState()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScheduleCtaTile(
                        title = stringResource(R.string.student_schedule),
                        gradient = Brush.horizontalGradient(listOf(KubMiCtaBlue, KubMiCtaBlueDark)),
                        onClick = { navController.navigate(Screen.StudentSchedule.route) },
                        modifier = Modifier.weight(1f)
                    )
                    ScheduleCtaTile(
                        title = stringResource(R.string.teacher_schedule),
                        gradient = Brush.horizontalGradient(listOf(KubMiAccentRed, KubMiAccentRedDark)),
                        onClick = { navController.navigate(Screen.TeacherSchedule.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bell Schedule section
            if (bellText.isNotBlank()) {
                item {
                    PanelSectionTitle(text = scheduleSectionTitle.ifBlank { stringResource(R.string.bell_schedule_title) })
                }
                item {
                    BellScheduleCard(text = bellText)
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
            item {
                PanelSectionTitle(text = stringResource(R.string.contacts))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.contact_deanery),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.contact_deanery_phone),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.contact_education_department),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.contact_education_department_phone),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.contact_vice_dean),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.contact_vice_dean_phone),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.contact_accounting),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.contact_accounting_phone),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.panel_address_value),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.panel_address_value_2),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun NewsCarousel(news: List<News>, navController: NavController) {
    val carouselNews = remember(news) { news.take(6) }
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var activeIndex by remember { mutableStateOf(0) }
    val configuration = LocalConfiguration.current
    val cardHeightPx = configuration.screenHeightDp * 0.22f
    val cardHeight = cardHeightPx.dp.coerceIn(150.dp, 210.dp)
    val cardWidth = (cardHeight.value * 1.36f).dp
    val imageHeight = (cardHeight.value * 0.55f).dp

    // Синхронизация dot-индикатора с центральной карточкой
    LaunchedEffect(listState) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@snapshotFlow 0
            val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
            visible.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: 0
        }.collect { centerIdx -> activeIndex = centerIdx }
    }

    // Автопрокрутка карусели
    LaunchedEffect(listState, carouselNews.size) {
        while (carouselNews.size > 1) {
            delay(5000)
            if (!listState.isScrollInProgress) {
                val next = (activeIndex + 1) % carouselNews.size
                listState.animateScrollToItem(next)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(items = carouselNews, key = { _, item -> item.id }) { index, newsItem ->
                NewsCard(
                    news = newsItem,
                    navController = navController,
                    highlighted = activeIndex == index,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    imageHeight = imageHeight
                )
            }
        }

        // Индикаторы-точки
        if (carouselNews.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                carouselNews.forEachIndexed { index, _ ->
                    val isActive = activeIndex == index
                    val dotSize by animateFloatAsState(
                        targetValue = if (isActive) 10f else 6f,
                        animationSpec = tween(300),
                        label = "dotSize"
                    )
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.4f,
                        animationSpec = tween(300),
                        label = "dotAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(dotSize.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha))
                    )
                }
            }
        }
    }
}

@Composable
fun NewsCard(
    news: News,
    navController: NavController,
    highlighted: Boolean = false,
    cardWidth: androidx.compose.ui.unit.Dp = 300.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 220.dp,
    imageHeight: androidx.compose.ui.unit.Dp = 130.dp
) {
    val targetScale by animateFloatAsState(
        targetValue = if (highlighted) 1.05f else 0.97f,
        label = "newsCardScale",
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
    )
    val targetElevation by animateFloatAsState(
        targetValue = if (highlighted) 8f else 2f,
        label = "newsCardElevation",
        animationSpec = tween(durationMillis = 400)
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0.82f,
        label = "newsCardAlpha",
        animationSpec = tween(durationMillis = 400)
    )

    Card(
        onClick = { navController.navigate(Screen.NewsDetail.createRoute(news.id)) },
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer(
                scaleX = targetScale,
                scaleY = targetScale
            )
            .alpha(targetAlpha),
        elevation = CardDefaults.cardElevation(defaultElevation = targetElevation.dp),
        border = if (highlighted)
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else null
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            news.imageUrl?.let { imageUrl ->
                CoilImage(
                    imageUrl = imageUrl,
                    contentDescription = stringResource(R.string.news_image_desc, news.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
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
    var isFocused by remember { mutableStateOf(false) }

    val targetScale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing),
        label = "ctaScale"
    )
    val targetElevation by animateFloatAsState(
        targetValue = if (isFocused) 8f else 2f,
        animationSpec = tween(durationMillis = 250),
        label = "ctaElevation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer(
                scaleX = targetScale,
                scaleY = targetScale
            )
            .onFocusChanged { isFocused = it.isFocused },
        shape = MaterialTheme.shapes.medium,
        shadowElevation = targetElevation.dp,
        border = if (isFocused)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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