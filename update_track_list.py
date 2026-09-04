import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# Update TrackListItem signature
content = re.sub(
    r"isFavorite: Boolean,\n\s*onClick: \(\) -> Unit,",
    "isFavorite: Boolean,\n    onFavoriteToggle: () -> Unit,\n    onClick: () -> Unit,",
    content
)

# Update the call in ExploreSongsList
content = re.sub(
    r"isFavorite = isFav,\n\s*onClick =",
    "isFavorite = isFav,\n                    onFavoriteToggle = { viewModel.toggleFavorite(song) },\n                    onClick =",
    content
)

# Update the heart icon in TrackListItem
old_heart = r"""if \(isFavorite\) \{\s*Icon\(\s*Icons\.Default\.Favorite,\s*contentDescription = "Favorite",\s*tint = MaterialTheme\.colorScheme\.primary,\s*modifier = Modifier\.padding\(end = 8\.dp\)\s*\)\s*\}"""
new_heart = """IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(end = 8.dp)
            )
        }"""
content = re.sub(old_heart, new_heart, content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
