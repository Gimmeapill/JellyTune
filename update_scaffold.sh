sed -i 's/modifier = modifier.fillMaxSize(),/modifier = modifier.fillMaxSize().navigationBarsPadding(),/g' /app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt
sed -i 's/modifier = Modifier.navigationBarsPadding()/modifier = Modifier/g' /app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt
