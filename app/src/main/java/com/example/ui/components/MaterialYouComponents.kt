package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getMaterialYouShapeForKey(key: String): Shape {
    return when (key) {
        "1" -> RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
        "2" -> RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
        "3" -> RoundedCornerShape(topStart = 28.dp, topEnd = 6.dp, bottomEnd = 28.dp, bottomStart = 6.dp)
        "4" -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 28.dp)
        "5" -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 6.dp, bottomStart = 24.dp)
        "6" -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp)
        "7" -> RoundedCornerShape(topStart = 6.dp, topEnd = 28.dp, bottomEnd = 6.dp, bottomStart = 28.dp)
        "8" -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
        "9" -> RoundedCornerShape(topStart = 24.dp, topEnd = 6.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
        "0" -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
        "DELETE" -> RoundedCornerShape(topStart = 12.dp, topEnd = 24.dp, bottomEnd = 12.dp, bottomStart = 24.dp)
        else -> RoundedCornerShape(20.dp)
    }
}

@Composable
fun MaterialYouKeypadButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = getMaterialYouShapeForKey(key)
    
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun MaterialYouPinDot(
    isFilled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val size by animateDpAsState(
        targetValue = if (isFilled) 20.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    val color by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFilled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = 150)
    )
    
    // Circle when empty, cute soft rounded rectangle (squircle-ish) when filled
    val cornerRadius by animateDpAsState(
        targetValue = if (isFilled) 6.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = color,
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}
