import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

def inject_is_dragging(func_name):
    global content
    pattern = r"(fun " + func_name + r"\(viewModel: JellyTuneViewModel\) \{.*?val isAlphabetical = sortCriteria == SortCriteria\.ALPHABETICAL)(.*?if \(\w+\.isEmpty\(\)\) \{)"
    
    match = re.search(pattern, content, flags=re.DOTALL)
    if match:
        if "var isDragging by remember { mutableStateOf(false) }" not in match.group(2):
            new_str = match.group(1) + "\n    var isDragging by remember { mutableStateOf(false) }" + match.group(2)
            content = content[:match.start()] + new_str + content[match.end():]

inject_is_dragging("ExploreAlbumsGrid")
inject_is_dragging("ExploreArtistsGrid")
inject_is_dragging("ExploreAlbumArtistsGrid")
inject_is_dragging("ExploreSongsList")

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
