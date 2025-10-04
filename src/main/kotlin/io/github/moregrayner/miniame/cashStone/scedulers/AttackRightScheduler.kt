package io.github.moregrayner.miniame.cashStone.scedulers

import io.github.moregrayner.miniame.cashStone.CashStone
import org.bukkit.scheduler.BukkitRunnable


class AttackRightScheduler(private val plugin: CashStone) : BukkitRunnable() {

    override fun run() {
        plugin.getBattleManager()?.checkAttackRights()
    }
}