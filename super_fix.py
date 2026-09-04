import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# 1. Remove all `if (isAlphabetical && ...isScrollInProgress) { AlphabetOverlay(...) }` everywhere!
content = re.sub(r'\s*if \(isAlphabetical && (?:gridState|listState)\.isScrollInProgress\) \{\s*AlphabetOverlay\([\s\S]*?\)\s*\}', '', content)

# 2. Let's add them back to the exact right places.
# For ExploreAlbumArtistsGrid
aa_pattern = r'(\s*)(items\(artists\) \{\s*artist ->[\s\S]*?\}\s*\n\s*\})'
aa_replacement = r'\1\2\1if (isAlphabetical && gridState.isScrollInProgress) {\1    AlphabetOverlay(firstVisibleItemIndex = gridState.firstVisibleItemIndex, items = artists)\1}'
content = re.sub(r'(@Composable\s*fun ExploreAlbumArtistsGrid[\s\S]*?)(\s*\n\s*\}(?=\s*\n\s*@Composable\s*fun ExploreArtistsGrid))', r'\1\n        if (isAlphabetical && gridState.isScrollInProgress) {\n            AlphabetOverlay(gridState.firstVisibleItemIndex, artists)\n        }\2', content)

content = re.sub(r'(@Composable\s*fun ExploreArtistsGrid[\s\S]*?)(\s*\n\s*\}(?=\s*\n\s*@Composable\s*fun ExploreAlbumsGrid))', r'\1\n        if (isAlphabetical && gridState.isScrollInProgress) {\n            AlphabetOverlay(gridState.firstVisibleItemIndex, artists)\n        }\2', content)

content = re.sub(r'(@Composable\s*fun ExploreAlbumsGrid[\s\S]*?)(\s*\n\s*\}(?=\s*\n\s*@Composable\s*fun AlbumActionDialog))', r'\1\n        if (isAlphabetical && gridState.isScrollInProgress) {\n            AlphabetOverlay(gridState.firstVisibleItemIndex, albums)\n        }\2', content)

content = re.sub(r'(@Composable\s*fun ExploreSongsList[\s\S]*?)(\s*\n\s*\}(?=\s*\n\s*@Composable\s*fun AlbumCard))', r'\1\n        if (isAlphabetical && listState.isScrollInProgress) {\n            AlphabetOverlay(listState.firstVisibleItemIndex, songs)\n        }\2', content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
