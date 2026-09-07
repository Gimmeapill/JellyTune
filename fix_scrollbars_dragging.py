import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# Update drawListScrollbar signature
old_list_sig = """fun Modifier.drawListScrollbar(
    state: LazyListState,
    color: Color
): Modifier = this.composed {"""
new_list_sig = """fun Modifier.drawListScrollbar(
    state: LazyListState,
    color: Color,
    onDraggingChanged: ((Boolean) -> Unit)? = null
): Modifier = this.composed {"""
content = content.replace(old_list_sig, new_list_sig)

# Find where isDragging is updated for list and add callback
content = content.replace("isDragging = true\n                            down.consume()", "isDragging = true\n                            onDraggingChanged?.invoke(true)\n                            down.consume()")
content = content.replace("isDragging = false\n                        }", "isDragging = false\n                            onDraggingChanged?.invoke(false)\n                        }")

# Update drawGridScrollbar signature
old_grid_sig = """fun Modifier.drawGridScrollbar(
    state: LazyGridState,
    color: Color
): Modifier = this.composed {"""
new_grid_sig = """fun Modifier.drawGridScrollbar(
    state: LazyGridState,
    color: Color,
    onDraggingChanged: ((Boolean) -> Unit)? = null
): Modifier = this.composed {"""
content = content.replace(old_grid_sig, new_grid_sig)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
