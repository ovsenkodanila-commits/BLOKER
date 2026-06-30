package com.example.ui

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppLockerApplication
import com.example.data.AppLockRepository
import com.example.data.BlockedAppEntity
import com.example.service.AppLockAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val appName: String
)

class AppLockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppLockRepository = (application as AppLockerApplication).repository
    private val packageManager: PackageManager = application.packageManager

    // Holds all installed apps loaded from package manager
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    // Holds loading state for installed apps
    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps = _isLoadingApps.asStateFlow()

    // Search filter query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Accessibility Service Enabled status
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled = _isAccessibilityEnabled.asStateFlow()

    // Database flow of blocked package names
    val blockedApps: StateFlow<List<BlockedAppEntity>> = repository.blockedAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow of settings
    val configuredPin: StateFlow<String?> = repository.getSettingFlow("pin")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val lockType: StateFlow<String> = repository.getSettingFlow("lock_type")
        .combine(MutableStateFlow("pin")) { value, _ ->
            value ?: "pin"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "pin"
        )

    val configuredPattern: StateFlow<String?> = repository.getSettingFlow("pattern")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isLockerEnabled: StateFlow<Boolean> = repository.getSettingFlow("locker_enabled")
        .combine(MutableStateFlow(true)) { value, _ ->
            value?.toBoolean() ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val unlockDurationSeconds: StateFlow<Int> = repository.getSettingFlow("unlock_duration")
        .combine(MutableStateFlow(15)) { value, _ ->
            value?.toIntOrNull() ?: 15
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 15
        )

    val unlockMode: StateFlow<String> = repository.getSettingFlow("unlock_mode")
        .combine(MutableStateFlow("until_closed")) { value, _ ->
            value ?: "until_closed"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "until_closed"
        )

    val isFaceRegistered: StateFlow<Boolean> = repository.getSettingFlow("face_registered")
        .combine(MutableStateFlow(false)) { value, _ ->
            value?.toBoolean() ?: false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isFaceUnlockEnabled: StateFlow<Boolean> = repository.getSettingFlow("face_unlock_enabled")
        .combine(MutableStateFlow(false)) { value, _ ->
            value?.toBoolean() ?: false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Filtered list combining installed apps, search query, and block status
    val filteredApps = combine(
        _installedApps,
        _searchQuery,
        blockedApps
    ) { apps, query, blocked ->
        val queryLower = query.lowercase().trim()
        val filtered = if (queryLower.isEmpty()) {
            apps
        } else {
            apps.filter { it.appName.lowercase().contains(queryLower) || it.packageName.lowercase().contains(queryLower) }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadInstalledApps()
        checkAccessibilityServiceStatus()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                try {
                    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
                    val ourPackageName = getApplication<Application>().packageName

                    resolveInfos.asSequence()
                        .map { it.activityInfo.packageName }
                        .filter { it != ourPackageName }
                        .distinct()
                        .map { pkg ->
                            val label = try {
                                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                                packageManager.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                pkg
                            }
                            AppItem(pkg, label)
                        }
                        .sortedBy { it.appName.lowercase() }
                        .toList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun checkAccessibilityServiceStatus() {
        val context = getApplication<Application>()
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val isEnabled = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == AppLockAccessibilityService::class.java.name
        }
        _isAccessibilityEnabled.value = isEnabled
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppBlock(app: AppItem, shouldBlock: Boolean) {
        viewModelScope.launch {
            if (shouldBlock) {
                repository.blockApp(app.packageName, app.appName)
            } else {
                repository.unblockApp(app.packageName)
            }
        }
    }

    fun setLockerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("locker_enabled", enabled.toString())
        }
    }

    fun savePinCode(pin: String) {
        viewModelScope.launch {
            repository.saveSetting("pin", pin)
        }
    }

    fun saveLockType(type: String) {
        viewModelScope.launch {
            repository.saveSetting("lock_type", type)
        }
    }

    fun savePatternCode(pattern: String) {
        viewModelScope.launch {
            repository.saveSetting("pattern", pattern)
        }
    }

    fun setUnlockDuration(seconds: Int) {
        viewModelScope.launch {
            repository.saveSetting("unlock_duration", seconds.toString())
        }
    }

    fun setUnlockMode(mode: String) {
        viewModelScope.launch {
            repository.saveSetting("unlock_mode", mode)
        }
    }

    fun setFaceRegistered(registered: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("face_registered", registered.toString())
        }
    }

    fun setFaceUnlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("face_unlock_enabled", enabled.toString())
        }
    }
}
