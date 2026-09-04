sed -i '/private val _wifiOnlyMode/,/val wifiOnlyMode/d' /app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt
sed -i '/fun setWifiOnlyMode/,/}/d' /app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt
sed -i 's/_wifiOnlyMode,//g' /app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt
sed -i 's/wifiOnly,//g' /app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt
sed -i 's/offlineSelected || (wifiOnly && !isWifi)/offlineSelected/g' /app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt
