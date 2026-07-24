package com.example.kubmi.presentation.screens.admin

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.AuthState
import com.example.kubmi.kiosk.AdvancedKioskManager
import com.example.kubmi.presentation.navigation.Screen
import com.example.kubmi.util.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val viewModel: AdminAuthViewModel = hiltViewModel()
    val context = LocalContext.current
    val kioskManager = remember(context) {
        AdvancedKioskManager(context.applicationContext)
    }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Initial -> {
            // First time - set password
            SetPasswordScreen(
                password = password,
                onPasswordChange = { password = it },
                passwordConfirmation = passwordConfirmation,
                onPasswordConfirmationChange = { passwordConfirmation = it },
                onSetPassword = { 
                    viewModel.setPassword(password, passwordConfirmation)
                },
                onBack = { navController.popBackStack() },
                errorMessage = state.errorMessage
            )
        }
        
        is AuthState.RequiresAuth -> {
            // Password exists - authenticate
            LoginScreen(
                password = password,
                onPasswordChange = { password = it },
                onLogin = { 
                    viewModel.authenticate(password)
                    password = ""
                },
                onBack = { navController.popBackStack() },
                errorMessage = state.errorMessage
            )
        }
        
        is AuthState.Authenticated -> {
            AdminMainScreen(navController, kioskManager)
        }
        
        is AuthState.Loading -> {
            LoadingScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetPasswordScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordConfirmation: String,
    onPasswordConfirmationChange: (String) -> Unit,
    onSetPassword: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_set_password_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.admin_enter_new_password),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = passwordConfirmation,
                onValueChange = onPasswordConfirmationChange,
                label = { Text(stringResource(R.string.confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSetPassword,
                enabled = password.length >= SecurePreferences.MIN_PASSWORD_LENGTH &&
                    passwordConfirmation.isNotBlank(),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.set_password))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.admin_enter_password),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onLogin,
                enabled = password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.login))
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    navController: NavController,
    kioskManager: AdvancedKioskManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val securePrefs = remember { SecurePreferences(context.applicationContext) }
    var exitPassword by remember { mutableStateOf("") }
    var exitError by remember { mutableStateOf(false) }
    var exitVerificationInProgress by remember { mutableStateOf(false) }
    var exitDurationMs by remember { mutableStateOf(2 * 60_000L) }
    var screensaverMode by remember { mutableStateOf(securePrefs.getScreensaverMode()) }
    var showInstructions by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_panel)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.close)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kiosk Settings Button - most important
            Button(
                onClick = { navController.navigate(Screen.KioskSettings.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.kiosk_settings_title))
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Screensaver Settings Section
            Text(
                text = stringResource(R.string.screensaver_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.screensaver_mode_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // News mode radio button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = screensaverMode == "news",
                            onClick = {
                                screensaverMode = "news"
                                securePrefs.saveScreensaverMode("news")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.screensaver_mode_news),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.screensaver_mode_news_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // SFTP mode radio button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = screensaverMode == "sftp",
                            onClick = {
                                screensaverMode = "sftp"
                                securePrefs.saveScreensaverMode("sftp")
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.screensaver_mode_sftp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.screensaver_mode_sftp_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show instructions button
                    TextButton(
                        onClick = { showInstructions = !showInstructions },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showInstructions) 
                                stringResource(R.string.hide_instructions) 
                            else 
                                stringResource(R.string.show_upload_instructions)
                        )
                    }
                    
                    // Instructions card (collapsible)
                    if (showInstructions) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.sftp_upload_instructions_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = stringResource(R.string.sftp_server_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                Text(
                                    text = stringResource(R.string.sftp_instructions_windows),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Text(
                                    text = stringResource(R.string.sftp_instructions_macos_linux),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Text(
                                    text = stringResource(R.string.sftp_instructions_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Exit section
            Text(
                text = stringResource(R.string.admin_exit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.admin_exit_duration),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    60_000L to "1 мин",
                    2 * 60_000L to "2 мин",
                    5 * 60_000L to "5 мин"
                ).forEach { (duration, label) ->
                    FilterChip(
                        selected = exitDurationMs == duration,
                        onClick = { exitDurationMs = duration },
                        label = { Text(label) }
                    )
                }
            }
            
            OutlinedTextField(
                value = exitPassword,
                onValueChange = {
                    exitPassword = it
                    exitError = false
                },
                label = { Text(stringResource(R.string.admin_exit_prompt)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = exitError,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (exitError) {
                val remainingSeconds =
                    (securePrefs.getRemainingLockoutMillis() + 999L) / 1000L
                Text(
                    text = if (remainingSeconds > 0L) {
                        "Слишком много попыток. Повторите через $remainingSeconds сек."
                    } else {
                        stringResource(R.string.admin_exit_error)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Button(
                onClick = {
                    exitVerificationInProgress = true
                    coroutineScope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            securePrefs.verifyPassword(exitPassword)
                        }
                        exitVerificationInProgress = false
                        if (ok) {
                            val activity = context as? Activity
                            if (activity != null) {
                                exitPassword = ""
                                exitError = false
                                kioskManager.exitKioskForAdmin(activity, exitDurationMs)
                            } else {
                                exitError = true
                            }
                        } else {
                            exitError = true
                        }
                    }
                },
                enabled = exitPassword.isNotBlank() && !exitVerificationInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (exitVerificationInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Выйти из kiosk-режима (${exitDurationMs / 60_000L} мин)")
                }
            }
        }
    }
}
