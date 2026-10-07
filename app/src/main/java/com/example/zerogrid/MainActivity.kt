package com.example.zerogrid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

import com.example.zerogrid.admin.AdminPanelScreen
import com.example.zerogrid.auth.LoginScreen
import com.example.zerogrid.auth.ProfileCompletionScreen
import com.example.zerogrid.auth.RegisterScreen
import com.example.zerogrid.navigation.ZeroGridApp
import com.example.zerogrid.service.MeshForegroundService
import com.example.zerogrid.ui.theme.ZeroGridTheme
import com.zerogrid.mesh.app.ui.UserRole
import com.zerogrid.mesh.app.ui.UserSessionManager
import com.zerogrid.mesh.app.ui.navigation.AppScreen
import com.example.zerogrid.ui.ThemeMode
import com.example.zerogrid.ui.ThemePreferenceManager

// ── Main App Gateway ───────────────────────────────────────────────────────

@Composable
fun MainAppGateway() {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager.getInstance(context) }

    // Determine starting screen: auto-login if a token is already stored
    val startScreen = remember {
        if (sessionManager.isLoggedIn()) {
            if (!sessionManager.isProfileComplete()) {
                AppScreen.ProfileCompletion
            } else {
                when (sessionManager.getUserRole()) {
                    UserRole.ADMIN   -> AppScreen.AdminPanel
                    UserRole.CITIZEN -> AppScreen.UserDashboard
                    null             -> AppScreen.Login
                }
            }
        } else {
            AppScreen.Login
        }
    }

    var currentScreen by remember { mutableStateOf<AppScreen>(startScreen) }

    fun routeToDashboard(role: UserRole) {
        currentScreen = if (role == UserRole.ADMIN) AppScreen.AdminPanel else AppScreen.UserDashboard
    }

    fun logout() {
        Log.d("MainAppGateway", "User logged out. Stopping mesh service.")
        try {
            MeshForegroundService.stopService(context)
            com.example.zerogrid.mesh.engine.MeshEngine.getInstance(context).stopMesh()
        } catch (e: Exception) {
            Log.e("MainAppGateway", "Error stopping mesh service on logout", e)
        }
        sessionManager.clearSession()
        currentScreen = AppScreen.Login
    }

    // Start mesh service, sync FCM token, and ensure notification permission on authenticated dashboard
    LaunchedEffect(currentScreen) {
        val isAuthenticated = currentScreen == AppScreen.UserDashboard || currentScreen == AppScreen.AdminPanel
        if (isAuthenticated && sessionManager.isLoggedIn()) {
            Log.d("MainAppGateway", "Authenticated screen active ($currentScreen). Starting mesh service.")
            MeshForegroundService.startService(context)
            com.example.zerogrid.service.ZeroGridFirebaseMessagingService.syncTokenWithBackend(context)
            (context as? MainActivity)?.requestNotificationPermission()
        }
    }

    // Back-handling: only block back on screens where it makes sense
    BackHandler(enabled = currentScreen == AppScreen.Register || currentScreen == AppScreen.ProfileCompletion) {
        if (currentScreen == AppScreen.Register) {
            currentScreen = AppScreen.Login
        } else if (currentScreen == AppScreen.ProfileCompletion) {
            val role = sessionManager.getUserRole() ?: UserRole.CITIZEN
            routeToDashboard(role)
        }
    }

    when (currentScreen) {

        // ── Auth screens ───────────────────────────────────────────────
        AppScreen.Login -> {
            LoginScreen(
                sessionManager = sessionManager,
                onNavigateToRegister = { currentScreen = AppScreen.Register },
                onLoginSuccess = { role, profileComplete ->
                    if (!profileComplete) {
                        currentScreen = AppScreen.ProfileCompletion
                    } else {
                        routeToDashboard(role)
                    }
                }
            )
        }

        AppScreen.Register -> {
            RegisterScreen(
                sessionManager = sessionManager,
                onNavigateToLogin = { currentScreen = AppScreen.Login },
                onRegisterSuccess = { role, profileComplete ->
                    if (!profileComplete) {
                        currentScreen = AppScreen.ProfileCompletion
                    } else {
                        routeToDashboard(role)
                    }
                }
            )
        }

        // ── Profile completion ─────────────────────────────────────────
        AppScreen.ProfileCompletion -> {
            ProfileCompletionScreen(
                sessionManager = sessionManager,
                onProfileCompleted = {
                    val role = sessionManager.getUserRole() ?: UserRole.CITIZEN
                    routeToDashboard(role)
                },
                onSkip = {
                    val role = sessionManager.getUserRole() ?: UserRole.CITIZEN
                    routeToDashboard(role)
                }
            )
        }

        // ── Citizen: existing mesh app ─────────────────────────────────
        AppScreen.UserDashboard -> {
            val activity = context as? ComponentActivity
            val intent = activity?.intent
            val sosId = intent?.getStringExtra("sosId")
                ?: intent?.getStringExtra("EXTRA_SOS_ID")
            val latStr = intent?.getStringExtra("lat")
                ?: intent?.getStringExtra("EXTRA_LAT")
            val lngStr = intent?.getStringExtra("lng")
                ?: intent?.getStringExtra("EXTRA_LNG")
            val senderName = intent?.getStringExtra("senderName")
                ?: intent?.getStringExtra("EXTRA_SENDER_NAME")
                ?: "Emergency Contact"
            val category = intent?.getStringExtra("category")
                ?: intent?.getStringExtra("EXTRA_CATEGORY")
                ?: "SOS"
            val message = intent?.getStringExtra("message")
                ?: intent?.getStringExtra("EXTRA_MESSAGE")
                ?: ""

            val lat = latStr?.toDoubleOrNull() ?: 0.0
            val lng = lngStr?.toDoubleOrNull() ?: 0.0
            val hasSosIntent = !sosId.isNullOrBlank() || intent?.hasExtra("EXTRA_SOS_ID") == true || intent?.hasExtra("sosId") == true

            // Automatically inject the SOS into MeshEngine so it shows up in Emergency Center and alert logs
            if (hasSosIntent && (lat != 0.0 || lng != 0.0)) {
                try {
                    com.example.zerogrid.mesh.engine.MeshEngine.getInstance(context).recordExternalSosAlert(
                        sosId = sosId ?: java.util.UUID.randomUUID().toString(),
                        senderName = senderName,
                        category = category,
                        message = message,
                        lat = lat,
                        lng = lng
                    )
                } catch (_: Exception) {}
            }

            val initialTrackTarget = if ((lat != 0.0 || lng != 0.0) && hasSosIntent) {
                com.example.zerogrid.navigation.SosTrackingTarget(
                    lat = lat,
                    lng = lng,
                    name = senderName,
                    category = category
                )
            } else null

            val initialScreen = when {
                initialTrackTarget != null -> com.example.zerogrid.navigation.Screen.TRACK_SOS
                hasSosIntent -> com.example.zerogrid.navigation.Screen.SOS_CENTER
                else -> com.example.zerogrid.navigation.Screen.HOME
            }
            ZeroGridApp(
                initialScreen = initialScreen,
                initialTrackTarget = initialTrackTarget,
                onLogout = { logout() }
            )
        }

        // ── Admin: dedicated admin panel ───────────────────────────────
        AppScreen.AdminPanel -> {
            AdminPanelScreen(
                sessionManager = sessionManager,
                onOpenMeshApp = { currentScreen = AppScreen.UserDashboard },
                onLogout = { logout() }
            )
        }
    }
}

