package net.lazz.core.services.module.menu

import net.lazz.core.services.module.ModuleManager
import net.lazz.core.services.module.ModuleView
import net.lazz.core.services.smartiventory.SmartInventory
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class ModuleMenu(
    private val manager: ModuleManager
) {

    fun open(player: Player) {

        val menu = SmartInventory(
            holderName = "modules",
            title = "§8Modules - v${manager.plugin.description.version}",
            items = emptyList(),
            player = player,
            isPaginated = true
        )

        menu.setItemsProvider {
            val modules = manager.getModules().sortedByDescending { it.enabled }
            modules.map { createModuleItem(it) }.ifEmpty { listOf(createEmptyItem()) }
        }

        /*menu.setItemsProvider {

            val realModules = manager.getModules()

            val fakeModules = (1..50).map { i ->

                val depends = when {
                    i % 5 == 0 -> listOf("core", "economy")
                    i % 3 == 0 -> listOf("core")
                    i % 2 == 0 -> listOf("chat")
                    else -> emptyList()
                }

                ModuleView(
                    id = "fake$i",
                    name = "Fake Module $i",
                    version = "1.$i",
                    enabled = i % 2 == 0,
                    depends = depends
                )
            }

            val allModules = (realModules + fakeModules)
                .sortedByDescending { it.enabled }

            allModules.map { createModuleItem(it) }
                .ifEmpty { listOf(createEmptyItem()) }
        }*/

        // CLICK
        menu.setItemClickHandler { _, item, _ ->

            val id = ChatColor.stripColor(item.itemMeta?.displayName)
                ?.replace(" (NEW)", "")
                ?: return@setItemClickHandler

            ModuleDetailMenu(manager).open(player, id)
        }

        // LOAD
        menu.setControlItem(SmartInventory.SlotType.LEFT_1, createLoadButton()) {

            player.sendMessage("§7Atualizando módulos...")

            menu.runLoadingProcess(
                plugin = manager.plugin,
                task = { manager.loadNewModules() },
                isSuccess = { result ->
                    result != null && result.hasNew && result.errors == 0
                }
            ) { result, _ ->

                if (result == null) {
                    player.sendMessage("§cErro inesperado ao carregar módulos.")
                    return@runLoadingProcess
                }

                when {
                    result.empty ->
                        player.sendMessage("§eNenhum módulo encontrado.")

                    result.hasNew && result.errors == 0 ->
                        player.sendMessage("§a${result.loaded} módulo(s) carregado(s)!")

                    result.hasNew && result.errors > 0 ->
                        player.sendMessage("§e${result.loaded} carregado(s), §c${result.errors} erro(s).")

                    else ->
                        player.sendMessage("§7Nenhum módulo novo encontrado.")
                }
            }
        }

        menu.setControlItem(SmartInventory.SlotType.RIGHT_1, createLogSettingsButton()) { click ->

            when (click.click) {

                org.bukkit.event.inventory.ClickType.LEFT -> {
                    val state = manager.toggleDebug()
                    player.sendMessage("§7Debug agora está: " + (if (state) "§aON" else "§cOFF"))
                }

                org.bukkit.event.inventory.ClickType.RIGHT -> {
                    val state = manager.toggleVerbose()
                    player.sendMessage("§7Verbose agora está: " + (if (state) "§aON" else "§cOFF"))
                }

                else -> return@setControlItem
            }

            menu.updateControlItem(SmartInventory.SlotType.RIGHT_1, createLogSettingsButton())
        }

        menu.open()
    }

    private fun createModuleItem(module: ModuleView): ItemStack {

        val mat = if (module.enabled) Material.LIME_CONCRETE else Material.RED_CONCRETE
        val dependents = manager.getDependents(module.id)

        return ItemStack(mat).apply {

            val meta = itemMeta!!

            val isNew = !module.enabled

            meta.setDisplayName(
                (if (module.enabled) "§a" else "§c") + module.id +
                        (if (isNew) " §e(NEW)" else "")
            )

            meta.lore = buildList {
                add("§7Nome: §f${module.name}")
                add("§7Versão: §b${module.version}")
                add("")
                add(if (module.enabled) "§aATIVO" else "§cDESATIVADO")

                // ================= DEPENDS =================
                if (module.depends.isNotEmpty()) {
                    add("")
                    add("§eDependências:")

                    module.depends.forEach { dep ->

                        val isLoaded = manager.getModules().any {
                            it.id.equals(dep, ignoreCase = true) && it.enabled
                        }

                        val color = if (isLoaded) "§a" else "§c"

                        add("§7- $color$dep")
                    }
                }

                // ================= DEPENDENTS =================
                if (dependents.isNotEmpty()) {
                    add("")
                    add("§cDependentes:")

                    dependents.forEach {
                        add("§7- §f$it")
                    }

                    add("")
                    add("§c⚠ Este módulo afeta outros!")
                }

                // ================= HINT =================
                add("")
                add("§7Clique para gerenciar")
            }

            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            itemMeta = meta
        }
    }

    private fun createEmptyItem(): ItemStack {
        return ItemStack(Material.BARRIER).apply {
            val meta = itemMeta!!
            meta.setDisplayName("§cNenhum módulo encontrado")
            meta.lore = listOf("§7Adicione módulos na pasta modules/")
            itemMeta = meta
        }
    }

    private fun createLoadButton(): ItemStack {
        return ItemStack(Material.EMERALD).apply {

            val meta = itemMeta!!

            meta.setDisplayName("§aCarregar novos módulos")
            meta.lore = listOf(
                "§7Escaneia a pasta modules/",
                "§7e carrega novos módulos",
                "",
                "§eClique para atualizar"
            )

            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            itemMeta = meta
        }
    }

    private fun createLogSettingsButton(): ItemStack {

        val debug = manager.isDebug()
        val verbose = manager.isVerbose()

        return ItemStack(Material.COMPARATOR).apply {

            val meta = itemMeta!!

            meta.setDisplayName("§bConfiguração de Logs")

            meta.lore = listOf(
                "",
                "§7Debug: " + (if (debug) "§aON" else "§cOFF"),
                "§7Verbose: " + (if (verbose) "§aON" else "§cOFF"),
                "",
                "§eClique esquerdo → Toggle Debug",
                "§eClique direito → Toggle Verbose"
            )

            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            itemMeta = meta
        }
    }
}