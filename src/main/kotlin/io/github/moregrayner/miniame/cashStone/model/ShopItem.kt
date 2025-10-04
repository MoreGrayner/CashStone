package io.github.moregrayner.miniame.cashStone.model

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Nullable


class ShopItem {
    var id: String
        private set
    var material: Material
        private set
    var customItem: ItemStack? = null
        private set
    var displayName: String
        private set
    var price: Int
        private set
    @Nullable var lore: List<String>? = null

    constructor(id: String, material: Material, displayName: String, price: Int, lore: List<String>) {
        this.id = id
        this.material = material
        this.displayName = displayName
        this.price = price
        this.lore = lore
    }

    constructor(id: String, customItem: ItemStack, displayName: String, price: Int, lore: List<String>) {
        this.id = id
        this.customItem = customItem
        this.material = customItem.type
        this.displayName = displayName
        this.price = price
        this.lore = lore
    }
}