// ── MainActivity ───────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("MainActivity", "All required permissions granted.")
        } else {
            Log.w("MainActivity", "Some permissions were denied. Mesh functionality may be limited.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        checkAndRequestPermissions()

        val themePreferenceManager = ThemePreferenceManager(this)
        val initialThemeMode = themePreferenceManager.getThemeModeSync()

        // Set window background BEFORE setContent to eliminate window launch flash
        val isNightMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val initialWindowBgColor = when (initialThemeMode) {
            ThemeMode.LIGHT -> android.graphics.Color.parseColor("#F8FAFC")
            ThemeMode.DARK -> android.graphics.Color.parseColor("#0B1312")
            ThemeMode.SYSTEM -> if (isNightMode) {
                android.graphics.Color.parseColor("#0B1312")
            } else {
                android.graphics.Color.parseColor("#F8FAFC")
            }
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(initialWindowBgColor))

        setContent {
            // Collect theme state starting synchronously with saved preference
            val themeMode by themePreferenceManager.themeMode.collectAsState(initial = initialThemeMode)

            // Resolve the actual boolean to pass into your theme
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SideEffect {
                val currentBg = if (isDarkTheme) {
                    android.graphics.Color.parseColor("#0B1312")
                } else {
                    android.graphics.Color.parseColor("#F8FAFC")
                }
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(currentBg))
            }

            ZeroGridTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppGateway()
                }
            }
        }
        Log.d("MainActivity", "onCreate finished in ${System.currentTimeMillis() - startTime}ms")
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.d("MainActivity", "Requesting missing permissions: $missingPermissions")
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d("MainActivity", "All required permissions already granted.")
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting contextual POST_NOTIFICATIONS permission for emergency alerts.")
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }
}