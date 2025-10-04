package io.github.moregrayner.miniame.cashStone.commands

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.enums.GameState
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AssignCommand(private val plugin: CashStone) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return true
        }

        if (!sender.hasPermission("stonewar.admin")) {
            MessageUtil.sendMessage(sender, "&c권한이 없습니다.")
            return true
        }

        if (args.size != 1) {
            MessageUtil.sendMessage(sender, "&c사용법: /assign <팀 수>")
            return true
        }

        if ((plugin.getGameManager()?.getCurrentState() ?: GameState.MINING) != GameState.TEAM_SELECTION) {
            MessageUtil.sendMessage(sender, "&c팀 선택 단계에서만 사용할 수 있습니다.")
            return true
        }


        try {
            val numTeams = args[0].toInt()

            if (numTeams < 2 || numTeams > 6) {
                MessageUtil.sendMessage(sender, "&c팀 수는 2-6 사이여야 합니다.")
                return true
            }

            plugin.getTeamManager()?.assignCaptains(numTeams)
            MessageUtil.sendMessage(sender, "&a" + numTeams + "개 팀으로 팀장을 배정했습니다.")
        } catch (e: NumberFormatException) {
            MessageUtil.sendMessage(sender, "&c숫자를 입력해주세요!")
        }

        return true
    }
}