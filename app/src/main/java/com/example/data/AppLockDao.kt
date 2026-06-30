package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getBlockedAppsFlow(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getBlockedAppsList(): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<SettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)
}
