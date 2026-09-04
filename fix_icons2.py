import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Icons.AutoMirrored.Filled.QueueMusic", "Icons.Default.QueueMusic")
content = content.replace("Modifier\n                    .fillMaxWidth()\n                    .aspectRatio(1f)", "Modifier\n                    .fillMaxWidth()\n                    .height(150.dp)")

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
