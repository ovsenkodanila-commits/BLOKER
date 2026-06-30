package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.AppLockerApplication
import com.example.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var blockedPackages = setOf<String>()
    private var isLockerEnabled = true
    private var configuredPin: String? = null
    private var lastActivePackage: String? = null
    private var unlockMode = "until_closed" // "until_closed" or "timer"

    companion object {
        private const val TAG = "AppLockAccessibility"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        AppLockManager.isAccessibilityActive = true

        val repository = (applicationContext as AppLockerApplication).repository

        // Keep in-memory caches synchronized with Room Database
        serviceScope.launch {
            combine(
                repository.blockedAppsFlow,
                repository.getSettingFlow("pin"),
                repository.getSettingFlow("locker_enabled")
            ) { blockedApps, pin, enabled ->
                Triple(blockedApps, pin, enabled)
            }.collect { (blockedApps, pin, enabled) ->
                blockedPackages = blockedApps.map { it.packageName }.toSet()
                configuredPin = pin
                isLockerEnabled = enabled?.toBoolean() ?: true
                
                Log.d(TAG, "Cache loaded. Locked apps: ${blockedPackages.size}, Locker active: $isLockerEnabled, Has PIN: ${!configuredPin.isNullOrEmpty()}")
            }
        }
        
        // Also load the unlock duration setting
        serviceScope.launch {
            repository.getSettingFlow("unlock_duration").collect { durationStr ->
                val durationSec = durationStr?.toIntOrNull() ?: 15
                AppLockManager.setUnlockDuration(durationSec)
            }
        }

        // Load the unlock mode setting (default to until_closed)
        serviceScope.launch {
            repository.getSettingFlow("unlock_mode").collect { mode ->
                unlockMode = mode ?: "until_closed"
                Log.d(TAG, "Unlock mode loaded: $unlockMode")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isLockerEnabled || configuredPin.isNullOrEmpty()) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Skip our own package to prevent locking ourselves out during PIN entry
            if (packageName == applicationContext.packageName) {
                return
            }

            // Handle switching away from a blocked package to another package in "until_closed" mode
            val prevActive = lastActivePackage
            if (prevActive != null && prevActive != packageName) {
                if (unlockMode == "until_closed" && blockedPackages.contains(prevActive)) {
                    AppLockManager.lockPackage(prevActive)
                    Log.d(TAG, "User exited $prevActive, relocking it immediately.")
                }
            }
            lastActivePackage = packageName

            // Check if this package is a blocked app and is not currently temporarily unlocked
            if (blockedPackages.contains(packageName)) {
                val isUnlocked = AppLockManager.isPackageUnlocked(
                    packageName = packageName,
                    ignoreTimer = (unlockMode == "until_closed")
                )
                if (!isUnlocked) {
                    Log.d(TAG, "Intercepted launch of blocked app: $packageName. Showing PIN lock screen.")
                    launchLockScreen(packageName)
                }
            }
        }
    }

    private fun launchLockScreen(packageName: String) {
        val intent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_PACKAGE_NAME", packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLockManager.isAccessibilityActive = false
        serviceScope.cancel()
        Log.d(TAG, "Accessibility Service Destroyed")
    }
}
