package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PatternEnrollmentDialog(
    onDismiss: () -> Unit,
    onEnrollSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var step by remember { mutableStateOf(1) } // 1 = First draw, 2 = Confirm draw
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isError by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Нарисуйте графический ключ\n(соедините не менее 4 точек)") }

    val handlePatternCompleted = { pattern: List<Int> ->
        if (pattern.size < 4) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isError = true
            statusText = "Слишком короткий рисунок.\nСоедините не менее 4 точек."
        } else {
            if (step == 1) {
                firstPattern = pattern
                isError = false
                step = 2
                statusText = "Повторите графический ключ\nдля подтверждения"
            } else {
                if (pattern == firstPattern) {
                    // Match! Convert to string representation like "0123"
                    val patternStr = pattern.joinToString(separator = "")
                    onEnrollSuccess(patternStr)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isError = true
                    statusText = "Рисунки не совпадают.\nПопробуйте еще раз."
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Настройка ключа",
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

                // Dynamic Status & Visual
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (isError) MaterialTheme.colorScheme.errorContainer 
                                        else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Pattern Icon",
                            tint = if (isError) MaterialTheme.colorScheme.onErrorContainer 
                                   else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Pattern Drawing Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PatternLockView(
                        modifier = Modifier.size(300.dp),
                        isError = isError,
                        onPatternCompleted = { pattern ->
                            handlePatternCompleted(pattern)
                        }
                    )
                }

                // Footer control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (step == 2) {
                                step = 1
                                firstPattern = emptyList()
                                isError = false
                                statusText = "Нарисуйте графический ключ\n(соедините не менее 4 точек)"
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Text(
                            text = if (step == 2) "Сбросить" else "Отмена",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isError) {
                        Button(
                            onClick = {
                                isError = false
                                if (step == 2) {
                                    statusText = "Повторите графический ключ\nдля подтверждения"
                                } else {
                                    statusText = "Нарисуйте графический ключ\n(соедините не менее 4 точек)"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Повторить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternVerificationDialog(
    onDismiss: () -> Unit,
    correctPattern: String,
    onVerificationSuccess: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isError by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Нарисуйте графический ключ\nдля подтверждения действия") }

    LaunchedEffect(isError) {
        if (isError) {
            delay(1500)
            isError = false
            statusText = "Нарисуйте графический ключ\nдля подтверждения действия"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
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

                // Middle Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (isError) MaterialTheme.colorScheme.errorContainer 
                                        else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = if (isError) MaterialTheme.colorScheme.onErrorContainer 
                                   else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Drawing Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PatternLockView(
                        modifier = Modifier.size(300.dp),
                        isError = isError,
                        onPatternCompleted = { pattern ->
                            val patternStr = pattern.joinToString(separator = "")
                            if (patternStr == correctPattern) {
                                onVerificationSuccess()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isError = true
                                statusText = "Неверный графический ключ.\nПопробуйте еще раз."
                            }
                        }
                    )
                }

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Отмена", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
