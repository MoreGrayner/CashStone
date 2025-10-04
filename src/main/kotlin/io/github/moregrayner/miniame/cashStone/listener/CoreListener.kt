package io.github.moregrayner.miniame.cashStone.listener

import io.github.moregrayner.miniame.cashStone.manager.CoreManager
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent

class CoreListener(private val coreManager: CoreManager) : Listener {

    @EventHandler
    fun onCoreDamage(event: EntityDamageByEntityEvent) {
        val core = event.entity as? ArmorStand ?: return
        val team = coreManager.getTeamByCore(core) ?: return
        val damager = event.damager
        if (damager is Player) {
            event.isCancelled = true
            coreManager.decreaseCoreDurability(core, 10)
            damager.sendMessage("${team.name} : ${coreManager.getCoreDurability(core)}")
        }
    }
}