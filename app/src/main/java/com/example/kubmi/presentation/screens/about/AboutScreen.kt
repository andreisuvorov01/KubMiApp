package com.example.kubmi.presentation.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kubmi.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ContactMail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    var selectedSection by remember { mutableStateOf("history") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_institute)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Navigation rail
            NavigationRail {
                Text(
                    text = stringResource(R.string.sections),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationRailItem(
                    selected = selectedSection == "history",
                    onClick = { selectedSection = "history" },
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.History,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.history)) }
                )
                NavigationRailItem(
                    selected = selectedSection == "management",
                    onClick = { selectedSection = "management" },
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.People,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.management)) }
                )
                NavigationRailItem(
                    selected = selectedSection == "faculties",
                    onClick = { selectedSection = "faculties" },
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.School,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.faculties)) }
                )
                NavigationRailItem(
                    selected = selectedSection == "contacts",
                    onClick = { selectedSection = "contacts" },
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ContactMail,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.contacts)) }
                )
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    when (selectedSection) {
                        "history" -> {
                            Text(
                                text = stringResource(R.string.institute_history),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.institute_history_description),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        "management" -> {
                            Text(
                                text = stringResource(R.string.management),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.management_description),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        "faculties" -> {
                            Text(
                                text = stringResource(R.string.faculties_and_departments),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.faculties_description),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        "contacts" -> {
                            Text(
                                text = stringResource(R.string.contact_information),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.address),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.phone),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.email),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
