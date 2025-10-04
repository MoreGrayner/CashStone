package io.github.moregrayner.miniame.cashStone.utils

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.stream.Collectors

object ItemUtil {

    private fun String?.colorize(): String {
        return this?.replace("&", "§") ?: ""
    }

    fun createCustomItem(
        material: Material?,
        amount: Int,
        displayName: String?,
        lore: List<String>?
    ): ItemStack {
        val item = ItemStack(material ?: Material.STONE, amount)
        val meta = item.itemMeta

        meta?.let {
            it.setDisplayName(displayName.colorize())
            if (!lore.isNullOrEmpty()) {
                it.lore = lore.map { line -> line.colorize() }
            }
            item.itemMeta = it
        }

        return item
    }
}
