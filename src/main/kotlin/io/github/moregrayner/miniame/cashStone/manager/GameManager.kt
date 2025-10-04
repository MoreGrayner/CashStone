package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.GameState
import io.github.moregrayner.miniame.cashStone.model.Team
import io.github.moregrayner.miniame.cashStone.utils.LocationUtil
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class GameManager(private val plugin: CashStone){
    internal var currentState: GameState
    var miningTimeLeft: Int = 0
        private set
    var battleTimeLeft: Int = 0
        private set
    var waitingRoom: Location? = null
        private set
    private var miningArea: Location? = null
    private var battleArena: Location? = null
    internal var participants: MutableSet<UUID> = HashSet()
    private var currentTickRate: Double
    var isTimeAcceleratorActive: Boolean = false
        private set

    init {
        this.currentState = GameState.WAITING
        this.participants = HashSet()
        this.currentTickRate = 2.0 // 기본 10배 느림 (20 ticks = 1초, 2.0 = 10배 느림)
        this.isTimeAcceleratorActive = false

        loadLocations()
    }

    private fun loadLocations() {
        if (plugin.config.contains("locations.waiting-room")) {
            waitingRoom = LocationUtil.stringToLocation(
                plugin.config.getString("locations.waiting-room")
            )
        }
        if (plugin.config.contains("locations.mining-area")) {
            miningArea = LocationUtil.stringToLocation(
                plugin.config.getString("locations.mining-area")
            )
        }
        if (plugin.config.contains("locations.battle-arena")) {
            battleArena = LocationUtil.stringToLocation(
                plugin.config.getString("locations.battle-arena")
            )
        }
    }


    fun startGame() {
        if (currentState !== GameState.WAITING) {
            return
        }

        if (participants.size < 2) {
            MessageUtil.broadcastMessage("&c최소 2명의 플레이어가 필요합니다!")
            return
        }

        currentState = GameState.MINING
        miningTimeLeft = plugin.config.getInt("game.mining-time", 1800) // 20분


        teleportParticipantsToMining()


        // 채광 시간 시작
        startMiningPhase()

        MessageUtil.broadcastMessage("&a게임이 시작되었습니다! 채광 시간: " + (miningTimeLeft / 60) + "분")
    }

    private fun startMiningPhase() {
        object : BukkitRunnable() {
            override fun run() {
                if (currentState !== GameState.MINING) {
                    cancel()
                    return
                }

                miningTimeLeft--

                if (miningTimeLeft == 300 || miningTimeLeft == 180 || miningTimeLeft == 60 || miningTimeLeft <= 10) {
                    if (miningTimeLeft > 60) {
                        MessageUtil.broadcastMessage("&e채광 시간 " + (miningTimeLeft / 60) + "분 남음!")
                    } else if (miningTimeLeft > 10) {
                        MessageUtil.broadcastMessage("&e채광 시간 " + miningTimeLeft + "초 남음!")
                    } else if (miningTimeLeft > 0) {
                        MessageUtil.broadcastMessage("&c" + miningTimeLeft + "초!")
                    }
                }

                if (miningTimeLeft <= 0) {
                    endMiningPhase()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, tickInterval) // 여기 수정
    }

    private fun endMiningPhase() {
        currentState = GameState.TEAM_SELECTION

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                player.inventory.clear()
                if (waitingRoom != null) {
                    player.teleport(waitingRoom!!)
                }
            }
        }

        MessageUtil.broadcastMessage("&a채광 시간이 종료되었습니다! 팀 선택을 시작합니다.")


        // 팀 선택 시작
        plugin.getTeamManager()?.startTeamSelection()
    }

    fun startBattlePhase() {
        if (currentState !== GameState.TEAM_SELECTION) {
            return
        }

        currentState = GameState.BATTLE
        battleTimeLeft = plugin.config.getInt("game.battle-time", 1800) // 20분


        teleportTeamsToBattle()


        startBattleTimer()

        MessageUtil.broadcastMessage("&c전투가 시작되었습니다! 전투 시간: " + (battleTimeLeft / 60) + "분")
    }

    private fun startBattleTimer() {
        object : BukkitRunnable() {
            override fun run() {
                if (currentState !== GameState.BATTLE) {
                    cancel()
                    return
                }

                battleTimeLeft--

                if (battleTimeLeft == 600 || battleTimeLeft == 300 || battleTimeLeft == 60 || battleTimeLeft <= 10) {
                    if (battleTimeLeft > 60) {
                        MessageUtil.broadcastMessage("&e전투 시간 " + (battleTimeLeft / 60) + "분 남음!")
                    } else if (battleTimeLeft > 10) {
                        MessageUtil.broadcastMessage("&e전투 시간 " + battleTimeLeft + "초 남음!")
                    } else if (battleTimeLeft > 0) {
                        MessageUtil.broadcastMessage("&c" + battleTimeLeft + "초!")
                    }
                }

                if (battleTimeLeft <= 0) {
                    endGame("시간 만료")
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, tickInterval) // 여기 수정
    }

    fun endGame(reason: String) {
        if (currentState === GameState.ENDED) {
            return
        }

        currentState = GameState.ENDED

        val winner: String = plugin.getTeamManager()?.determineWinner().toString()

        MessageUtil.broadcastMessage("&a=== 게임 종료 ===")
        MessageUtil.broadcastMessage("&e종료 사유: $reason")
        MessageUtil.broadcastMessage("&b우승: $winner")

        // 상금 분배
        plugin.getCashManager()?.distributeWinnings(winner)

        // 흑우 TOP 3 발표 추가
        object : BukkitRunnable() {
            override fun run() {
                plugin.getCashManager()?.announceTopChargers()
            }
        }.runTaskLater(plugin, 100L) // 5초 후

        // 게임 리셋
        resetGame()
    }


    fun forceEndGame() {
        if (currentState !== GameState.ENDED) {
            endGame("강제 종료")
        }
    }

    private fun resetGame() {
        object : BukkitRunnable() {
            override fun run() {
                this@GameManager.isTimeAcceleratorActive = false
                this@GameManager.currentState = GameState.WAITING
                this@GameManager.participants.clear()
                this@GameManager.miningTimeLeft = 0
                this@GameManager.battleTimeLeft = 0
                this@GameManager.currentTickRate = 2.0

                plugin.getTeamManager()?.reset()
                plugin.getCashManager()?.resetForNewGame()


                // 모든 플레이어를 대기실로
                for (player in Bukkit.getOnlinePlayers()) {
                    if (waitingRoom != null) {
                        player.teleport(waitingRoom!!)
                    }
                    player.gameMode = GameMode.SURVIVAL
                    player.inventory.clear()
                    player.health = 20.0
                    player.foodLevel = 20
                }

                plugin.getCashManager()?.resetForNewGame()
                plugin.getCashManager()?.resetChargeStats() // 충전 통계 초기화

                MessageUtil.broadcastMessage("&a새 게임을 시작할 수 있습니다!")
            }
        }.runTaskLater(plugin, 100L) // 5초 후
    }

    private fun teleportParticipantsToMining() {
        if (miningArea == null) return

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                player.teleport(miningArea!!)
                player.gameMode = GameMode.SURVIVAL
                player.inventory.clear()
                player.health = 20.0
                player.foodLevel = 20
            }
        }
    }

    private fun teleportTeamsToBattle() {
        if (battleArena == null) return
        val coreManager = plugin.getCoreManager() ?: return

        plugin.getTeamManager()?.teleportTeamsToBattle(battleArena, coreManager)
    }

    fun activateTimeAccelerator() {
        if (!isTimeAcceleratorActive) {
            isTimeAcceleratorActive = true
            currentTickRate = 1.0
            MessageUtil.broadcastMessage("&e시간가속기가 활성화되었습니다!")
        }
    }

    private val tickInterval: Long
        get() = (20L * currentTickRate).toLong()

    // Getters & Setters
    fun getCurrentState(): GameState {
        return currentState
    }

    fun setCurrentState(state: GameState) {
        this.currentState = state
    }

    fun getParticipants(): Set<UUID> {
        return participants
    }

    fun addParticipant(uuid: UUID) {
        participants.add(uuid)
    }

    fun removeParticipant(uuid: UUID) {
        participants.remove(uuid)
    }

    fun isParticipant(uuid: UUID): Boolean {
        return participants.contains(uuid)
    }

    fun eliminateTeam(team: Team) {
        for (uuid in team.getMembers()) {
            participants.remove(uuid)
        }

        // 팀 관련 처리 (예: 코어 제거 등)
        plugin.getTeamManager()?.removeTeam(team)

        MessageUtil.broadcastMessage("§c${team.name} 팀이 게임에서 제거되었습니다!")
    }
}
