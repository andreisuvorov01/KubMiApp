package com.example.kubmi.presentation.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.AboutPageContent
import com.example.kubmi.ui.components.CoilImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val aboutContent by viewModel.aboutContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // Ensure data is loaded once
    LaunchedEffect(Unit) { viewModel.refreshContent() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_university)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshContent() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stringResource(R.string.error)}: $error",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        aboutContent?.let { content ->
            AboutContentList(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                content = content,
                onCall = { number ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    context.startActivity(intent)
                },
                onEmail = { email ->
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                    }
                    context.startActivity(intent)
                },
                onOpenUrl = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun AboutContentList(
    modifier: Modifier,
    content: AboutPageContent,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { CounterSection(content) }
        item { HistorySection(content) }
        item { LeaderGrid(title = "У истоков института", people = content.foundingLeaders) }
        item { TodaySection(content) }
        item { LeaderGrid(title = "Институт сегодня", people = content.todayLeaders) }
        item { BulletSection("Почему выбирают нас?", content.whyChooseUs) }
        item { BulletSection("Наши преимущества", content.ourAdvantages) }
        item { ContactsSection(content, onCall, onEmail) }
    }
}

@Composable
private fun CounterSection(content: AboutPageContent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            content.counters.forEach { counter ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = counter.value + counter.suffix,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = counter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySection(content: AboutPageContent) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "История института",
            style = MaterialTheme.typography.headlineSmall
        )
        content.historyParagraphs.forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LeaderGrid(title: String, people: List<com.example.kubmi.domain.model.PersonSpotlight>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            people.forEach { person ->
                Card(
                    modifier = Modifier
                        .width(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoilImage(
                            imageUrl = person.imageUrl,
                            contentDescription = person.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = person.caption,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = person.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaySection(content: AboutPageContent) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Институт сегодня", style = MaterialTheme.typography.headlineSmall)
        content.todayParagraphs.forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BulletSection(title: String, bullets: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        bullets.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ContactsSection(
    content: AboutPageContent,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Прием абитуриентов", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Адрес", style = MaterialTheme.typography.titleMedium)
        content.contacts.addresses.forEach { addr ->
            Text(text = addr, style = MaterialTheme.typography.bodyLarge)
        }
        Text(text = "Телефоны", style = MaterialTheme.typography.titleMedium)
        content.contacts.phones.forEach { phone ->
            Text(
                text = "${phone.label}: ${phone.number}",
                modifier = Modifier.clickable { onCall(phone.number) },
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(text = "Email: ${content.contacts.email}", modifier = Modifier.clickable { onEmail(content.contacts.email) })
    }
}

