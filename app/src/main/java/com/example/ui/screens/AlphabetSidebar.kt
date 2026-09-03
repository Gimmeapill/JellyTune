package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AlphabetSidebar(
    modifier: Modifier = Modifier,
    onLetterSelect: (Char) -> Unit
) {
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var activeChar by remember { mutableStateOf<Char?>(null) }

    Column(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val viewportHeight = size.height
                        val charHeight = viewportHeight / alphabet.size
                        val index = (offset.y / charHeight).toInt().coerceIn(0, alphabet.size - 1)
                        val char = alphabet[index]
                        activeChar = char
                        onLetterSelect(char)
                    },
                    onDragEnd = { activeChar = null },
                    onDragCancel = { activeChar = null },
                    onDrag = { change, _ ->
                        val viewportHeight = size.height
                        val charHeight = viewportHeight / alphabet.size
                        val index = (change.position.y / charHeight).toInt().coerceIn(0, alphabet.size - 1)
                        val char = alphabet[index]
                        if (char != activeChar) {
                            activeChar = char
                            onLetterSelect(char)
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { char ->
            val isActive = activeChar == char
            Text(
                text = char.toString(),
                fontSize = if (isActive) 12.sp else 9.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
