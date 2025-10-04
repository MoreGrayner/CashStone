package io.github.moregrayner.miniame.cashStone.listener

import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ItemStack

class CraftingListener : Listener {

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return

        val craftingLicense = ItemStack(Material.PAPER)
        val meta = craftingLicense.itemMeta
        meta?.setDisplayName(MessageUtil.colorize("&a제작권"))
        craftingLicense.itemMeta = meta

        val hasLicense = player.inventory.containsAtLeast(craftingLicense, 1)

        if (!hasLicense) {
            MessageUtil.sendMessage(player, "&c아이템을 제작하려면 제작권이 필요합니다! 상점에서 구매하세요.")
            event.inventory.close()
            event.isCancelled = true
        }
        val handItem = player.inventory.itemInMainHand
        if (handItem.type != Material.AIR) {
            handItem.amount -= 1
            if (handItem.amount <= 0) {
                player.inventory.setItemInMainHand(null)
            }
        }
    }
}