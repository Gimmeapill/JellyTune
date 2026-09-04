import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# The garbage usually starts with ` else {\n                        albums.indexOfFirst` or artists or songs
garbage_pattern = r'\s*else \{\s*\w+\.indexOfFirst \{\s*!it\.name\.firstOrNull\(\)\.let[\s\S]*?\}\s*\)\s*\}'
content = re.sub(garbage_pattern, '', content)

garbage2 = r'\s*else \{\s*\w+\.indexOfFirst \{\s*it\.name\.firstOrNull\(\)\?\.uppercaseChar\(\) == char\s*\}\s*\}\s*if \(index != -1\) \{\s*coroutineScope\.launch \{ (?:gridState|listState)\.scrollToItem\(index\) \}\s*\}\s*\}\s*\)\s*\}'
content = re.sub(garbage2, '', content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
