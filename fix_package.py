with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    lines = f.readlines()

if lines[0].startswith("import"):
    lines.pop(0)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    for line in lines:
        if line.startswith("package"):
            f.write(line)
            f.write("import androidx.compose.material.icons.filled.Add\n")
        else:
            f.write(line)
