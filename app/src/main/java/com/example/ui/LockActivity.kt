package com.example.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.MaterialYouKeypadButton
import com.example.ui.components.MaterialYouPinDot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PatternLockView
import com.example.ui.components.BiometricPromptHelper
import kotlinx.coroutines.delay
import androidx.core.graphics.drawable.toBitmap
import com.example.AppLockerApplication
import com.example.service.AppLockManager
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class LockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra("EXTRA_PACKAGE_NAME") ?: ""
        if (targetPackage.isEmpty()) {
            goToHomeScreen()
            return
        }

        setContent {
            MyApplicationTheme {
                LockScreen(
                    packageName = targetPackage,
                    onUnlockSuccess = {
                        AppLockManager.unlockPackage(targetPackage)
                        finish()
                    },
                    onCancel = {
                        goToHomeScreen()
                    }
                )
            }
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun LockScreen(
    packageName: String,
    onUnlockSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var appLabel by remember { mutableStateOf(packageName) }
    var appIconDrawable by remember { mutableStateOf<Drawable?>(null) }
    var storedPin by remember { mutableStateOf<String?>(null) }
    var storedPattern by remember { mutableStateOf<String?>(null) }
    var lockType by remember { mutableStateOf("pin") }
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Введите PIN-код для разблокировки") }

    // Intercept physical back button to go back to home screen instead of the target app
    BackHandler {
        onCancel()
    }

    // Load App Details & Stored PIN
    LaunchedEffect(packageName) {
        val pm = context.packageManager
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appLabel = pm.getApplicationLabel(appInfo).toString()
            appIconDrawable = pm.getApplicationIcon(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            appLabel = packageName
        }

        // Fetch configured PIN from Room DB
        val repo = (context.applicationContext as AppLockerApplication).repository
        storedPin = repo.getSetting("pin")
        storedPattern = repo.getSetting("pattern")
        lockType = repo.getSetting("lock_type") ?: "pin"
        
        statusText = if (lockType == "pattern") "Нарисуйте графический ключ для разблокировки" else "Введите PIN-код для разблокировки"

        // If there's no PIN or pattern configured, bypass lock
        val hasProtection = if (lockType == "pattern") !storedPattern.isNullOrEmpty() else !storedPin.isNullOrEmpty()
        if (!hasProtection) {
            onUnlockSuccess()
        }
    }

    // Process PIN updates
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == storedPin) {
                isError = false
                onUnlockSuccess()
            } else {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                isError = true
                enteredPin = ""
                statusText = "Неверный PIN! Попробуйте еще раз"
            }
        } else if (enteredPin.length > 0) {
            isError = false
            statusText = "Введите PIN-код для разблокировки"
        }
    }

    LaunchedEffect(isError) {
        if (isError) {
            delay(1500)
            isError = false
            statusText = if (lockType == "pattern") "Нарисуйте графический ключ для разблокировки" else "Введите PIN-код для разблокировки"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with App Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    appIconDrawable?.let { drawable ->
                        Image(
                            bitmap = drawable.toBitmap().asImageBitmap(),
                            contentDescription = appLabel,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } ?: Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Заблокировано",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // PIN/Pattern Indicator & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (lockType == "pattern") {
                    // Draw a beautiful geometric pattern canvas!
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternLockView(
                            modifier = Modifier.size(280.dp),
                            isError = isError,
                            onPatternCompleted = { pattern ->
                                val patternStr = pattern.joinToString(separator = "")
                                if (patternStr == storedPattern) {
                                    isError = false
                                    onUnlockSuccess()
                                } else {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    isError = true
                                    statusText = "Неверный графический ключ!"
                                }
                            }
                        )
                    }

                    // Exit button for pattern screen
                    Button(
                        onClick = { onCancel() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Выйти")
                    }
                } else {
                    // 4-Dot Indicator Row for PIN
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

            // Numeric Keypad (only for PIN mode)
            if (lockType != "pattern") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "CANCEL", "0", "DELETE")
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.width(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(keys) { key ->
                            when (key) {
                                "CANCEL" -> {
                                    MaterialYouKeypadButton(
                                        key = key,
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onCancel()
                                        },
                                        modifier = Modifier.testTag("key_cancel")
                                    ) {
                                        Text(
                                            text = "Выйти",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                "DELETE" -> {
                                    MaterialYouKeypadButton(
                                        key = key,
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        },
                                        modifier = Modifier.testTag("key_delete")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                else -> {
                                    MaterialYouKeypadButton(
                                        key = key,
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                            }
                                        },
                                        modifier = Modifier.testTag("key_$key")
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
