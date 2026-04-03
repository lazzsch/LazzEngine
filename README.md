# 🚀 LazzEngine

> **A powerful modular runtime engine for Bukkit/Paper plugins, featuring dynamic module loading, dependency injection, and a complete lifecycle system.**

---

## ✨ Features

* 🔌 **Runtime Module System** — Load `.jar` modules dynamically (no embedding)
* 🔄 **Hot Reload** — Enable, disable, and reload modules without restarting the server
* 🧩 **Service System** — Auto-discovered services via annotations
* 🔗 **Dependency Resolution** — Control module load order
* ⚙️ **Lifecycle Hooks** — `@OnLoad`, `@OnEnable`, `@OnDisable`
* ⚡ **Auto Command Registration**
* 🧩 **Module Isolation** — Dedicated ClassLoader per module
* 🛠 **Runtime Management** — Full control via in-game commands
* 🧹 **Safe Unload** — Proper cleanup on disable/reload

---

## ⚠️ Important Change

> ❗ **Modules are no longer embedded inside the main plugin jar.**

Modules are now:

✔ Fully independent
✔ Loaded at runtime
✔ Placed inside the `modules` folder

---

## 📦 How It Works

On first server start, LazzEngine generates:

```bash
/plugins/LazzEngine/modules/
```

Place your module `.jar` files inside:

```bash
/plugins/LazzEngine/modules/
 ├── money.jar
 ├── other.jar
```

Then load them:

```bash
/wm load <module>
/wm enable <module>
```

---

## 💡 Why This Change?

Old system (embedded modules):

* ❌ Required rebuilding the core
* ❌ Hard to update modules
* ❌ Limited modularity

New system:

* ✅ True modular architecture
* ✅ Independent updates
* ✅ Real hot reload
* ✅ Cleaner design

---

## ⚙️ Installation

```bash
./gradlew fullBuild
```

Place the generated `.jar` into:

```bash
/plugins/
```

Start the server once to generate folders.

---

## 🧩 Module Configuration

Each module must include:

```yaml
id: money
name: Money
main: net.lazz.modules.money.MoneyModule
package: net.lazz.modules.money
version: 1.0
```

---

# 🧩 Module API Example (Provider + Consumer)

This example demonstrates:

✔ A module providing an API
✔ Another module consuming it

---

## 📦 Scenario 1 — Provider Module (Money)

### MoneyModule.kt

```kotlin
class MoneyModule(
    plugin: JavaPlugin
) : AbstractModule(
    plugin = plugin,
    id = "money"
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

### MoneyService.kt

```kotlin
@ModuleAPI("MoneyService")
class MoneyService : ModuleCallable {

    private val data = ConcurrentHashMap<UUID, Int>()

    fun get(uuid: UUID): Int {
        return data.getOrDefault(uuid, 0)
    }

    fun add(uuid: UUID, value: Int) {
        data[uuid] = get(uuid) + value
    }

    override fun call(method: String, vararg args: Any?): Any? {
        return when (method) {
            "get" -> get(args[0] as UUID)
            "add" -> {
                add(args[0] as UUID, args[1] as Int)
                null
            }
            else -> null
        }
    }
}
```

---

## 📦 Scenario 2 — Consumer Module (Other)

### OtherModule.kt

```kotlin
@Depend("MoneyService")
class OtherModule(
    plugin: JavaPlugin
) : AbstractModule(
    plugin = plugin,
    id = "other"
) {

    override fun onEnable() {
        plugin.logger.info("[Other] Module enabled")
    }

    override fun onDisable() {
        super.onDisable()
        plugin.logger.info("[Other] Module disabled")
    }
}
```

---

### OtherCommand.kt

```kotlin
@ModuleCommand(
    name = "other",
    aliases = ["othertest"]
)
class OtherCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {

        if (sender !is Player) {
            sender.sendMessage("Players only.")
            return true
        }

        val uuid = sender.uniqueId

        val before = ModuleAPI.callAs<Int>("MoneyService", "get", uuid) ?: 0
        sender.sendMessage("§eCurrent balance: $before")

        ModuleAPI.call("MoneyService", "add", uuid, 50)

        val after = ModuleAPI.callAs<Int>("MoneyService", "get", uuid) ?: 0
        sender.sendMessage("§aNew balance: $after")

        return true
    }
}
```

---

## 🔄 Runtime Flow

```bash
/plugins/LazzEngine/modules/
 ├── money.jar
 ├── other.jar
```

```bash
/wm load money
/wm enable money

/wm load other
/wm enable other
```

Run:

```bash
/other
```

---

## 🧠 Behind the Scenes

* `MoneyService` is registered as a global API
* `@Depend` ensures proper load order
* `ModuleAPI` bridges module communication
* Each module runs in its own ClassLoader

---

## 💡 Pro Tip (Typed API)

Avoid string-based calls:

```kotlin
interface MoneyAPI {
    fun get(uuid: UUID): Int
    fun add(uuid: UUID, value: Int)
}
```

---

## ⚡ Commands

```bash
/wm - Open menu
/wm load <module>
/wm enable <module>
/wm disable <module>
/wm reload <module>
/wm list
```

---

## 🧪 Development

Requirements:

* Java 21
* Gradle
* Paper / Spigot 1.21+

---

## 🔥 Roadmap

* 📥 Module auto-download
* 🧠 Dependency graph resolver
* 🔁 Auto-load on startup
* 🛒 Module marketplace
* ⚙️ Per-module configuration

---

## 🌐 Language Notice

> ⚠️ **Not all parts of LazzEngine are fully in English yet.**

Currently:

* Some **log messages** are still in Portuguese
* Certain **internal messages and outputs** may not be fully translated
* Parts of the codebase still use mixed language (PT-BR + EN)

---

### 💡 Why?

The project is actively evolving, and full internationalization (i18n) is still in progress.

---

### 🚀 Future Plans

* Full English standardization
* Optional multi-language support
* Configurable language system

---

## 💬 Final Note

> LazzEngine is not just a plugin — it's a modular runtime framework.

---

## 👑 Author

Developed by **Lazz**
