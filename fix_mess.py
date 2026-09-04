import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# 1. Remove the bad AlphabetOverlay in SortSelectionBar
content = re.sub(r'        if \(isAlphabetical && listState\.isScrollInProgress\) \{\s*AlphabetOverlay\(firstVisibleItemIndex = listState\.firstVisibleItemIndex, items = songs\)\s*\}\s*\}\s*\}\s*@Composable\s*fun EmptyStateBlock', '        }\n    }\n}\n\n@Composable\nfun EmptyStateBlock', content)

# 2. Fix the error in ExploreSongsList `items = albums` to `items = songs`
# I already did `sed` but let's be sure
content = content.replace("AlphabetOverlay(firstVisibleItemIndex = gridState.firstVisibleItemIndex, items = albums)", "AlphabetOverlay(firstVisibleItemIndex = listState.firstVisibleItemIndex, items = songs)")

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
