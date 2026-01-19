<h1 align="center">DeepCraft 1.0: Central Plugin & Systems Repository</h1>

<p align="center">
  <img src="https://img.shields.io/github/repo-size/SoldadoHumano/DeepCraft-1.0" alt="Repo Size">
  <img src="https://img.shields.io/github/languages/top/SoldadoHumano/DeepCraft-1.0?color=blue" alt="Languages">
  <img src="https://img.shields.io/github/commit-activity/y/SoldadoHumano/DeepCraft-1.0" alt="Commits">
  <img src="https://img.shields.io/github/stars/SoldadoHumano/DeepCraft-1.0" alt="Stars">
  <img src="https://img.shields.io/github/license/SoldadoHumano/DeepCraft-1.0" alt="License">
</p>

---

> [!IMPORTANT]
> **This repository has been archived.**
> To improve project structure and management, all development has moved to the official **DeepCraft Organization**.
>
> 📂 **Access the new home here: [DeepCraft Organization](https://github.com/Deep-Craft)**

---

## 📄 Project Overview

This repository operates as the official **Central Plugin & Systems Hub** for the **DeepCraft Minecraft server**. It centralizes all custom-developed software, plugins, and server modules required for proprietary server functions.

**DeepCraft 1.0** contains the source code for the current, production-ready software components, facilitating version control and deployment.

---

## 🎯 Repository Objectives

**Technical Portfolio:** Demonstrates advanced backend development skills, server architecture expertise, and module integration using technologies like Java/Kotlin and **LuckPerms**.

**Deployment Standard:** Serves as the authoritative source for the server team, guaranteeing access to stable and tested deployment artifacts (JAR files).

**Open Source Reference:** Provides a public reference for the development and structure of scalable, modular Minecraft server systems under the GPLv3 license.

---

## 🛠️ Main Technologies

| Component          | Description                                     |
| ------------------ | ----------------------------------------------- |
| Base Platform| Paper (Main servers) and Velocity (Proxy)|
| Languages| Java, YAML (Configuration)|
| Database| MariaDB (Optional, depending on plugin) |
| Build Tools| Gradle|
| Java Version | 21 & 17 (provided by Azul Systems)|
| Permissions System | LuckPerms (Centralized permission management)|

---

## 📊 Component Status

| Component Name     | Latest Version | Status  |
| ------------------ | -------------- | ------- |
|vLobby|v3.2.17|Stable|
|vTabList|v1.0.0|Release|
|vAnimatedTags|v2.4.1|Stable|
|vBrands|v1.0.0|Beta|

---

## ⚙️ Usage & Deployment

Compiled JAR files are available in the **Releases** section of this repository.

1. Clone the repository:

```bash
git clone https://github.com/SoldadoHumano/DeepCraft-1.0.git
```

2. Navigate to the plugin directory (e.g., ```Lobby/vLobby```):

```bash
cd DeepCraft-1.0/Lobby/vLobby
```

3. Build the plugin using Gradle:

```bash
./gradlew build
```

4. Place the generated JAR into your corresponding Minecraft server and restart the server.

> ⚠️ Note for DeepCraft Developers:
> 
> All new, modified plugins, or proposed system features must undergo mandatory commitment, testing, and verification within the private DeepCraft repository before being merged and pushed to this public repository (```DeepCraft-1.0```).

---

## ⚖️ License

This project is **Open Source** and licensed under the **GNU General Public License v3.0 (*GPLv3*)**.

You are authorized to:

- Use, study, and share the software.

- Modify and distribute derivative works, provided that:

  - Credit is maintained to the original author (SoldadoHumano).

  - The same license (GPLv3) is applied to any derived work.

For complete terms and conditions, refer to the [LICENSE](https://github.com/SoldadoHumano/DEEPCRAFT-1.0/blob/main/LICENSE) file in the root of this repository.

---

## 👤 About the Author

This repository is owned, maintained, and developed by:

* **Name:** Vitor
* **Discord:** vitor1227_op
* **GitHub:** [@SoldadoHumano](https://github.com/SoldadoHumano)

If you enjoyed this project, please consider giving it a ⭐!

---

## 💡 Highlights

- Active plugins deployed on DeepCraft production servers.
- Modular server system architecture, scalable for future expansions.
- Codebase adhering to best practices for maintainability and robust deployment.
- Centralized permission management via advanced [LuckPerms](https://github.com/LuckPerms/LuckPerms) integration.
- Full compliance with the GPLv3 license terms.

<h1 align="center">Made with ❤️ and ☕</h1>
