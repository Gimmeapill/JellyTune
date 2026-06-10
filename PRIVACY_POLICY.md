# Privacy Policy for JellyTune

**Last Updated: June 10, 2026**

This Privacy Policy describes how **JellyTune** (referred to as "the App", "we", "us", or "our") handles, stores, and processes information when you use our mobile application. 

JellyTune is designed as an independent client application built to connect directly to user-hosted and user-configured **Jellyfin** media servers. We are committed to protecting your privacy and ensuring you have absolute control over your personal data.

---

## 1. Executive Summary: Zero-Data Collection & Privacy-First
* **No Third-Party Analytics:** We do not include any tracking software, telemetry, cookies, or analytics suites (such as Google Analytics or Firebase Analytics).
* **No Telemetry or Crash Reporting:** The app does not transmit diagnostics, logs, or crash data to us or any external servers. 
* **No Advertising:** The app is completely free of advertisements and has no promotional code integrations.
* **Direct Connections Only:** The app operates solely as a direct bridge between your Android device and the self-hosted Jellyfin server you explicitly specify. No intermediary servers, proxies, or cloud platforms are used.

---

## 2. Information Collected and Stored on Your Device
To provide music streaming services, JellyTune must store certain critical configuration files and operational data. All of this data is kept **strictly on-device** in secure, private sandboxed application storage:

1. **Server Credentials & Tokens:** 
   * When you log in, the server URL, username, user ID, and server-generated Authentication Token are stored locally in a secure dynamic database (Android SQLite via Room).
   * **Passwords are never stored in plain text.** JellyTune uses the standard Jellyfin authentication flow to request an auth token from your server, storing only the secure token for subsequent requests.
2. **Track Cache & Audio Metadata:**
   * Audio files and track metadata (such as song names, album art, and artist info) are cached temporarily in your device's local cache directory to support offline playback and save cellular data. 
   * Cache limits and storage allocations are fully customizable and controllable directly from the settings menu.
3. **Local Playback States & Database Record Lists:**
   * Playback queues, local playback states (e.g., song progress), and your list of favorited entries are stored locally within the private on-device Room database and Android `SharedPreferences`.

---

## 3. Data Transmission and Network Traffic
JellyTune only initiates outbound network traffic to the specific **Server URL** you enter inside the Login screen. 

* **To Your Jellyfin Server:** Outbound HTTP/HTTPS requests are sent directly to your server to authenticate, fetch libraries, query playlists, fetch track streams, and synchronize favorite flags.
* **No Third-Party APIs:** We do not send, transmit, sell, or rent any information (including metadata, filenames, server addresses, or account structures) to any third parties under any circumstances.

---

## 4. Android Device Permissions Required
To deliver functional audio playback and streaming, JellyTune requests the following platform permissions on Android:

* **`android.permission.INTERNET`**: Required to communicate with your self-hosted Jellyfin server to stream music and load library metadata.
* **`android.permission.FOREGROUND_SERVICE`** / **`android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`**: Required to keep the audio engine alive for seamless background playback when you lock your device or exit the app screen.
* **`android.permission.ACCESS_NETWORK_STATE`**: Used to determine connection availability (Wi-Fi vs. Mobile Data) to prevent unintended carrier data charges.

---

## 5. Security of Your Data
All authentication tokens, cache repositories, and local database entries are hosted inside Android’s standardized secure sandboxed filesystem. This prevents other companion apps or unauthorized software on your phone from accessing your credentials or personal servers.

* **Transport Security:** We strongly encourage configuring your personal Jellyfin server with **SSL/TLS (HTTPS)** to ensure all music streaming and credentials are fully encrypted in transit.

---

## 6. Your Rights and Data Erasure
Because all data is stored strictly on your device, you have absolute ownership over your personal data:

* **Clear Cache:** You can purge all cached audio tracks from your device at any time inside the app's *Settings -> Cache* tab.
* **Data Erasure (Logout):** Logging out of the server or uninstalling JellyTune immediately and permanently deletes all stored server URLs, authentication tokens, credentials, caches, queues, and metadata records from your internal storage.

---

## 7. Changes to This Privacy Policy
We may update our Privacy Policy from time to time to accommodate future feature expansions or security optimizations. Any changes will be updated on this page with a revised "Last Updated" timestamp. We recommend reviewing this policy occasionally for updates.

---

## 8. Contact Information
If you have any questions or concerns regarding this Privacy Policy, your local data handling practices, or open-source compliance of JellyTune, you may contact the developers directly at:

* **Email:** gimmeapill@gmail.com
