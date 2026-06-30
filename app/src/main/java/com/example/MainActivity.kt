package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import com.example.ui.components.MaterialYouKeypadButton
import com.example.ui.components.MaterialYouPinDot
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AppLockManager
import com.example.ui.AppItem
import com.example.ui.AppLockViewModel
import com.example.ui.theme.MyApplicationTheme

import com.example.ui.components.PatternEnrollmentDialog
import com.example.ui.components.PatternVerificationDialog
import com.example.ui.components.BiometricPromptHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val viewModel: AppLockViewModel = viewModel()
    
    val configuredPin by viewModel.configuredPin.collectAsState()
    val lockType by viewModel.lockType.collectAsState()
    val configuredPattern by viewModel.configuredPattern.collectAsState()
    val isFaceRegistered by viewModel.isFaceRegistered.collectAsState()
    val isFaceUnlockEnabled by viewModel.isFaceUnlockEnabled.collectAsState()
    var isAppUnlockedInSession by rememberSaveable { mutableStateOf(false) }

    // Refresh accessibility status whenever app comes to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAccessibilityServiceStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasProtection = if (lockType == "pattern") {
        !configuredPattern.isNullOrEmpty()
    } else {
        !configuredPin.isNullOrEmpty()
    }

    if (hasProtection && !isAppUnlockedInSession) {
        // App-level lock to protect App Locker itself
        AppEntryLockScreen(
            lockType = lockType,
            correctPin = configuredPin ?: "",
            correctPattern = configuredPattern ?: "",
            onUnlockSuccess = {
                isAppUnlockedInSession = true
            },
            isFaceRegistered = isFaceRegistered,
            isFaceUnlockEnabled = isFaceUnlockEnabled
        )
    } else {
        // Main App Content
        AppLockerDashboard(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockerDashboard(viewModel: AppLockViewModel) {
    val context = LocalContext.current
    
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val isLockerEnabled by viewModel.isLockerEnabled.collectAsState()
    val configuredPin by viewModel.configuredPin.collectAsState()
    val lockType by viewModel.lockType.collectAsState()
    val configuredPattern by viewModel.configuredPattern.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val unlockDurationSeconds by viewModel.unlockDurationSeconds.collectAsState()
    val unlockMode by viewModel.unlockMode.collectAsState()

    val isFaceRegistered by viewModel.isFaceRegistered.collectAsState()
    val isFaceUnlockEnabled by viewModel.isFaceUnlockEnabled.collectAsState()

    var activeTab by rememberSaveable { mutableStateOf(0) } // 0 = Apps, 1 = Settings
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showSetPatternDialog by remember { mutableStateOf(false) }

    // Verification and enrollment states
    var pendingAuthorizedAction by remember { mutableStateOf(AuthorizedAction.NONE) }
    var pendingLockerValue by remember { mutableStateOf(false) }
    var pendingFaceUnlockValue by remember { mutableStateOf(false) }
    var pendingLockTypeTarget by remember { mutableStateOf("pin") }

    var showVerificationFaceDialog by remember { mutableStateOf(false) }
    var showVerificationPinDialog by remember { mutableStateOf(false) }
    var showVerificationPatternDialog by remember { mutableStateOf(false) }
    var showFaceEnrollDialog by remember { mutableStateOf(false) }

    val onAuthSuccess = {
        when (pendingAuthorizedAction) {
            AuthorizedAction.TOGGLE_LOCKER -> {
                viewModel.setLockerEnabled(pendingLockerValue)
                Toast.makeText(context, "Действие подтверждено", Toast.LENGTH_SHORT).show()
            }
            AuthorizedAction.CHANGE_PIN -> {
                showSetPinDialog = true
            }
            AuthorizedAction.ENROLL_PATTERN -> {
                showSetPatternDialog = true
            }
            AuthorizedAction.TOGGLE_FACE_UNLOCK -> {
                viewModel.setFaceUnlockEnabled(pendingFaceUnlockValue)
                Toast.makeText(context, "Действие подтверждено", Toast.LENGTH_SHORT).show()
            }
            AuthorizedAction.ENROLL_FACE -> {
                showFaceEnrollDialog = true
            }
            AuthorizedAction.CHANGE_LOCK_TYPE -> {
                viewModel.saveLockType(pendingLockTypeTarget)
                Toast.makeText(context, "Способ изменен на ${if (pendingLockTypeTarget == "pattern") "графический ключ" else "PIN-код"}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
        pendingAuthorizedAction = AuthorizedAction.NONE
        showVerificationFaceDialog = false
        showVerificationPinDialog = false
        showVerificationPatternDialog = false
    }

    fun requestAuthorization(
        action: AuthorizedAction,
        lockerValue: Boolean = false,
        faceUnlockValue: Boolean = false,
        lockTypeTarget: String = "pin"
    ) {
        val hasProtection = if (lockType == "pattern") !configuredPattern.isNullOrEmpty() else !configuredPin.isNullOrEmpty()
        
        if (!hasProtection) {
            // No credentials set yet, allow direct execution
            when (action) {
                AuthorizedAction.TOGGLE_LOCKER -> viewModel.setLockerEnabled(lockerValue)
                AuthorizedAction.CHANGE_PIN -> showSetPinDialog = true
                AuthorizedAction.ENROLL_PATTERN -> showSetPatternDialog = true
                AuthorizedAction.TOGGLE_FACE_UNLOCK -> viewModel.setFaceUnlockEnabled(faceUnlockValue)
                AuthorizedAction.ENROLL_FACE -> showFaceEnrollDialog = true
                AuthorizedAction.CHANGE_LOCK_TYPE -> viewModel.saveLockType(lockTypeTarget)
                else -> {}
            }
        } else {
            pendingAuthorizedAction = action
            pendingLockerValue = lockerValue
            pendingFaceUnlockValue = faceUnlockValue
            pendingLockTypeTarget = lockTypeTarget
            
            if (isFaceRegistered && isFaceUnlockEnabled && context is FragmentActivity) {
                BiometricPromptHelper.showBiometricPrompt(
                    activity = context,
                    onSuccess = {
                        onAuthSuccess()
                    },
                    onError = { err ->
                        if (lockType == "pattern") {
                            showVerificationPatternDialog = true
                        } else {
                            showVerificationPinDialog = true
                        }
                    }
                )
            } else {
                if (lockType == "pattern") {
                    showVerificationPatternDialog = true
                } else {
                    showVerificationPinDialog = true
                }
            }
        }
    }

    val blockedPackageNames = remember(blockedApps) {
        blockedApps.map { it.packageName }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Locker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadInstalledApps() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav")
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Приложения") },
                    label = { Text("Приложения") },
                    modifier = Modifier.testTag("tab_apps")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status Block: If service is inactive or no PIN is set, show attention banners
            StatusSection(
                isAccessibilityEnabled = isAccessibilityEnabled,
                hasPin = !configuredPin.isNullOrEmpty(),
                onEnableServiceClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                    }
                },
                onSetPinClick = {
                    requestAuthorization(AuthorizedAction.CHANGE_PIN, false, false)
                }
            )

            if (activeTab == 0) {
                // APPS LIST TAB
                AppsListScreen(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    apps = filteredApps,
                    blockedPackages = blockedPackageNames,
                    isLoading = isLoadingApps,
                    onToggleBlock = { app, shouldBlock ->
                        if (configuredPin.isNullOrEmpty()) {
                            Toast.makeText(context, "Сначала установите PIN-код в настройках!", Toast.LENGTH_LONG).show()
                            showSetPinDialog = true
                        } else {
                            viewModel.toggleAppBlock(app, shouldBlock)
                        }
                    }
                )
            } else {
                // SETTINGS TAB
                SettingsScreen(
                    isLockerEnabled = isLockerEnabled,
                    onToggleLocker = { requestAuthorization(AuthorizedAction.TOGGLE_LOCKER, it, false) },
                    hasPin = !configuredPin.isNullOrEmpty(),
                    onChangePinClick = { requestAuthorization(AuthorizedAction.CHANGE_PIN, false, false) },
                    lockType = lockType,
                    onLockTypeChange = { targetType ->
                        requestAuthorization(AuthorizedAction.CHANGE_LOCK_TYPE, false, false, targetType)
                    },
                    hasPattern = !configuredPattern.isNullOrEmpty(),
                    onChangePatternClick = { requestAuthorization(AuthorizedAction.ENROLL_PATTERN, false, false) },
                    unlockDurationSeconds = unlockDurationSeconds,
                    onDurationChange = { viewModel.setUnlockDuration(it) },
                    unlockMode = unlockMode,
                    onUnlockModeChange = { viewModel.setUnlockMode(it) },
                    isFaceRegistered = isFaceRegistered,
                    isFaceUnlockEnabled = isFaceUnlockEnabled,
                    onToggleFaceUnlock = { requestAuthorization(AuthorizedAction.TOGGLE_FACE_UNLOCK, false, it) },
                    onEnrollFaceClick = { requestAuthorization(AuthorizedAction.ENROLL_FACE, false, false) }
                )
            }
        }

        // Beautiful PIN Setup Dialog / Sheet
        if (showSetPinDialog) {
            PinSetupDialog(
                onDismiss = { showSetPinDialog = false },
                onPinSet = { pin ->
                    viewModel.savePinCode(pin)
                    showSetPinDialog = false
                    Toast.makeText(context, "PIN-код успешно сохранен!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Verification Dialogs
        if (showVerificationPinDialog) {
            PinVerificationDialog(
                onDismiss = {
                    showVerificationPinDialog = false
                    pendingAuthorizedAction = AuthorizedAction.NONE
                },
                correctPin = configuredPin ?: "",
                onVerificationSuccess = {
                    onAuthSuccess()
                }
            )
        }

        if (showSetPatternDialog) {
            PatternEnrollmentDialog(
                onDismiss = { showSetPatternDialog = false },
                onEnrollSuccess = { pattern ->
                    viewModel.savePatternCode(pattern)
                    viewModel.saveLockType("pattern")
                    showSetPatternDialog = false
                    Toast.makeText(context, "Графический ключ успешно настроен!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showVerificationPatternDialog) {
            PatternVerificationDialog(
                onDismiss = {
                    showVerificationPatternDialog = false
                    pendingAuthorizedAction = AuthorizedAction.NONE
                },
                correctPattern = configuredPattern ?: "",
                onVerificationSuccess = {
                    onAuthSuccess()
                }
            )
        }
    }
}

@Composable
fun StatusSection(
    isAccessibilityEnabled: Boolean,
    hasPin: Boolean,
    onEnableServiceClick: () -> Unit,
    onSetPinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hasPin) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetPinClick() }
                    .testTag("set_pin_warning_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Предупреждение",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Установите PIN-код",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Без PIN-кода блокировка приложений работать не будет. Нажмите, чтобы установить.",
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else if (!isAccessibilityEnabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_warning_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Предупреждение службы",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Служба доступности отключена",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Для работы блокировщика необходимо включить службу доступности в настройках Android.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onEnableServiceClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("enable_service_button")
                    ) {
                        Text("Включить в настройках", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF2E7D32), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Активна",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Защита активна",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Служба запущена и контролирует доступ к выбранным приложениям.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppsListScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    apps: List<AppItem>,
    blockedPackages: Set<String>,
    isLoading: Boolean,
    onToggleBlock: (AppItem, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("search_field"),
            placeholder = { Text("Поиск приложений...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Загрузка списка приложений...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Пусто",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Нет доступных приложений",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("apps_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val isBlocked = blockedPackages.contains(app.packageName)
                    AppRow(
                        app = app,
                        isBlocked = isBlocked,
                        onBlockChange = { onToggleBlock(app, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRow(
    app: AppItem,
    isBlocked: Boolean,
    onBlockChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var iconDrawable by remember(app.packageName) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(app.packageName) {
        try {
            iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            // fallback
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_row_${app.packageName}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                iconDrawable?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } ?: Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name and package
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Lock toggle switch
            Switch(
                checked = isBlocked,
                onCheckedChange = onBlockChange,
                modifier = Modifier.testTag("switch_${app.packageName}")
            )
        }
    }
}

@Composable
fun SettingsScreen(
    isLockerEnabled: Boolean,
    onToggleLocker: (Boolean) -> Unit,
    hasPin: Boolean,
    onChangePinClick: () -> Unit,
    lockType: String,
    onLockTypeChange: (String) -> Unit,
    hasPattern: Boolean,
    onChangePatternClick: () -> Unit,
    unlockDurationSeconds: Int,
    onDurationChange: (Int) -> Unit,
    unlockMode: String,
    onUnlockModeChange: (String) -> Unit,
    isFaceRegistered: Boolean,
    isFaceUnlockEnabled: Boolean,
    onToggleFaceUnlock: (Boolean) -> Unit,
    onEnrollFaceClick: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle locking
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Общая блокировка",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Включить или выключить защиту для всех выбранных программ.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLockerEnabled,
                        onCheckedChange = onToggleLocker,
                        modifier = Modifier.testTag("toggle_locker_global")
                    )
                }
            }
        }

        // Security settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Безопасность",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Choice of Lock Type
                    Column {
                        Text(
                            text = "Способ защиты",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Выберите тип блокировки для приложений.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("pin" to "Цифровой PIN", "pattern" to "Графический ключ").forEach { (type, label) ->
                                val selected = lockType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onLockTypeChange(type)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // PIN Setup Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangePinClick() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (hasPin) "Изменить PIN-код" else "Установить PIN-код",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Используется для защиты выбранных приложений и входа.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (hasPin) "Изменить" else "Настроить",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Pattern Setup Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangePatternClick() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (hasPattern) "Изменить графический ключ" else "Установить графический ключ",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Графический рисунок по точкам для разблокировки.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (hasPattern) "Изменить" else "Настроить",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Unlock Mode Setting
                    Column {
                        Text(
                            text = "Режим разблокировки",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Выберите, когда приложение должно запрашивать PIN-код снова.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("until_closed" to "До закрытия", "timer" to "По таймеру").forEach { (mode, label) ->
                                val selected = unlockMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onUnlockModeChange(mode)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Grace Period Duration (Shown only if unlockMode is "timer")
                    if (unlockMode == "timer") {
                        Column {
                            Text(
                                text = "Задержка блокировки: $unlockDurationSeconds сек",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Время, в течение которого разблокированное приложение остается доступным без повторного ввода PIN.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0, 15, 60, 300).forEach { sec ->
                                    val label = when (sec) {
                                        0 -> "Сразу"
                                        15 -> "15с"
                                        60 -> "1м"
                                        300 -> "5м"
                                        else -> "${sec}с"
                                    }
                                    val selected = unlockDurationSeconds == sec
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                onDurationChange(sec)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Explanations about how it works
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "О технологии",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Приложение использует системное API Службы специальных возможностей (AccessibilityService). Это позволяет нам безопасно обнаруживать запуски защищенных приложений прямо в системе, не потребляя лишнюю батарею в фоне. Мы уважаем вашу конфиденциальность: сервис работает полностью локально и не передает никаких данных в интернет.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = Enter new PIN, 2 = Confirm PIN
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val currentEnteredPin = if (step == 1) pin1 else pin2

    LaunchedEffect(currentEnteredPin) {
        if (currentEnteredPin.length == 4) {
            if (step == 1) {
                step = 2
                errorMsg = null
            } else {
                if (pin1 == pin2) {
                    onPinSet(pin1)
                } else {
                    errorMsg = "PIN-коды не совпадают!"
                    pin2 = ""
                    step = 1
                    pin1 = ""
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (step == 1) "Придумайте PIN-код" else "Подтвердите PIN-код",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Закрыть")
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (step == 1) "Введите 4 цифры нового PIN-кода" else "Введите PIN-код еще раз для подтверждения",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Dots indicator
                Row {
                    for (i in 0 until 4) {
                        val isFilled = i < currentEnteredPin.length
                        MaterialYouPinDot(
                            isFilled = isFilled,
                            isError = errorMsg != null
                        )
                    }
                }
            }

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DELETE")
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.width(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(keys) { key ->
                        if (key.isNotEmpty()) {
                            if (key == "DELETE") {
                                MaterialYouKeypadButton(
                                    key = key,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (step == 1 && pin1.isNotEmpty()) {
                                            pin1 = pin1.dropLast(1)
                                        } else if (step == 2 && pin2.isNotEmpty()) {
                                            pin2 = pin2.dropLast(1)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                MaterialYouKeypadButton(
                                    key = key,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (step == 1 && pin1.length < 4) {
                                            pin1 += key
                                        } else if (step == 2 && pin2.length < 4) {
                                            pin2 += key
                                        }
                                    }
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }
        }
    }
}

enum class AuthorizedAction {
    NONE,
    TOGGLE_LOCKER,
    CHANGE_PIN,
    TOGGLE_FACE_UNLOCK,
    ENROLL_FACE,
    ENROLL_PATTERN,
    CHANGE_LOCK_TYPE
}

@Composable
fun PinVerificationDialog(
    onDismiss: () -> Unit,
    correctPin: String,
    onVerificationSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == correctPin) {
                onVerificationSuccess()
            } else {
                isError = true
                enteredPin = ""
            }
        } else if (enteredPin.length > 0) {
            isError = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Подтверждение",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Требуется авторизация",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (isError) "Неверный PIN-код! Попробуйте еще раз" else "Введите ваш PIN-код для подтверждения действия",
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        MaterialYouPinDot(
                            isFilled = isFilled,
                            isError = isError
                        )
                    }
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DELETE")
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.width(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(keys) { key ->
                        if (key.isNotEmpty()) {
                            if (key == "DELETE") {
                                MaterialYouKeypadButton(
                                    key = key,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                MaterialYouKeypadButton(
                                    key = key,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                        }
                                    }
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppEntryLockScreen(
    lockType: String,
    correctPin: String,
    correctPattern: String,
    onUnlockSuccess: () -> Unit,
    isFaceRegistered: Boolean,
    isFaceUnlockEnabled: Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf(if (lockType == "pattern") "Нарисуйте графический ключ для входа" else "Введите PIN-код для входа") }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == correctPin) {
                onUnlockSuccess()
            } else {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                isError = true
                enteredPin = ""
                statusText = "Неверный PIN! Попробуйте еще раз"
            }
        } else if (enteredPin.length > 0) {
            isError = false
            statusText = "Введите PIN-код для входа"
        }
    }

    LaunchedEffect(isError) {
        if (isError) {
            delay(1500)
            isError = false
            statusText = if (lockType == "pattern") "Нарисуйте графический ключ для входа" else "Введите PIN-код для входа"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer 
                                    else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (isError) MaterialTheme.colorScheme.onErrorContainer 
                               else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Доступ ограничен",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (lockType == "pattern") {
                // Pattern Lock Drawing Interface
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.components.PatternLockView(
                        modifier = Modifier.size(300.dp),
                        isError = isError,
                        onPatternCompleted = { pattern ->
                            val patternStr = pattern.joinToString(separator = "")
                            if (patternStr == correctPattern) {
                                onUnlockSuccess()
                            } else {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                isError = true
                                statusText = "Неверный графический ключ!"
                            }
                        }
                    )
                }
            } else {
                // PIN Dots Indicators
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Row {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPin.length
                            MaterialYouPinDot(
                                isFilled = isFilled,
                                isError = isError
                            )
                        }
                    }
                }
            }

            // Biometrics Prompt trigger / Back options if applicable
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (lockType == "pin") {
                    // Numeric Keypad (only for PIN mode)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DELETE")
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.width(280.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(keys) { key ->
                                if (key.isNotEmpty()) {
                                    if (key == "DELETE") {
                                        MaterialYouKeypadButton(
                                            key = key,
                                            onClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        MaterialYouKeypadButton(
                                            key = key,
                                            onClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = key,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(72.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
