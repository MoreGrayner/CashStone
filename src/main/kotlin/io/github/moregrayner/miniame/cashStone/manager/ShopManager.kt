package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.ShopCategory
import io.github.moregrayner.miniame.cashStone.model.ShopItem
import io.github.moregrayner.miniame.cashStone.utils.ItemUtil
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class ShopManager(private val plugin: CashStone) {
    private val shopItems: MutableMap<ShopCategory, List<ShopItem>> = EnumMap(ShopCategory::class.java)

    init {
        initializeShopItems()
    }

    private fun initializeShopItems() {
        val essentials: MutableList<ShopItem> = ArrayList()
        essentials.add(
            ShopItem(
                "oak_sapling", Material.OAK_SAPLING, "참나무 묘목", 100,
                mutableListOf("50% 특별 할인!")
            )
        )

        essentials.add(
            ShopItem(
                "bone_powder", Material.BONE_MEAL, "뼛가루", 100,
                mutableListOf("무려 다이아몬드와 같은 재질로 만들어진 뼛가루!")
            )
        )
        essentials.add(
            ShopItem(
                "lava_bucket", Material.LAVA_BUCKET, "용암 양동이", 200, emptyList()
            )
        )
        essentials.add(
            ShopItem(
                "water_bucket", Material.WATER_BUCKET, "물 양동이", 150, emptyList()
            )
        )
        essentials.add(
            ShopItem(
                "obsidian", Material.OBSIDIAN, "흑요석", 300, emptyList()
            )
        )


        essentials.add(
            ShopItem(
                "mining_0", Material.WOODEN_PICKAXE, "채굴권 0단계", 0,
                mutableListOf("&c이 세상은 무엇을 하던지 돈이 필요하지.", "&7기본 상태")
            )
        )
        essentials.add(
            ShopItem(
                "mining_1", Material.STONE_PICKAXE, "채굴권 1단계", 500,
                mutableListOf("&7무려 블록을 캘 수 있다고!?")
            )
        )
        essentials.add(
            ShopItem(
                "mining_2", Material.IRON_PICKAXE, "채굴권 2단계", 1000,
                mutableListOf("&7무려 성급함 1!")
            )
        )
        essentials.add(
            ShopItem(
                "mining_3", Material.DIAMOND_PICKAXE, "채굴권 3단계", 1500,
                mutableListOf("&7무려 성급함 2!?")
            )
        )


        essentials.add(
            ShopItem(
                "attack_right", Material.SNOWBALL, "공격권 (1분)", 300,
                mutableListOf("&71분간 돌을 투척할 수 있습니다.", "&c사망 시 구독 취소됩니다.")
            )
        )

        shopItems[ShopCategory.ESSENTIALS] = essentials


        val buffs: MutableList<ShopItem> = ArrayList()
        buffs.add(
            ShopItem(
                "rabbit_foot", Material.RABBIT_FOOT, "토끼발", 800,
                mutableListOf("&7신속 2, 점프강화 2 효과", "&e영구 지속")
            )
        )
        buffs.add(
            ShopItem(
                "anvil", Material.ANVIL, "부동세", 600,
                mutableListOf("&7넉백 저항 효과", "&e영구 지속")
            )
        )
        buffs.add(
            ShopItem(
                "iron_sword", Material.IRON_SWORD, "힘", 700,
                mutableListOf("&7돌 투척 사정거리 1.5배 증가", "&e영구 지속")
            )
        )

        shopItems[ShopCategory.BUFFS] = buffs


        val items: MutableList<ShopItem> = ArrayList()
        items.add(
            ShopItem(
                "time_accelerator", Material.CLOCK, "시간가속기", 1000,
                mutableListOf("&7게임 틱을 2배 가속시킵니다.", "&c1회 구매 제한")
            )
        )
        items.add(
            ShopItem(
                "wind_charge", Material.WIND_CHARGE, "돌풍구", 400,
                mutableListOf("&7마인크래프트 돌풍구", "&e소모품")
            )
        )
        items.add(
            ShopItem(
                "marker", Material.ENCHANTED_BOOK, "표식", 350,
                mutableListOf("우클릭한 대상에게 넉백 1.5배를 받는 표식을 부여합니다.", "&e소모품")
            )
        )

        items.add(
            ShopItem(
                "core_durability", Material.SHIELD, "팀 코어 내구도 +10", 100,
                mutableListOf("&7팀 코어의 내구도를 10 증가시킵니다.", "&e구매 시 즉시 적용")
            )
        )



        val cannon1: ItemStack = ItemUtil.createCustomItem(
            Material.TNT, 1, "&c대포 (5/5)",
            mutableListOf("&7TNT를 5연발 발사합니다.", "&e우클릭으로 사용")
        )
        items.add(
            ShopItem(
                "cannon", cannon1, "대포", 1200,
                mutableListOf("&7사정거리: 돌 투척의 1.5배", "&e최대 5번 사용 가능")
            )
        )

        items.add(
            ShopItem(
                "crafting_license", Material.PAPER, "&a제작권", 100,
                mutableListOf("&7아이템 제작용 티켓입니다.", "&e영구 소지(아님)")
            )
        )

        shopItems[ShopCategory.ITEMS] = items
    }

    fun openMainShop(player: Player) {
        val inv = Bukkit.createInventory(null, 27, "§6상점!")


        val buyButton: ItemStack = ItemUtil.createCustomItem(
            Material.EMERALD, 1,
            "&a구매하기", mutableListOf("&7아이템을 구매합니다.")
        )
        inv.setItem(11, buyButton)


        val cashButton: ItemStack = ItemUtil.createCustomItem(
            Material.DIAMOND, 1,
            "&b캐시 충전", mutableListOf("&7실제 돈으로 캐시를 충전합니다.")
        )
        inv.setItem(15, cashButton)


        val currentCash: Int = plugin.getCashManager()?.getCash(player.uniqueId) ?: 0
        val cashDisplay: ItemStack = ItemUtil.createCustomItem(
            Material.GOLD_INGOT, 1,
            "&e현재 캐시: $currentCash", mutableListOf("&7보유중인 캐시입니다.")
        )
        inv.setItem(13, cashDisplay)


        val netherStar: ItemStack = ItemUtil.createCustomItem(
            Material.NETHER_STAR, 1,
            "&d상점 액세스", mutableListOf("&7상점에 접근할 수 있는 아이템입니다.", "&c드롭되지 않습니다.")
        )
        if (!player.inventory.contains(Material.NETHER_STAR)) {
            player.inventory.addItem(netherStar)
        }

        player.openInventory(inv)
    }

    fun openCategoryShop(player: Player) {
        val inv = Bukkit.createInventory(null, 27, "§6아이템 카테고리")


        val essentialsButton: ItemStack = ItemUtil.createCustomItem(
            Material.CRAFTING_TABLE, 1,
            "&a필수품", mutableListOf("&7게임에 필요한 기본 아이템들")
        )
        inv.setItem(11, essentialsButton)


        val buffsButton: ItemStack = ItemUtil.createCustomItem(
            Material.POTION, 1,
            "&c버프", mutableListOf("&7플레이어를 강화하는 아이템들")
        )
        inv.setItem(13, buffsButton)


        val itemsButton: ItemStack = ItemUtil.createCustomItem(
            Material.ENDER_CHEST, 1,
            "&d특수 아이템", mutableListOf("&7특별한 효과를 가진 아이템들")
        )
        inv.setItem(15, itemsButton)


        val backButton: ItemStack = ItemUtil.createCustomItem(
            Material.ARROW, 1,
            "&7뒤로가기", mutableListOf("&7메인 상점으로 돌아갑니다.")
        )
        inv.setItem(22, backButton)

        player.openInventory(inv)
    }

    fun openCategoryItems(player: Player, category: ShopCategory) {
        val title = "§6" + getCategoryName(category)
        val inv = Bukkit.createInventory(null, 54, title)

        val items: List<ShopItem> = shopItems[category] ?: return

        var slot = 0
        for (shopItem in items) {
            if (slot >= 45) break
            val displayItem: ItemStack = if (shopItem.customItem != null) {
                shopItem.customItem!!.clone()
            } else {
                ItemUtil.createCustomItem(
                    shopItem.material,
                    1,
                    shopItem.displayName,
                    shopItem.lore
                )
            }


            val lore: MutableList<String> = shopItem.lore as MutableList<String>
            lore.add("")
            lore.add(("&6가격: " + shopItem.price.toString() + " 캐시"))
            lore.add("&e클릭하여 구매!")

            val finalItem: ItemStack = ItemUtil.createCustomItem(
                displayItem.type, displayItem.amount,
                shopItem.displayName, lore
            )

            if (displayItem.hasItemMeta() && displayItem.itemMeta.hasCustomModelData()) {
                finalItem.itemMeta.setCustomModelData(displayItem.itemMeta.customModelData)
            }

            inv.setItem(slot, finalItem)
            slot++
        }


        val backButton: ItemStack = ItemUtil.createCustomItem(
            Material.ARROW, 1,
            "&7뒤로가기", mutableListOf("&7카테고리 선택으로 돌아갑니다.")
        )
        inv.setItem(49, backButton)

        player.openInventory(inv)
    }

    fun openCashShop(player: Player) {
        val inv = Bukkit.createInventory(null, 9, "§b캐시 충전")


        val cash1000: ItemStack = ItemUtil.createCustomItem(
            Material.GOLD_NUGGET, 1,
            "&a1000원 충전", mutableListOf("&71000 캐시를 충전합니다.", "&e클릭할 때마다 1000씩 증가")
        )
        inv.setItem(2, cash1000)


        val cash5000: ItemStack = ItemUtil.createCustomItem(
            Material.GOLD_INGOT, 1,
            "&a5000원 충전", mutableListOf("&75000 캐시를 충전합니다.", "&e클릭할 때마다 5000씩 증가")
        )
        inv.setItem(4, cash5000)


        val cash10000: ItemStack = ItemUtil.createCustomItem(
            Material.GOLD_BLOCK, 1,
            "&a10000원 충전", mutableListOf("&710000 캐시를 충전합니다.", "&e클릭할 때마다 10000씩 증가")
        )
        inv.setItem(6, cash10000)


        val confirmButton: ItemStack = ItemUtil.createCustomItem(
            Material.EMERALD_BLOCK, 1,
            "&c결제 확정", mutableListOf("&7선택한 금액을 결제합니다.", "&c주의: 실제 결제가 진행됩니다!")
        )
        inv.setItem(8, confirmButton)

        player.openInventory(inv)
    }

    fun purchaseItem(player: Player, category: ShopCategory, itemId: String): Boolean {
        val items: List<ShopItem> = shopItems[category] ?: return false

        val shopItem: ShopItem? = items.stream()
            .filter { item: ShopItem? -> item?.id.equals(itemId) }
            .findFirst()
            .orElse(null)

        if (shopItem == null) return false

        val playerCash: Int = plugin.getCashManager()?.getCash(player.uniqueId) ?: 0
        if (playerCash < shopItem.price) {
            MessageUtil.sendMessage(
                player,
                ("&c캐시가 부족합니다! " + ("필요: ") + (shopItem.price).toString() + ", 보유: " + playerCash)
            )
            return false
        }


        if (itemId == "time_accelerator") {
            if (plugin.getGameManager()?.isTimeAcceleratorActive == true) {
                MessageUtil.sendMessage(player, "&c이미 시간가속기가 활성화되어 있습니다!")
                return false
            }
        }


        plugin.getCashManager()?.subtractCash(player.uniqueId, shopItem.price)


        giveItemToPlayer(player, shopItem)

        MessageUtil.sendMessage(player, ("&a" + shopItem.displayName + "&a을(를) 구매했습니다!"))

        return true
    }

    private fun giveItemToPlayer(player: Player, shopItem: ShopItem?) {
        when (val itemId: String = shopItem?.id ?: "none") {
            "mining_0", "mining_1", "mining_2", "mining_3" -> plugin.getMiningManager()?.setMiningLevel(
                player.uniqueId,
                itemId.substring(itemId.length - 1).toInt()
            )

            "attack_right" -> plugin.getBattleManager()?.grantAttackRight(player.uniqueId, 60) // 60초
            "rabbit_foot" -> plugin.getBattleManager()?.applyPermanentBuff(player, "speed_jump")
            "anvil" -> plugin.getBattleManager()?.applyPermanentBuff(player, "knockback_resistance")
            "iron_sword" -> plugin.getBattleManager()?.applyPermanentBuff(player, "throw_range")
            "time_accelerator" -> plugin.getGameManager()?.activateTimeAccelerator()
            "cannon" -> {
                val cannon = ItemUtil.createCustomItem(
                    Material.TNT, 1, "&c대포 (5/5)",
                    mutableListOf("&7TNT를 5연발 발사합니다.", "&e우클릭으로 사용")
                )
                val meta = cannon.itemMeta
                meta?.setCustomModelData(5)
                cannon.itemMeta = meta
                player.inventory.addItem(cannon)
            }
            "core_durability" -> {
                val team = plugin.getTeamManager()?.getPlayerTeam(player.uniqueId)
                if (team != null) {
                    val core = plugin.getCoreManager()?.getTeamCore(team)
                    if (core != null) {
                        plugin.getCoreManager()?.increaseCoreDurability(core, 10)
                        MessageUtil.sendMessage(player, "&a팀 코어 내구도가 10 증가했습니다!")
                    } else {
                        MessageUtil.sendMessage(player, "&c팀 코어를 찾을 수 없습니다!")
                    }
                } else {
                    MessageUtil.sendMessage(player, "&c팀에 속하지 않았습니다!")
                }
            }
            "crafting_license" -> {
                // 제작권은 영구 소지이므로 인벤토리에 직접 지급
                val licenseItem = ItemUtil.createCustomItem(
                    Material.PAPER, 1, "&a제작권",
                    mutableListOf("&7아이템을 제작할 수 있는 권한을 부여합니다.", "&e영구 소지")
                )
                player.inventory.addItem(licenseItem)
                MessageUtil.sendMessage(player, "&a제작권을 획득했습니다!")
            }


            else -> // 일반 아이템 지급
                if (shopItem?.customItem != null) {
                    player.inventory.addItem(shopItem.customItem!!.clone())
                } else {
                    shopItem?.let { ItemStack(it.material, 1) }?.let { player.inventory.addItem(it) }
                }

        }
    }

    private fun getCategoryName(category: ShopCategory): String {
        return when (category) {
            ShopCategory.ESSENTIALS -> "필수품"
            ShopCategory.BUFFS -> "버프"
            ShopCategory.ITEMS -> "특수 아이템"
        }
    }

    fun getShopItem(category: ShopCategory, itemId: String?): ShopItem? {
        val items: List<ShopItem> = shopItems[category] ?: return null

        return items.firstOrNull { it.id == itemId }
    }
}