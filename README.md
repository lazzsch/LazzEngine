# 🚀 LazzEngine

> **A powerful modular engine for Bukkit/Paper plugins, enabling dynamic module loading, dependency injection, and scalable plugin architecture.**

---

## ✨ Features

* 🔌 **Dynamic Module Loading** — Load `.jar` modules at runtime
* 🔄 **Hot Reload** — Enable, disable, reload modules without restarting the server
* 🧠 **Dependency Injection** — Lightweight service container with `@Inject`
* ⚡ **Auto Command Registration** — Annotate commands with `@CommandInfo`
* 📦 **Service System** — Register and inject services per module
* 🧩 **Module Isolation** — ClassLoader-based separation
* 🛠 **Runtime Management** — Manage modules with in-game commands
* 🧹 **Safe Cleanup** — Proper command unregistering and service cleanup

---

## 🏗️ Architecture

```
Core
 ├── Bootstrap
 ├── ModuleManager
 ├── CommandRegistry
 └── ServiceRegistry

Modules (.jar)
 ├── Commands (@CommandInfo)
 ├── Services (@Service)
 ├── Listeners (@AutoListener)
```

---

## ⚙️ Installation

1. Build the project:

```bash
./gradlew fullBuild
```

2. Place the generated plugin `.jar` into your server `plugins/` folder

3. Start your server

---

## 📁 Modules Folder

Modules are loaded from:

```
/plugins/LazzEngine/modules/
```

> ⚡ Modules are automatically detected during build via `settings.gradle.kts`.

Each module must be a `.jar` file.

---

## 📄 Module Configuration

Each module must contain a YAML file:

```yaml
id: money
name: Money
main: net.lazz.modules.money.MoneyModule
package: net.lazz.modules.money
version: 1.0
depends: []
```

---

## 🧩 Creating a Module

Manual:

```bash
./gradlew createModule -Pm=money
```

Or via helper (recommended):

```bash
./gradlew helper
```

---

### 📌 Example Module

```kotlin
class MoneyModule(plugin: JavaPlugin) : AbstractModule(
    id = "money",
    context = ModuleContext(plugin)
) {

    override fun onEnable() {
        plugin.logger.info("[Money] Module enabled")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Money] Module disabled")
    }
}
```

---

## ⚡ Commands

```
/wm load <module>
/wm enable <module>
/wm disable <module>
/wm reload <module>
/wm list
```

---

## 🧠 Dependency Injection Example

```kotlin
@CommandInfo(name = "money")
class MoneyCommand : CommandExecutor {

    @Inject
    lateinit var service: MoneyService

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        sender.sendMessage(service.hello())
        return true
    }
}
```

---

## 🔌 Service Example

```kotlin
@Service
class MoneyService {

    fun hello(): String {
        return "Service working!"
    }
}
```

---

## 🧰 Helper CLI (Developer Tooling)

The project includes an interactive CLI to automate development tasks:

```bash
./gradlew helper
```

---

### 📋 Available Options

```
1. Commit (Git)
2. Version (Bump)
3. Generate Changelog
4. Create Release
5. Create Module
6. Full Build
```

---

### ✨ Commit Assistant (Conventional Commits)

The helper guides you through creating standardized commits:

| Type     | Description             |
| -------- | ----------------------- |
| feat     | New feature             |
| fix      | Bug fix                 |
| refactor | Internal improvement    |
| perf     | Performance improvement |
| docs     | Documentation           |
| style    | Code formatting         |
| test     | Tests                   |
| chore    | Maintenance             |

Example:

```
feat(module): add economy system
fix(core): fix module unload bug
```

---

### ⚠️ Breaking Changes

If your change breaks compatibility, the helper marks it automatically:

```
feat(api)!: change module structure
```

---

### 📦 Versioning (SemVer)

The helper updates project version automatically:

| Type  | Example       |
| ----- | ------------- |
| patch | 1.0.0 → 1.0.1 |
| minor | 1.0.0 → 1.1.0 |
| major | 1.0.0 → 2.0.0 |

---

### 📜 Changelog

Automatically generates `CHANGELOG.md` based on commits.

---

### 🚀 Release

Creates commit + tag:

```bash
git tag vX.X.X
git push origin vX.X.X
```

---

## 🧪 Development

Requirements:

* Java 21
* Gradle (use wrapper: `./gradlew`)
* Paper / Spigot 1.21+

Run local test server:

```bash
./gradlew runServer
```

---

## 📦 Build System

* Multi-module Gradle project
* Automatic module detection
* Module packaging into core

```bash
./gradlew fullBuild
# or
./gradlew helper
```

---

## 🔥 Why LazzEngine?

Minecraft plugin development often leads to:

* ❌ Monolithic codebases
* ❌ Hard restarts for updates
* ❌ Poor modularity

**LazzEngine solves this with a modular runtime architecture.**

---

## 🧭 Roadmap

* [ ] Improved ClassLoader isolation
* [ ] Module dependency graph resolver
* [ ] Event bus system
* [ ] Config system per module
* [ ] Public API distribution (Maven/Gradle)
* [ ] Module marketplace

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the project
2. Create your branch
3. Commit your changes
4. Open a Pull Request

---

## 📄 License

MIT License — free to use and modify.

---

## 👑 Author

Developed by **Lazz**

---

## 💬 Final Note

> LazzEngine is not just a plugin — it's a foundation for scalable Minecraft server architecture.
