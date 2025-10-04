package io.github.moregrayner.miniame.cashStone.listener

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.ShopCategory
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

class InventoryListener(private val plugin: CashStone) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) {
            return
        }

        val player = event.whoClicked as Player
        val title = event.view.title
        val clicked = event.currentItem

        if (clicked == null || clicked.type == Material.AIR) {
            return
        }


        if (!title.contains("석쩐!") && !title.contains("아이템 카테고리") &&
            !title.contains("필수품") && !title.contains("버프") && !title.contains("특수 아이템") &&
            !title.contains("캐시 충전")
        ) {
            return
        }

        event.isCancelled = true


        if (title.contains("석쩐!")) {
            handleMainShopClick(player, clicked)
        } else if (title == "§6아이템 카테고리") {
            handleCategoryClick(player, clicked)
        } else if (title == "§b캐시 충전") {
            handleCashShopClick(player, clicked)
        } else if (title.contains("필수품") || title.contains("버프") || title.contains("특수 아이템")) {
            handleItemPurchase(player, title, clicked, event.slot)
        }
    }

    private fun handleMainShopClick(player: Player, clicked: ItemStack) {
        when (clicked.type) {
            Material.EMERALD ->
                plugin.getShopManager()?.openCategoryShop(player)

            Material.DIAMOND ->
                plugin.getShopManager()?.openCashShop(player)

            else -> {}
        }
    }

    private fun handleCategoryClick(player: Player, clicked: ItemStack) {
        when (clicked.type) {
            Material.CRAFTING_TABLE ->
                plugin.getShopManager()?.openCategoryItems(player, ShopCategory.ESSENTIALS)

            Material.POTION ->
                plugin.getShopManager()?.openCategoryItems(player, ShopCategory.BUFFS)

            Material.ENDER_CHEST ->
                plugin.getShopManager()?.openCategoryItems(player, ShopCategory.ITEMS)

            Material.ARROW ->
                plugin.getShopManager()?.openMainShop(player)

            else -> {}
        }
    }

    private fun handleCashShopClick(player: Player, clicked: ItemStack) {
        val playerUuid = player.uniqueId

        when (clicked.type) {
            Material.GOLD_NUGGET -> {
                plugin.getCashManager()?.updateChargeAmount(playerUuid, 1000)
            }

            Material.GOLD_INGOT -> {
                plugin.getCashManager()?.updateChargeAmount(playerUuid, 5000)
            }

            Material.GOLD_BLOCK -> {
                plugin.getCashManager()?.updateChargeAmount(playerUuid, 10000)
            }

            Material.EMERALD_BLOCK -> {
                val amount: Int = plugin.getCashManager()?.getPendingCharge(playerUuid) ?: 0
                if (amount > 0) {
                    if (plugin.getCashManager()?.confirmCashCharge(playerUuid) == true) {
                        player.closeInventory()
                        MessageUtil.sendMessage(player, "&a결제가 완료되었습니다!")
                        MessageUtil.sendMessage(player, "&e${amount} 캐시가 충전되었습니다.")
                    } else {
                        MessageUtil.sendMessage(player, "&c결제에 실패했습니다!")
                    }
                } else {
                    MessageUtil.sendMessage(player, "&c충전할 금액을 선택해주세요!")
                }
            }

            else -> {}
        }
    }

    private fun updateCashButtonLore(inventory: Inventory, item: ItemStack, playerUuid: UUID) {
        val pending = plugin.getCashManager()?.getPendingCharge(playerUuid) ?: 0
        val meta = item.itemMeta ?: return

        meta.lore = listOf(
            "§7현재 선택된 금액:",
            "§e${pending} 원"
        )
        item.itemMeta = meta

        val slot = inventory.first(item)
        if (slot >= 0) {
            inventory.setItem(slot, item)
        }
    }


    private fun handleItemPurchase(player: Player, title: String, clicked: ItemStack, slot: Int) {
        if (slot >= 45) {
            if (clicked.type == Material.ARROW) {
                plugin.getShopManager()?.openCategoryShop(player)
            }
            return
        }

        val category: ShopCategory = when {
            title.contains("필수품") -> ShopCategory.ESSENTIALS
            title.contains("버프") -> ShopCategory.BUFFS
            title.contains("특수 아이템") -> ShopCategory.ITEMS
            else -> return
        }

        val itemId = getItemIdBySlot(category, slot)
        if (itemId != null) {
            if (plugin.getShopManager()?.purchaseItem(player, category, itemId) == true) {
                plugin.getShopManager()?.openCategoryItems(player, category)
            }
        }
    }

    private fun getItemIdBySlot(category: ShopCategory, slot: Int): String? {
        return when (category) {
            ShopCategory.ESSENTIALS -> when (slot) {
                0 -> "oak_sapling"
                1 -> "bone_powder"
                2 -> "lava_bucket"
                3 -> "water_bucket"
                4 -> "obsidian"
                5 -> "mining_0"
                6 -> "mining_1"
                7 -> "mining_2"
                8 -> "mining_3"
                9 -> "attack_right"
                else -> null
            }

            ShopCategory.BUFFS -> when (slot) {
                0 -> "rabbit_foot"
                1 -> "anvil"
                2 -> "iron_sword"
                else -> null
            }

            ShopCategory.ITEMS -> when (slot) {
                0 -> "time_accelerator"
                1 -> "wind_charge"
                2 -> "marker"
                3 -> "core_durability"
                4 -> "cannon"

                else -> null
            }

            else -> null
        }
    }

    private fun updateCashDisplay(player: Player) {
        val pending: Int = plugin.getCashManager()?.getPendingCharge(player.uniqueId) ?: return
        MessageUtil.sendMessage(player, "&e현재 충전 예정 금액: " + pending + "원")
    }
}