# Agent Custom Instructions

- **Automatic Version Bumping**: Always bump up the version number (`versionCode` and `versionName`) in `app/build.gradle.kts` after making any functional changes to the application. This is because the user tests the project using the Google Play Console and must repeatedly publish new versions to test changes. Ensure the new version number is strictly greater than the previous one.
