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

1. Download or build the project:

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

You can generate a module using:

```bash
./gradlew createModule -Pm=money
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

## 🧪 Development

Requirements:

* Java 21
* Gradle
* Paper / Spigot 1.21+

Run local test server:

```bash
./gradlew runServer
```

---

## 📦 Build System

* Multi-module Gradle project
* Automatic module detection
* Custom task for packaging modules into core

```bash
./gradlew fullBuild
```

---

## 🔥 Why LazzEngine?

Minecraft plugin development often leads to:

* ❌ Monolithic codebases
* ❌ Hard restarts for updates
* ❌ Poor modularity

**LazzEngine solves this by introducing a modular architecture with runtime control.**

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
2. Create your feature branch
3. Commit your changes
4. Open a Pull Request

---

## 📄 License

MIT License — feel free to use and modify.

---

## 👑 Author

Developed by **Lazz**

---

## 💬 Final Note

> LazzEngine is not just a plugin — it's a foundation for scalable Minecraft server architecture.
