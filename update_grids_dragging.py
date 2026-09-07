import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# 1. Add isDragging state to all four views
content = content.replace(
"""    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    
    if (artists.isEmpty()) {""",
"""    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    var isDragging by remember { mutableStateOf(false) }
    
    if (artists.isEmpty()) {"""
)

content = content.replace(
"""    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    
    if (albums.isEmpty()) {""",
"""    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    var isDragging by remember { mutableStateOf(false) }
    
    if (albums.isEmpty()) {"""
)

content = content.replace(
"""    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    
    if (songs.isEmpty()) {""",
"""    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    var isDragging by remember { mutableStateOf(false) }
    
    if (songs.isEmpty()) {"""
)

# 2. Update scrollbar modifiers
content = content.replace(
"""                .drawGridScrollbar(gridState, scrollbarColor)""",
"""                .drawGridScrollbar(gridState, scrollbarColor, onDraggingChanged = { isDragging = it })"""
)

content = content.replace(
"""                .drawListScrollbar(listState, scrollbarColor)""",
"""                .drawListScrollbar(listState, scrollbarColor, onDraggingChanged = { isDragging = it })"""
)

# 3. Update overlay conditions
content = content.replace(
"""        if (isAlphabetical && gridState.isScrollInProgress) {""",
"""        if (isAlphabetical && (gridState.isScrollInProgress || isDragging)) {"""
)

content = content.replace(
"""        if (isAlphabetical && listState.isScrollInProgress) {""",
"""        if (isAlphabetical && (listState.isScrollInProgress || isDragging)) {"""
)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)

