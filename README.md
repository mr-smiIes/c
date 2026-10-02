# Ore Highlighter — Minecraft 26.3

Client-side Fabric prototype based on the Fabric Example Mod 26.3 template.

## Features
- Press K to open the menu.
- Toggle ore highlighting.
- Colored outlines for nearby ores.
- No server-side code or networking.

## Build
GitHub Actions runs the Gradle build and uploads the JAR as an artifact.

## Important
This project targets Minecraft 26.3 and Java 25. The Fabric 26.3 toolchain recommends Loom 1.17, Gradle 9.6.0, and Fabric Loader 0.19.5.

The Fabric API version is kept as a single property in `gradle.properties`. Set `fabric_api_version` to the current Fabric API release for Minecraft 26.3 before building if the current release value has changed.
