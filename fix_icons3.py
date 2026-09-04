import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Icons.AutoMirrored.Filled.QueueMusic", "Icons.Default.QueueMusic")
content = content.replace("Icons.Default.Done", "Icons.Default.Check")
content = content.replace("Icons.Default.DownloadDone", "Icons.Default.Check")

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
