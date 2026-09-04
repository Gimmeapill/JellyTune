import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# Just use Icons.Default.PlayArrow for QueueMusic if it doesn't exist, or Icons.Default.Add
content = content.replace("Icons.Default.QueueMusic", "Icons.Default.Add")
content = content.replace("Icons.AutoMirrored.Filled.QueueMusic", "Icons.Default.Add")

content = content.replace("Icons.Default.Check", "Icons.Default.PlayArrow")
content = content.replace("Icons.Default.DownloadDone", "Icons.Default.PlayArrow")
content = content.replace("Icons.Default.Done", "Icons.Default.PlayArrow")

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)

with open("/app/applet/app/build.gradle.kts", "r") as f:
    gradle = f.read()
gradle = gradle.replace("versionCode = 80", "versionCode = 81").replace('versionName = "80.0"', 'versionName = "81.0"')
with open("/app/applet/app/build.gradle.kts", "w") as f:
    f.write(gradle)
