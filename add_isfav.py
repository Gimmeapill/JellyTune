import re

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "r") as f:
    content = f.read()

helper = """
    suspend fun isFavoriteLocally(songId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(songId)
    }
"""
if "isFavoriteLocally" not in content:
    content = content.replace("    suspend fun toggleFavorite(", helper + "    suspend fun toggleFavorite(")

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "w") as f:
    f.write(content)
