package io.github.moregrayner.miniame.cashStone.listener

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.GameState
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

class PlayerListener(private val plugin: CashStone) : Listener {

    // PlayerListener.kt - onPlayerJoin 수정
    // PlayerListener.kt - onPlayerJoin 수정
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // cash.yml에 기록이 없는 신규 플레이어만 지급
        if (plugin.getCashManager()?.isNewPlayer(playerUuid) == true) {
            plugin.getCashManager()?.addCash(playerUuid, 500)
            MessageUtil.sendMessage(player, "&a첫 접속 보너스 500 캐시가 지급되었습니다!")
        }

        plugin.gameManager?.addParticipant(playerUuid)
        plugin.gameManager?.waitingRoom?.let { player.teleport(it) }
        resetPlayer(player)
        MessageUtil.sendMessage(player, "&aStoneWar에 오신 것을 환영합니다!")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        plugin.gameManager?.removeParticipant(playerUuid)
        plugin.teamManager?.getPlayerTeam(playerUuid)?.removeMember(playerUuid)

        if (plugin.teamManager!!.isCurrentPickingCaptain(playerUuid)) {
            plugin.teamManager!!.skipCurrentCaptainTurn()
        }
    }


    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        val target = event.rightClicked as? Player ?: return

        when {
            item.isNamed("팀원 모집") && (plugin.gameManager?.currentState
                ?: GameState.WAITING) == GameState.TEAM_SELECTION -> {
                event.isCancelled = true
                if (plugin.teamManager?.recruitPlayer(player.uniqueId, target.uniqueId) == true) {
                    MessageUtil.sendMessage(player, "&a${target.name}을(를) 팀원으로 모집했습니다!")
                } else {
                    MessageUtil.sendMessage(player, "&c모집할 수 없습니다!")
                }
            }

            // 표식 사용
            item.isNamed("표식") && (plugin.gameManager?.currentState ?: GameState.WAITING) == GameState.BATTLE -> {
                event.isCancelled = true

                val playerTeam = plugin.teamManager?.getPlayerTeam(player.uniqueId)
                val targetTeam = plugin.teamManager?.getPlayerTeam(target.uniqueId)

                if (playerTeam != null && playerTeam == targetTeam) {
                    MessageUtil.sendMessage(player, "&c같은 팀원에게는 사용할 수 없습니다!")
                    return
                }

                val direction = target.location.toVector().subtract(player.location.toVector()).normalize()
                plugin.battleManager?.applyMarkerKnockback(target, direction)
                consumeItem(player, item)
                MessageUtil.sendMessage(player, "&a${target.name}에게 표식을 사용했습니다!")
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        plugin.battleManager?.handlePlayerDeath(player)
        if ((plugin.gameManager?.currentState ?: GameState.WAITING) == GameState.BATTLE) checkForGameEnd()
        event.deathMessage = "§c${player.name}님이 숨졌습니다!"
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager !is Snowball) return
        val snowball = event.damager as Snowball
        if (snowball.shooter !is Player || event.entity !is Player) return
        val shooter1 = snowball.shooter as Player
        val target = event.entity as Player

        val shooterTeam = plugin.teamManager?.getPlayerTeam(shooter1.uniqueId)
        val targetTeam = plugin.teamManager?.getPlayerTeam(target.uniqueId)

        if (shooterTeam != null && shooterTeam == targetTeam) {
            event.isCancelled = true
            MessageUtil.sendMessage(shooter1, "&c같은 팀원을 공격할 수 없습니다.")
            return
        }

        event.damage = 1.0
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        when {
            item.type == Material.NETHER_STAR -> {
                event.isCancelled = true
                plugin.shopManager?.openMainShop(player)
            }

            event.action.isRightClick && (plugin.gameManager?.currentState
                ?: GameState.WAITING) == GameState.BATTLE -> {
                when {
                    item.isCannon() -> {
                        event.isCancelled = true
                        plugin.battleManager?.useCannon(player)
                    }
                    item.isWindCharge() -> {
                        event.isCancelled = true
                        val targetLoc = player.getTargetBlock(null, 10).location
                        plugin.battleManager?.applyWindCharge(player, targetLoc)
                        consumeItem(player, item)
                    }

                    else -> {
                        plugin.battleManager?.throwStone(player)
                    }
                }
            }
        }
    }

    private fun resetPlayer(player: Player) {
        player.gameMode = GameMode.SURVIVAL
        player.inventory.clear()
        player.health = 20.0
        player.foodLevel = 20
    }

    private fun consumeItem(player: Player, item: ItemStack) {
        if (item.amount > 1) item.amount-- else player.inventory.remove(item)
    }

    private fun ItemStack.isNamed(name: String): Boolean {
        return this.type == Material.ENCHANTED_BOOK && this.hasItemMeta() &&
                this.itemMeta?.displayName?.contains(name) == true
    }

    private fun ItemStack.isCannon(): Boolean {
        return this.type == Material.TNT || (this.hasItemMeta() && this.itemMeta?.customModelData == 1)
    }

    private fun ItemStack.isWindCharge(): Boolean {
        return this.type.name == "WIND_CHARGE"
    }

    private fun checkForGameEnd() {
        val aliveTeams = plugin.teamManager?.getTeams()?.filter { team ->
            plugin.getCoreManager()?.isCoreAlive(team) == true &&
                    team.members.any { memberUuid ->
                        val member = Bukkit.getPlayer(memberUuid)
                        member != null && member.isOnline && !member.isDead
                    }
        } ?: emptyList()

        if (aliveTeams.size <= 1) {
            val winner = aliveTeams.firstOrNull()?.name ?: "무승부!"
            plugin.gameManager?.endGame("최후의 팀: $winner")
        }
    }
}
