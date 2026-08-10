package com.example.householdapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AvatarPalette = listOf(
    Color(0xFF0B6E6E),
    Color(0xFF7A5B00),
    Color(0xFF964F00),
    Color(0xFF3D5A80),
    Color(0xFF6D28D9),
    Color(0xFFB33B5E)
)

@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderColor: Color = Color.White.copy(alpha = 0.5f)
) {
    val color = AvatarPalette[(name.hashCode() and Int.MAX_VALUE) % AvatarPalette.size]
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
    ) {
        Text(
            text = name.trim().take(2).uppercase().ifBlank { "?" },
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
