package io.github.moregrayner.miniame.cashStone.listener

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.ShopCategory
import io.github.moregrayner.miniame.cashStone.manager.ShopManager
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class ShopListener(private val plugin: CashStone) : Listener {

    private val shopManager: ShopManager? = plugin.shopManager

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clickedItem: ItemStack? = event.currentItem

        if (clickedItem == null || clickedItem.type == Material.AIR) return

        val title = event.view.title

        when {
            title.contains("석쩐!") -> {
                event.isCancelled = true

                when (clickedItem.type) {
                    Material.CRAFTING_TABLE -> shopManager?.openCategoryShop(player)
                    Material.POTION -> shopManager?.openCategoryShop(player) // 카테고리별 처리
                    Material.ENDER_CHEST -> shopManager?.openCategoryShop(player)
                    Material.EMERALD -> MessageUtil.sendMessage(player, "&a구매 버튼 클릭!")
                    Material.DIAMOND -> shopManager?.openCashShop(player)
                    else -> return
                }
            }

            title.contains("아이템 카테고리") -> {
                event.isCancelled = true
                when (clickedItem.type) {
                    Material.CRAFTING_TABLE -> shopManager?.openCategoryItems(player, ShopCategory.ESSENTIALS)
                    Material.POTION -> shopManager?.openCategoryItems(player, ShopCategory.BUFFS)
                    Material.ENDER_CHEST -> shopManager?.openCategoryItems(player, ShopCategory.ITEMS)
                    Material.ARROW -> shopManager?.openMainShop(player)
                    else -> return
                }
            }

            title.contains("필수품") || title.contains("버프") || title.contains("특수 아이템") -> {
                event.isCancelled = true
                if (clickedItem.type == Material.ARROW) {
                    shopManager?.openCategoryShop(player)
                    return
                }

                val category = when {
                    title.contains("필수품") -> ShopCategory.ESSENTIALS
                    title.contains("버프") -> ShopCategory.BUFFS
                    title.contains("특수 아이템") -> ShopCategory.ITEMS
                    else -> return
                }

                val shopItem = shopManager?.getShopItem(category, clickedItem.itemMeta?.displayName)
                if (shopItem != null) {
                    shopManager?.purchaseItem(player, category, shopItem.id)
                }
            }

            title.contains("캐시 충전") -> {
                event.isCancelled = true
                when (clickedItem.type) {
                    Material.GOLD_NUGGET -> plugin.getCashManager()?.addCash(player.uniqueId, 1000)
                    Material.GOLD_INGOT -> plugin.getCashManager()?.addCash(player.uniqueId, 5000)
                    Material.GOLD_BLOCK -> plugin.getCashManager()?.addCash(player.uniqueId, 10000)
                    Material.EMERALD_BLOCK -> MessageUtil.sendMessage(player, "&a결제가 완료되었습니다!")
                    else -> return
                }
            }
        }
    }
}
