import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

replacement = """        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val down = event.changes.firstOrNull { it.pressed }
                    if (down != null && !isDragging) {
                        if (down.position.x >= size.width - 48.dp.toPx()) {
                            isDragging = true
                            down.consume()
                        }
                    }
                    if (isDragging) {
                        event.changes.forEach { it.consume() }
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            val viewportHeight = size.height
                            val totalItemsCount = state.layoutInfo.totalItemsCount
                            if (totalItemsCount > 0 && viewportHeight > 0) {
                                val touchY = change.position.y
                                val progress = (touchY / viewportHeight).coerceIn(0f, 1f)
                                val targetIndex = (progress * totalItemsCount).toInt().coerceIn(0, totalItemsCount - 1)
                                coroutineScope.launch {
                                    state.scrollToItem(targetIndex)
                                }
                            }
                        }
                        if (event.changes.all { !it.pressed }) {
                            isDragging = false
                        }
                    }
                }
            }
        }
}"""

list_pattern = r'\.pointerInput\(state\)\s*\{\s*detectDragGestures\([\s\S]*?\}\s*\)\s*\}\s*\}'
content = re.sub(list_pattern, replacement, content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
