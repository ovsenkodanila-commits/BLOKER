package com.example.service

import java.util.concurrent.ConcurrentHashMap

object AppLockManager {
    // Stores package name to the epoch millisecond of when it was successfully unlocked.
    private val unlockedPackages = ConcurrentHashMap<String, Long>()
    
    // Default lock grace period duration in milliseconds.
    private var unlockDurationMs: Long = 15 * 1000 // 15 seconds grace period
    
    // Globally holds active state
    var isAccessibilityActive: Boolean = false
    
    fun unlockPackage(packageName: String) {
        unlockedPackages[packageName] = System.currentTimeMillis()
    }
    
    fun isPackageUnlocked(packageName: String, ignoreTimer: Boolean = false): Boolean {
        val unlockTime = unlockedPackages[packageName] ?: return false
        if (ignoreTimer) {
            return true
        }
        if (System.currentTimeMillis() - unlockTime < unlockDurationMs) {
            return true
        }
        unlockedPackages.remove(packageName)
        return false
    }

    fun lockPackage(packageName: String) {
        unlockedPackages.remove(packageName)
    }
    
    fun clearAllUnlocks() {
        unlockedPackages.clear()
    }

    fun setUnlockDuration(durationSeconds: Int) {
        unlockDurationMs = durationSeconds * 1000L
    }

    fun getUnlockDurationSeconds(): Int {
        return (unlockDurationMs / 1000).toInt()
    }
}
