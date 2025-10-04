package io.github.moregrayner.miniame.cashStone.commands

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.model.Team
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil.sendMessage
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*


class TeamCommand(private val plugin: CashStone) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return true
        }

        if (args.isEmpty()) {
            showTeamInfo(sender)
            return true
        }

        val subCommand = args[0].lowercase(Locale.getDefault())

        when (subCommand) {
            "info" -> showTeamInfo(sender)
            "appeal" -> {
                if (args.size < 2) {
                    sendMessage(sender, "&c사용법: /team appeal <메시지>")
                    return true
                }
                appealToTeam(sender, java.lang.String.join(" ", *Arrays.copyOfRange(args, 1, args.size)))
            }

            else -> sendMessage(sender, "&c사용법: /team [info|appeal]")
        }

        return true
    }

    private fun showTeamInfo(player: Player) {
        val team: Team? = plugin.getTeamManager()?.getPlayerTeam(player.uniqueId)

        if (team == null) {
            sendMessage(player, "&c아직 팀에 속하지 않았습니다.")
            return
        }

        sendMessage(player, "&6=== 팀 정보 ===")
        sendMessage(player, "&e팀 이름: &f" + team.name)

        val captain: Player? = Bukkit.getPlayer(team.captain)
        if (captain != null) {
            sendMessage(player, "&e팀장: &a" + captain.name)
        }

        sendMessage(player, "&e팀원 목록:")
        for (memberUuid in team.getMembers()) {
            val member: Player? = Bukkit.getPlayer(memberUuid)
            if (member != null) {
                val role = if (memberUuid == team.captain) " (팀장)" else ""
                sendMessage(player, "&f- " + member.name + role)
            }
        }
    }

    private fun appealToTeam(player: Player, message: String) {
        var sent = false

        for (team in plugin.getTeamManager()?.getTeams()!!) {
            val captain: Player? = Bukkit.getPlayer(team.captain)
            if (captain != null && captain.isOnline) {
                captain.playSound(captain.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                sendMessage(captain, "&6[팀원 어필] &f" + player.name + "&7: " + message)
                sent = true
            }
        }

        if (!sent) {
            sendMessage(player, "&c현재 온라인 상태인 팀장이 없습니다.")
        }
    }
}