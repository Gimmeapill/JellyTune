import re

file_path = '/app/applet/app/build.gradle.kts'
with open(file_path, 'r') as f:
    content = f.read()

old_release = """    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }"""
    
new_release = """    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      ndk {
        debugSymbolLevel = "FULL"
      }
    }"""

content = content.replace(old_release, new_release)

with open(file_path, 'w') as f:
    f.write(content)

print("NDK debug symbols enabled")
