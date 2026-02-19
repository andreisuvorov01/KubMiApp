package com.example.kubmi.presentation.screens.admin

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.kubmi.presentation.navigation.Screen
import com.example.kubmi.service.KioskService
import com.example.kubmi.util.SecurePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val viewModel: AdminAuthViewModel = hiltViewModel()
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Initial -> {
            // First time - set password
            SetPasswordScreen(
                password = password,
                onPasswordChange = { password = it },
                onSetPassword = { 
                    viewModel.setPassword(password)
                    password = ""
                },
                onBack = { navController.popBackStack() }
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
            AdminMainScreen(navController)
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
    onSetPassword: () -> Unit,
    onBack: () -> Unit
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSetPassword,
                enabled = password.isNotBlank(),
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
fun AdminMainScreen(navController: NavController) {
    val context = LocalContext.current
    val securePrefs = remember { SecurePreferences(context.applicationContext) }
    var exitPassword by remember { mutableStateOf("") }
    var exitError by remember { mutableStateOf(false) }
    var exitSuccess by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_panel)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kiosk Settings Button - most important
            Button(
                onClick = {
                    navController.navigate(Screen.KioskSettings.route)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.kiosk_settings_title))
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Exit section
            Text(
                text = stringResource(R.string.admin_exit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = exitPassword,
                onValueChange = {
                    exitPassword = it
                    exitError = false
                    exitSuccess = false
                },
                label = { Text(stringResource(R.string.admin_exit_prompt)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = exitError,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (exitError) {
                Text(
                    text = stringResource(R.string.admin_exit_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (exitSuccess) {
                Text(
                    text = stringResource(R.string.admin_exit_success),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Button(
                onClick = {
                    val ok = securePrefs.verifyPassword(exitPassword)
                    if (ok) {
                        allowAdminExit(context)
                        exitPassword = ""
                        exitError = false
                        exitSuccess = true
                    } else {
                        exitError = true
                        exitSuccess = false
                    }
                },
                enabled = exitPassword.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.admin_exit_allow))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Exit app button at the bottom
            Button(
                onClick = { 
                    (context as? Activity)?.let { activity ->
                        com.example.kubmi.util.KioskManager.disableKioskMode(activity)
                        com.example.kubmi.util.KioskManager.stopLockTask(activity)
                        activity.finish()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.exit_app))
            }
        }
    }
}

private fun allowAdminExit(context: Context) {
    val prefs = context.getSharedPreferences(KioskService.PREF_KIOSK_GUARD, Context.MODE_PRIVATE)
    prefs.edit()
        .putLong(
            KioskService.KEY_ALLOW_EXIT_UNTIL,
            System.currentTimeMillis() + KioskService.ADMIN_EXIT_WINDOW_MS
        )
        .apply()
    
    // Disable kiosk mode and stop lock task when exiting
    (context as? Activity)?.let { activity ->
        com.example.kubmi.util.KioskManager.disableKioskMode(activity)
        com.example.kubmi.util.KioskManager.stopLockTask(activity)
    }
    
    context.stopService(Intent(context, KioskService::class.java))
}
