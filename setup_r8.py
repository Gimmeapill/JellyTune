import re

file_path = '/app/applet/app/build.gradle.kts'
with open(file_path, 'r') as f:
    content = f.read()

# Enable minification and resource shrinking
old_release = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }"""
    
new_release = """    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }"""

content = content.replace(old_release, new_release)

# Bump version
def replace_version_code(match):
    current = int(match.group(1))
    return f'versionCode = {current + 1}'

def replace_version_name(match):
    current = float(match.group(1))
    return f'versionName = "{current + 1.0}"'

content = re.sub(r'versionCode\s*=\s*(\d+)', replace_version_code, content)
content = re.sub(r'versionName\s*=\s*"([\d\.]+)"', replace_version_name, content)

with open(file_path, 'w') as f:
    f.write(content)

# Add Proguard rule
with open('/app/applet/app/proguard-rules.pro', 'a') as f:
    f.write("\n# Keep models for Serialization/Deserialization\n")
    f.write("-keep class com.example.data.jellyfin.** { *; }\n")
    f.write("-keep class com.example.data.database.** { *; }\n")
    f.write("-keepattributes *Annotation*\n")
    f.write("-keepattributes Signature\n")
    f.write("-keepattributes InnerClasses\n")

print("R8 setup complete")
