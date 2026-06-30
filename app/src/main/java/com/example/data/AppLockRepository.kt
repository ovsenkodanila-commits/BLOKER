package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppLockRepository(private val dao: AppLockDao) {
    val blockedAppsFlow: Flow<List<BlockedAppEntity>> = dao.getBlockedAppsFlow()

    suspend fun getBlockedAppsList(): List<BlockedAppEntity> = dao.getBlockedAppsList()

    suspend fun isAppBlocked(packageName: String): Boolean {
        return dao.getBlockedAppsList().any { it.packageName == packageName }
    }

    suspend fun blockApp(packageName: String, appName: String) {
        dao.insertBlockedApp(BlockedAppEntity(packageName, appName))
    }

    suspend fun unblockApp(packageName: String) {
        dao.deleteBlockedApp(packageName)
    }

    fun getSettingFlow(key: String): Flow<String?> {
        return dao.getSettingFlow(key).map { it?.value }
    }

    suspend fun getSetting(key: String): String? {
        return dao.getSetting(key)?.value
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(SettingEntity(key, value))
    }
}
