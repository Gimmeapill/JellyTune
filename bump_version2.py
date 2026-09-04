with open("/app/applet/app/build.gradle.kts", "r") as f:
    gradle = f.read()
gradle = gradle.replace("versionCode = 84", "versionCode = 85").replace('versionName = "84.0"', 'versionName = "85.0"')
with open("/app/applet/app/build.gradle.kts", "w") as f:
    f.write(gradle)
