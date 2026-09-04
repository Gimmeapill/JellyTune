import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

bad_when = """        when (criteria) {
            SortCriteria.ALPHABETICAL -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
            SortCriteria.DATE_ADDED -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.dateCreated } else filtered.sortedByDescending { it.dateCreated }
            SortCriteria.RELEASE_YEAR -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.productionYear } else filtered.sortedByDescending { it.productionYear }
        }"""

good_when = """        when (criteria) {
            SortCriteria.ALPHABETICAL -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
            SortCriteria.DATE_ADDED -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.dateCreated } else filtered.sortedByDescending { it.dateCreated }
            SortCriteria.DATE -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.productionYear } else filtered.sortedByDescending { it.productionYear }
            else -> filtered
        }"""
content = content.replace(bad_when, good_when)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
