package io.github.moregrayner.miniame.cashStone.listener

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.GameState
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class MiningListener(private val plugin: CashStone) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val blockType = event.block.type
        val currentState = plugin.gameManager?.currentState ?: GameState.WAITING

        if (currentState != GameState.MINING) {
            if (currentState == GameState.BATTLE) {
                val miningLevel = plugin.miningManager?.getMiningLevel(player.uniqueId) ?: 0
                if (miningLevel == 0) {
                    event.isCancelled = true
                    MessageUtil.sendMessage(player, "&f저런! 채굴권이 없군요! 상점에서 채굴권을 구매해 보세요!")
                    return
                }

                if (!isBattleAllowedBlock(blockType)) {
                    event.isCancelled = true
                    return
                }

                if (blockType == Material.COBBLESTONE) {
                    event.isCancelled = true
                    event.block.type = Material.AIR
                    val loc = event.block.location
                    val itemStack = ItemStack(Material.COBBLESTONE, 10)
                    loc.world?.dropItemNaturally(loc, itemStack)
                    return
                }
            } else {
                event.isCancelled = true
                MessageUtil.sendMessage(player, "&f현재는 채굴할 수 없습니다!")
                return
            }
        }

        val miningLevel = plugin.miningManager?.getMiningLevel(player.uniqueId) ?: 0
        if (miningLevel == 0) {
            event.isCancelled = true
            MessageUtil.sendMessage(player, "&f저런! 채굴권이 없군요! 상점에서 채굴권을 구매해 보세요!")
            return
        }

        applyMiningSpeed(player, miningLevel)

        if (isOreBlock(blockType)) {
            plugin.cashManager1?.giveMiningReward(player.uniqueId, blockType)
        }
    }

    private fun applyMiningSpeed(player: Player, miningLevel: Int) {
        player.removePotionEffect(PotionEffectType.HASTE)

        when (miningLevel) {
            1 -> {  }
            2 -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 100, 0, false, false))
            }
            3 -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 100, 1, false, false))
            }
        }
    }

    private fun isOreBlock(material: Material): Boolean {
        return when (material) {
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS -> true
            else -> false
        }
    }

    private fun isBattleAllowedBlock(material: Material): Boolean {
        return when (material) {
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.STONE,
            Material.COBBLESTONE,
            Material.OAK_LOG,
            Material.OAK_LEAVES,
            Material.SAND,
            Material.GRAVEL -> true
            else -> false
        }
    }
}
