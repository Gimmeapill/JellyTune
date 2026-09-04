import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# For Album Artists: items = artists
# Wait, let's just replace all `if (isAlphabetical && ...isScrollInProgress) { AlphabetOverlay(...) }` 
# I will use regex to find the components and replace their specific overlay

def replace_overlay(component_name, state_name, items_name, code):
    pattern = r'@Composable\s*fun ' + component_name + r'[\s\S]*?(?=}\n}\n\n@Composable)'
    
    match = re.search(pattern, code)
    if match:
        block = match.group(0)
        # remove existing overlays in this block
        block = re.sub(r'\s*if \(isAlphabetical && (?:gridState|listState)\.isScrollInProgress\) \{\s*AlphabetOverlay\([\s\S]*?\)\s*\}', '', block)
        
        # add the correct one right before the end
        new_overlay = f"\n        if (isAlphabetical && {state_name}.isScrollInProgress) {{\n            AlphabetOverlay({state_name}.firstVisibleItemIndex, {items_name})\n        }}"
        block += new_overlay
        
        return code.replace(match.group(0), block)
    return code

content = replace_overlay("ExploreAlbumArtistsGrid", "gridState", "artists", content)
content = replace_overlay("ExploreArtistsGrid", "gridState", "artists", content)
content = replace_overlay("ExploreAlbumsGrid", "gridState", "albums", content)
# Special case for SongsList because it might end differently, let's check
pattern_songs = r'@Composable\s*fun ExploreSongsList[\s\S]*?(?=}\n}\n\n@Composable)'
match_songs = re.search(pattern_songs, content)
if match_songs:
    block = match_songs.group(0)
    block = re.sub(r'\s*if \(isAlphabetical && (?:gridState|listState)\.isScrollInProgress\) \{\s*AlphabetOverlay\([\s\S]*?\)\s*\}', '', block)
    block += "\n        if (isAlphabetical && listState.isScrollInProgress) {\n            AlphabetOverlay(listState.firstVisibleItemIndex, songs)\n        }"
    content = content.replace(match_songs.group(0), block)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)

