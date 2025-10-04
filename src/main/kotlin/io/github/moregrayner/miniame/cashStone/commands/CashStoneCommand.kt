package io.github.moregrayner.miniame.cashStone.commands

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.GameState
import io.github.moregrayner.miniame.cashStone.manager.GameManager
import io.github.moregrayner.miniame.cashStone.utils.LocationUtil.locationToString
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil.sendMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*


class StoneWarCommand(private val plugin: CashStone) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return true
        }

        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase(Locale.getDefault())

        when (subCommand) {
            "start" -> {
                if (!sender.hasPermission("stonewar.admin")) {
                    sendMessage(sender, "&c권한이 없습니다.")
                    return true
                }
                plugin.getGameManager()?.startGame()
            }

            "end" -> {
                if (!sender.hasPermission("stonewar.admin")) {
                    sendMessage(sender, "&c권한이 없습니다.")
                    return true
                }
                plugin.getGameManager()?.forceEndGame()
            }

            "join" -> {
                plugin.getGameManager()?.addParticipant(sender.uniqueId)
                sendMessage(sender, "&a게임에 참가했습니다.")
            }

            "leave" -> {
                plugin.getGameManager()?.removeParticipant(sender.uniqueId)
                sendMessage(sender, "&e게임에서 나갔습니다.")
            }

            "status" -> showGameStatus(sender)
            "setlocation" -> {
                if (args.size < 2) {
                    sendMessage(sender, "&c사용법: /stonewar setlocation <waiting|mining|battle>")
                    return true
                }
                setLocation(sender, args[1])
            }

            else -> showHelp(sender)
        }

        return true
    }

    private fun showHelp(player: Player) {
        sendMessage(player, "&6=== 도움말 ===")
        sendMessage(player, "&e/stonewar join &7- 게임 참가")
        sendMessage(player, "&e/stonewar leave &7- 게임 나가기")
        sendMessage(player, "&e/stonewar status &7- 게임 상태 확인")

        if (player.hasPermission("stonewar.admin")) {
            sendMessage(player, "&c=== 관리자 전용 ===")
            sendMessage(player, "&e/stonewar start &7- 게임 시작")
            sendMessage(player, "&e/stonewar end &7- 게임 강제 종료")
            sendMessage(player, "&e/stonewar setlocation <type> &7- 위치 설정")
        }
    }

    private fun showGameStatus(player: Player) {
        val gameManager: GameManager? = plugin.getGameManager()

        sendMessage(player, "&6=== 게임 상태 ===")
        sendMessage(player, "&e상태: &f" + gameManager?.let { getStateDisplayName(it.getCurrentState()) })
        if (gameManager != null) {
            sendMessage(player, "&e참가자 수: &f" + gameManager.getParticipants().size)
        }

        if ((gameManager?.getCurrentState() ?: GameState.WAITING) === GameState.MINING) {
            val timeLeft: Int = gameManager?.miningTimeLeft ?: return
            sendMessage(player, "&e채굴 시간: &f" + (timeLeft / 60) + "분 " + (timeLeft % 60) + "초")
        } else if ((gameManager?.getCurrentState() ?: GameState.WAITING) === GameState.BATTLE) {
            val timeLeft: Int = gameManager?.battleTimeLeft ?: 0
            sendMessage(player, "&e전투 시간: &f" + (timeLeft / 60) + "분 " + (timeLeft % 60) + "초")
        }

        sendMessage(player, ("&e총 상금: &f" + (plugin.getCashManager()?.totalPrizePool ?: 0) + "원"))
    }

    private fun getStateDisplayName(state: GameState): String {
        return when (state) {
            GameState.WAITING -> "대기 중"
            GameState.MINING -> "채굴 시간"
            GameState.TEAM_SELECTION -> "팀 선택"
            GameState.BATTLE -> "전투"
            GameState.ENDED -> "종료"
            else -> "알 수 없음"
        }
    }

    private fun setLocation(player: Player, locationType: String) {
        if (!player.hasPermission("stonewar.admin")) {
            sendMessage(player, "&c권한이 없습니다.")
            return
        }

        var configPath: String? = null
        var displayName: String? = null

        when (locationType.lowercase(Locale.getDefault())) {
            "waiting" -> {
                configPath = "locations.waiting-room"
                displayName = "대기실"
            }

            "mining" -> {
                configPath = "locations.mining-area"
                displayName = "채굴 구역"
            }

            "battle" -> {
                configPath = "locations.battle-arena"
                displayName = "전투 구역"
            }

            else -> {
                sendMessage(player, "&c올바른 위치 타입: waiting, mining, battle")
                return
            }
        }

        val locationString = locationToString(player.location)
        plugin.config.set(configPath, locationString)
        plugin.saveConfig()

        sendMessage(player, "&a$displayName 위치가 설정되었습니다.")
    }
}