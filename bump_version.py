with open("/app/applet/app/build.gradle.kts", "r") as f:
    gradle = f.read()
gradle = gradle.replace("versionCode = 82", "versionCode = 83").replace('versionName = "82.0"', 'versionName = "83.0"')
with open("/app/applet/app/build.gradle.kts", "w") as f:
    f.write(gradle)
