package net.lazz.core.services.smartiventory

import net.lazz.core.services.smartiventory.holder.SmartInventoryHolder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class SmartInventory(
    private val holderName: String,
    private val title: String,
    private var items: List<ItemStack>,
    private val player: Player,
    private val isPaginated: Boolean = false,
    private val closeName: String = "§cFechar",
    private val pageName: String = "Página",
    private val prevPageName: String = "§ePágina Anterior",
    private val nextPageName: String = "§ePróxima Página",
    private val backMenuName: String = "§eVoltar ao Menu",
    private val prevPageLore: List<String>? = listOf("§7Clique para ir à página anterior"),
    private val nextPageLore: List<String>? = listOf("§7Clique para ir à próxima página"),
    private val closeLore: List<String>? = listOf("§7Clique para fechar o menu."),
    private val backMenuLore: List<String>? = listOf("§7Voltar ao menu anterior"),
    private val backMenuAction: (() -> Unit)? = null,
    private val rows: Int = 6
) {

    enum class SlotType { LEFT_3, LEFT_2, LEFT_1, LEFT, CENTER, RIGHT, RIGHT_1, RIGHT_2, RIGHT_3 }

    private val useCloseButton = backMenuAction == null

    private var itemClickHandler: ((InventoryClickEvent, ItemStack, Int) -> Unit)? = null

    private val customButtons = mutableMapOf<Int, Pair<ItemStack, (InventoryClickEvent) -> Unit>>()
    private val controlItems = mutableListOf<Triple<SlotType, ItemStack, (InventoryClickEvent) -> Unit>>()

    private var inventories: MutableList<Inventory> = mutableListOf()
    private var currentPage = 0

    private var animationTask: BukkitRunnable? = null
    private var locked = false

    private var itemsProvider: (() -> List<ItemStack>)? = null

    // ================= API =================

    fun setItemsProvider(provider: () -> List<ItemStack>) {
        this.itemsProvider = provider
    }

    fun setItemClickHandler(handler: (InventoryClickEvent, ItemStack, Int) -> Unit) {
        this.itemClickHandler = handler
    }

    fun setCustomItem(slot: Int, item: ItemStack, action: (InventoryClickEvent) -> Unit) {

        customButtons[slot] = item to action

        ensureInventories()

        inventories.forEach { inv ->
            if (slot < inv.size) inv.setItem(slot, item)
        }
    }

    fun setControlItem(type: SlotType, item: ItemStack, action: (InventoryClickEvent) -> Unit) {
        controlItems.add(Triple(type, item, action))
    }

    fun refresh() {
        cancelAnimation()

        val previousPage = currentPage

        inventories.clear()
        inventories = createInventories().toMutableList()

        currentPage = previousPage.coerceAtMost(inventories.lastIndex).coerceAtLeast(0)

        open()
    }

    fun updateItem(slot: Int, item: ItemStack) {
        val action = customButtons[slot]?.second ?: { _: InventoryClickEvent -> }

        customButtons[slot] = item to action

        inventories.forEach { inv ->
            if (slot < inv.size) inv.setItem(slot, item)
        }

        if (player.openInventory.topInventory.holder is SmartInventoryHolder) {
            player.updateInventory()
        }
    }

    fun updateControlItem(type: SlotType, item: ItemStack) {

        controlItems.replaceAll { (t, oldItem, action) ->
            if (t == type) Triple(type, item, action)
            else Triple(t, oldItem, action)
        }

        inventories.forEach { inv ->

            val slot = getFooterSlot(type, inv.size)

            if (slot < inv.size) {
                inv.setItem(slot, item)
            }
        }

        player.updateInventory()
    }

    // ================= PROVIDER =================

    private fun resolveItems(): List<ItemStack> {
        return itemsProvider?.invoke() ?: items
    }

    // ================= LOADING =================

    fun <T> runLoadingProcess(
        plugin: JavaPlugin,
        loadingDuration: Long = 20L,
        resultDuration: Long = 10L,
        task: () -> T?,
        isSuccess: (T?) -> Boolean = { it != null },
        onComplete: (T?, Boolean) -> Unit = { _, _ -> }
    ) {

        locked = true // 🔥 trava clique
        cancelAnimation()
        animateLoading(plugin, loadingDuration)

        object : BukkitRunnable() {
            override fun run() {

                val result = try { task() } catch (ex: Exception) {
                    ex.printStackTrace(); null
                }

                val success = try { isSuccess(result) } catch (_: Exception) { false }

                animateResult(plugin, success, resultDuration)
                onComplete(result, success)

                object : BukkitRunnable() {
                    override fun run() {
                        refresh()
                    }
                }.runTaskLater(plugin, resultDuration * 5)
            }
        }.runTaskLater(plugin, loadingDuration * 2)
    }

    // ================= ANIMATION =================

    fun animateLoading(plugin: JavaPlugin, duration: Long = 20L) {
        cancelAnimation()

        val slots = getFooterSlots()
        if (slots.isEmpty()) return

        var index = 0

        animationTask = object : BukkitRunnable() {
            var ticks = 0

            override fun run() {
                if (ticks >= duration) {
                    finishAnimation()
                    cancel()
                    return
                }

                val active = slots[index % slots.size]

                slots.forEach { slot ->
                    val mat = if (slot == active) Material.LIME_STAINED_GLASS_PANE else Material.GRAY_STAINED_GLASS_PANE
                    val item = ItemStack(mat)

                    inventories.forEach { if (slot < it.size) it.setItem(slot, item) }
                }

                index++
                ticks++
            }
        }

        animationTask!!.runTaskTimer(plugin, 0L, 2L)
    }

    fun animateResult(plugin: JavaPlugin, success: Boolean, duration: Long = 10L) {
        cancelAnimation()

        val slots = getFooterSlots()
        if (slots.isEmpty()) return

        val mat = if (success) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE

        animationTask = object : BukkitRunnable() {
            var ticks = 0
            var visible = true

            override fun run() {
                if (ticks >= duration) {
                    finishAnimation()
                    cancel()
                    return
                }

                val current = if (visible) mat else Material.GRAY_STAINED_GLASS_PANE

                slots.forEach { slot ->
                    val item = ItemStack(current)
                    inventories.forEach { if (slot < it.size) it.setItem(slot, item) }
                }

                visible = !visible
                ticks++
            }
        }

        animationTask!!.runTaskTimer(plugin, 0L, 5L)
    }

    // ================= BUILD =================

    private fun createInventories(): List<Inventory> {

        val items = resolveItems()
        val isFreeLayout = items.isEmpty()

        if (isFreeLayout) {

            val rows = this.rows.coerceIn(1, 6)
            val size = rows * 9

            val holder = SmartInventoryHolder(holderName).apply {
                inventoryHandler = this@SmartInventory
            }

            val inv = Bukkit.createInventory(holder, size, title)

            val left = getFooterSlot(SlotType.LEFT, size)
            val center = getFooterSlot(SlotType.CENTER, size)
            val right = getFooterSlot(SlotType.RIGHT, size)

            if (center < inv.size) {
                inv.setItem(
                    center,
                    if (useCloseButton)
                        nav(Material.REDSTONE, closeName, closeLore)
                    else
                        nav(Material.SPECTRAL_ARROW, backMenuName, backMenuLore)
                )
            }

            controlItems.forEach { (type, item, action) ->
                val slot = getFooterSlot(type, size)

                if (slot < inv.size) {
                    inv.setItem(slot, item)
                    customButtons[slot] = item to action
                }
            }

            return listOf(inv)
        }

        val previewRows = calculateRowsForPage(items.size)
        val baseSlotMap = getStyledSlotMap(previewRows)
        val maxItemsPerPage = baseSlotMap.size.coerceAtLeast(1)

        val paginationEnabled = isPaginated && items.size > maxItemsPerPage

        val totalPages = if (!paginationEnabled) 1 else (items.size + maxItemsPerPage - 1) / maxItemsPerPage

        return List(totalPages) { page ->

            val start = page * maxItemsPerPage
            val end = (start + maxItemsPerPage).coerceAtMost(items.size)

            val count = end - start
            val rows = calculateRowsForPage(count)
            val size = rows * 9

            val slotMap = getStyledSlotMap(rows)

            val holder = SmartInventoryHolder(holderName).apply {
                inventoryHandler = this@SmartInventory
            }

            val inv = Bukkit.createInventory(holder, size, "$title §7($pageName ${page + 1})")

            val left = getFooterSlot(SlotType.LEFT, size)
            val center = getFooterSlot(SlotType.CENTER, size)
            val right = getFooterSlot(SlotType.RIGHT, size)

            slotMap.forEachIndexed { index, slot ->
                if (start + index < end && slot < inv.size) {
                    inv.setItem(slot, items[start + index])
                }
            }

            if (paginationEnabled) {
                if (page > 0 && left < inv.size) {
                    inv.setItem(left, nav(Material.ARROW, prevPageName, prevPageLore))
                }

                if (page < totalPages - 1 && right < inv.size) {
                    inv.setItem(right, nav(Material.ARROW, nextPageName, nextPageLore))
                }
            }

            if (center < inv.size) {
                inv.setItem(
                    center,
                    if (useCloseButton)
                        nav(Material.REDSTONE, closeName, closeLore)
                    else
                        nav(Material.SPECTRAL_ARROW, backMenuName, backMenuLore)
                )
            }

            controlItems.forEach { (type, item, action) ->
                val slot = getFooterSlot(type, size)

                if (slot < inv.size) {
                    inv.setItem(slot, item)
                    customButtons[slot] = item to action
                }
            }

            inv
        }
    }

    private fun getFooterSlot(type: SlotType, size: Int): Int {
        val base = size - 9

        return when (type) {
            SlotType.LEFT_3 -> base
            SlotType.LEFT_2 -> base + 1
            SlotType.LEFT_1 -> base + 2

            SlotType.LEFT -> base + 3
            SlotType.CENTER -> base + 4
            SlotType.RIGHT -> base + 5

            SlotType.RIGHT_1 -> base + 6
            SlotType.RIGHT_2 -> base + 7
            SlotType.RIGHT_3 -> base + 8
        }
    }

    private fun calculateRowsForPage(count: Int): Int {
        if (!isPaginated) return rows.coerceIn(4, 6)
        return when {
            count <= 7 -> 4
            count <= 14 -> 5
            else -> 6
        }
    }

    private fun getStyledSlotMap(rows: Int): List<Int> = when (rows) {
        6 -> listOf(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34)
        5 -> listOf(10,11,12,13,14,15,16,19,20,21,22,23,24,25)
        4 -> listOf(10,11,12,13,14,15,16)
        else -> emptyList()
    }

    private fun nav(mat: Material, name: String, lore: List<String>?): ItemStack {
        val item = ItemStack(mat)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(name)
        lore?.let { meta.lore = it }
        item.itemMeta = meta
        return item
    }

    fun open() {
        ensureInventories()
        player.openInventory(inventories[currentPage])
    }

    fun getInventory(): Inventory {
        ensureInventories()
        return inventories[currentPage]
    }

    fun handleClick(event: InventoryClickEvent) {

        if (locked) {
            event.isCancelled = true
            return
        }

        val slot = event.slot
        val item = event.currentItem
        val size = event.inventory.size

        customButtons[slot]?.let {
            it.second.invoke(event)
            event.isCancelled = true
            return
        }

        val left = getFooterSlot(SlotType.LEFT, size)
        val center = getFooterSlot(SlotType.CENTER, size)
        val right = getFooterSlot(SlotType.RIGHT, size)

        when (slot) {
            center -> if (useCloseButton) player.closeInventory() else backMenuAction?.invoke()
            left -> if (currentPage > 0) previousPage()
            right -> if (currentPage < inventories.lastIndex) nextPage()
            else -> if (item != null && !item.type.isAir) itemClickHandler?.invoke(event, item, slot)
        }

        event.isCancelled = true
    }

    private fun ensureInventories() {
        if (inventories.isEmpty()) inventories = createInventories().toMutableList()
    }

    private fun cancelAnimation() {
        animationTask?.cancel()
        animationTask = null
    }

    private fun finishAnimation() {
        cancelAnimation()
        locked = false

        customButtons.forEach { (slot, pair) ->
            inventories.forEach { if (slot < it.size) it.setItem(slot, pair.first) }
        }

        player.updateInventory()
    }

    private fun previousPage() { currentPage--; open() }
    private fun nextPage() { currentPage++; open() }

    private fun getFooterSlots(): List<Int> {
        ensureInventories()
        val size = inventories.first().size
        return (size - 9 until size).toList()
    }
}