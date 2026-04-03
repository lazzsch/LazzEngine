package net.lazz.core.services.module.menu

import net.lazz.core.services.module.ModuleManager
import net.lazz.core.services.smartiventory.SmartInventory
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class ModuleDetailMenu(
    private val manager: ModuleManager
) {

    fun open(player: Player, id: String) {

        val desc = manager.getDescription(id) ?: return

        val depends = desc.depends
        val dependents = manager.getDependents(id)

        val menu = SmartInventory(
            holderName = "module_detail",
            title = "§8Module: $id",
            items = emptyList(),
            player = player,
            backMenuAction = { ModuleMenu(manager).open(player) },
            rows = 4
        )

        // ================= INFO =================

        val infoLore = mutableListOf(
            "§7ID: §f${desc.id}",
            "§7Versão: §b${desc.version}"
        )

        if (depends.isNotEmpty()) {
            infoLore.add("")
            infoLore.add("§eDepende de:")
            depends.forEach {
                infoLore.add(" §7- §f$it")
            }
        }

        if (dependents.isNotEmpty()) {
            infoLore.add("")
            infoLore.add("§cDependentes:")
            dependents.forEach {
                infoLore.add(" §7- §f$it")
            }
        }

        menu.setCustomItem(11, createItem(
            Material.BOOK,
            "§fInformações",
            infoLore
        )) {}

        // ================= BUILDERS =================

        fun buildToggleItem(): ItemStack {
            val isEnabled = manager.isEnabled(id)
            val isLoading = manager.isLoading(id)

            val dependents = manager.getDependents(id)

            val lore = mutableListOf<String>()

            if (isLoading) {
                lore.add("§8Aguarde finalizar")
            } else {
                lore.add("§eShift + Clique para confirmar")

                if (dependents.isNotEmpty()) {
                    lore.add("")
                    lore.add("§cAo desativar:")
                    dependents.forEach {
                        lore.add(" §7- §f$it §cserá desativado")
                    }
                }
            }

            return createItem(
                when {
                    isLoading -> Material.GRAY_DYE
                    isEnabled -> Material.REDSTONE_BLOCK
                    else -> Material.EMERALD_BLOCK
                },
                when {
                    isLoading -> "§7Ação em andamento..."
                    isEnabled -> "§cDesativar"
                    else -> "§aAtivar"
                },
                lore
            )
        }

        fun buildReloadItem(): ItemStack {
            val isLoading = manager.isLoading(id)
            val dependents = manager.getDependents(id)

            val lore = mutableListOf<String>()

            if (isLoading) {
                lore.add("§8Aguarde finalizar")
            } else {
                lore.add("§eShift + Clique para confirmar")

                if (dependents.isNotEmpty()) {
                    lore.add("")
                    lore.add("§cAo recarregar:")
                    dependents.forEach {
                        lore.add(" §7- §f$it §cserá recarregado")
                    }
                }
            }

            return createItem(
                if (isLoading) Material.GRAY_DYE else Material.BLAZE_POWDER,
                if (isLoading) "§7Ação em andamento..." else "§eRecarregar",
                lore
            )
        }

        fun loadingItem(): ItemStack {
            return createItem(
                Material.CLOCK,
                "§7Ação em andamento...",
                listOf("§8Aguarde finalizar")
            )
        }

        // ================= TOGGLE =================

        menu.setCustomItem(14, buildToggleItem()) { click ->

            if (manager.isLoading(id)) return@setCustomItem

            if (click.click != ClickType.SHIFT_LEFT && click.click != ClickType.SHIFT_RIGHT) {
                player.sendMessage("§cUse SHIFT + Clique para confirmar")
                return@setCustomItem
            }

            val isEnabled = manager.isEnabled(id)

            if (isEnabled) {

                val future = manager.disableAsync(id)

                future.whenComplete { result, _ ->
                    manager.plugin.server.scheduler.runTask(manager.plugin, Runnable {
                        player.sendMessage(result.message)
                    })
                }

            } else {

                val future = manager.enableAsync(id)

                future.whenComplete { result, _ ->
                    manager.plugin.server.scheduler.runTask(manager.plugin, Runnable {
                        player.sendMessage(result.message)
                    })
                }
            }

            menu.updateItem(14, loadingItem())
            menu.updateItem(15, loadingItem())

            manager.plugin.server.scheduler.runTaskLater(manager.plugin, Runnable {
                menu.updateItem(14, buildToggleItem())
                menu.updateItem(15, buildReloadItem())
            }, 10L)
        }

        // ================= RELOAD =================

        menu.setCustomItem(15, buildReloadItem()) { click ->

            if (manager.isLoading(id)) return@setCustomItem

            if (click.click != ClickType.SHIFT_LEFT && click.click != ClickType.SHIFT_RIGHT) {
                player.sendMessage("§cUse SHIFT + Clique para confirmar")
                return@setCustomItem
            }

            val future = manager.reload(id)

            menu.updateItem(14, loadingItem())
            menu.updateItem(15, loadingItem())

            future.whenComplete { _, _ ->
                manager.plugin.server.scheduler.runTask(manager.plugin, Runnable {

                    menu.updateItem(14, buildToggleItem())
                    menu.updateItem(15, buildReloadItem())

                })
            }
        }

        menu.open()
    }

    private fun createItem(mat: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(mat)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        meta.lore = lore
        item.itemMeta = meta
        return item
    }
}