with open("/app/applet/README.md", "r") as f:
    content = f.read()

ack_section = """## 🤝 Acknowledgements
JellyTune stands proud on the shoulders of giants:

*   **[Jellyfin](https://github.com/jellyfin/jellyfin)**: For their world-class, free, and open media server ecosystem.
*   **[Phonograph](https://github.com/kabouzeid/Phonograph)**: For pioneering the clean, timeless art of beautiful Material Design audio layout architecture.
*   **[Google AI Studio](https://ai.studio/build)**: For powering the advanced, context-aware AI Coding Agent that engineered this codebase.

**Note on Naming & Scope:**
While this Android application is named **JellyTune**, please note that it is an independent project and is not affiliated with the [JellyTunes](https://github.com/orainlabs/jellytunes) desktop client by OrainLabs. JellyTune (this app) is specifically designed as a lightweight, offline-first mobile client for Android."""

if "## 🤝 Acknowledgements" in content:
    content = content.replace(
        "## 🤝 Acknowledgements\nJellyTune stands proud on the shoulders of giants:\n\n*   **[Jellyfin](https://github.com/jellyfin/jellyfin)**: For their world-class, free, and open media server ecosystem.\n*   **[Phonograph](https://github.com/kabouzeid/Phonograph)**: For pioneering the clean, timeless art of beautiful Material Design audio layout architecture.\n*   **[Google AI Studio](https://ai.studio/build)**: For powering the advanced, context-aware AI Coding Agent that engineered this codebase.",
        ack_section
    )
    with open("/app/applet/README.md", "w") as f:
        f.write(content)
    print("README updated")
else:
    print("Could not find section")
