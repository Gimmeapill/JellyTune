import re

file_path = '/app/applet/app/build.gradle.kts'
with open(file_path, 'r') as f:
    content = f.read()

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
