package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.model.Team
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.ArmorStand

class CoreManager(private val plugin: CashStone) {

    private val teamCores: MutableMap<Team, ArmorStand> = HashMap()
    private val coreDurabilities: MutableMap<ArmorStand, Int> = HashMap()
    private val coreBossBars: MutableMap<Team, BossBar> = HashMap()

    fun registerTeamCore(team: Team, stand: ArmorStand, initialHealth: Int = 1000) {
        teamCores[team] = stand
        coreDurabilities[stand] = initialHealth
        updateCoreName(stand)

        // 보스바 생성
        val bossBar = Bukkit.createBossBar(
            "§c${team.name} §f코어 체력",
            BarColor.RED,
            BarStyle.SOLID
        )
        // 모든 온라인 플레이어에게 보이도록
        Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }
        bossBar.progress = 1.0 // 초기 100%
        coreBossBars[team] = bossBar
    }

    fun getTeamCore(team: Team): ArmorStand? = teamCores[team]

    fun getCoreDurability(core: ArmorStand): Int = coreDurabilities[core] ?: 0

    fun increaseCoreDurability(core: ArmorStand, amount: Int) {
        val current = coreDurabilities[core] ?: 0
        coreDurabilities[core] = current + amount
        updateCoreName(core)
        updateBossBar(core)
    }

    fun decreaseCoreDurability(core: ArmorStand, amount: Int) {
        val current = coreDurabilities[core] ?: 0
        val newValue = (current - amount).coerceAtLeast(0)
        coreDurabilities[core] = newValue
        updateCoreName(core)
        updateBossBar(core)

        if (newValue <= 0) destroyCore(core)
    }

    companion object {
        const val MAX_CORE_HEALTH = 1000
    }

    private fun updateBossBar(core: ArmorStand) {
        val team = getTeamByCore(core) ?: return
        val currentHealth = coreDurabilities[core]?.toDouble() ?: 0.0
        coreBossBars[team]?.progress = (currentHealth / MAX_CORE_HEALTH).coerceIn(0.0, 1.0)
        coreBossBars[team]?.setTitle("§c${team.name} §f코어 체력 §7[$currentHealth/$MAX_CORE_HEALTH]")
    }

    internal fun getTeamByCore(core: ArmorStand): Team? = teamCores.entries.find { it.value == core }?.key

    private fun destroyCore(core: ArmorStand) {
        val team = getTeamByCore(core) ?: return
        core.remove()
        teamCores.remove(team)
        coreDurabilities.remove(core)

        // 보스바 제거
        coreBossBars[team]?.removeAll()
        coreBossBars.remove(team)

        Bukkit.broadcastMessage("§c${team.name} §f팀의 코어가 파괴되었습니다!")
        plugin.getGameManager()?.eliminateTeam(team)
    }

    private fun updateCoreName(core: ArmorStand) {
        val baseName = core.customName?.split("§f")?.get(0) ?: "팀"
        val health = coreDurabilities[core] ?: 0
        core.customName = "§c$baseName §f코어 §7[$health]"
    }

    fun isCoreAlive(team: Team): Boolean = getTeamCore(team)?.let { getCoreDurability(it) > 0 } ?: false
}
