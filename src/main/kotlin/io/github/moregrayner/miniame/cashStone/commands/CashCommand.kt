package io.github.moregrayner.miniame.cashStone.commands

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil.sendMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*


class CashCommand(private val plugin: CashStone) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return true
        }

        if (args.isEmpty()) {
            plugin.getCashManager()?.showCashInfo(sender)
            return true
        }

        val subCommand = args[0].lowercase(Locale.getDefault())

        if (!sender.hasPermission("stonewar.admin")) {
            sendMessage(sender, "&c권한이 없습니다.")
            return true
        }

        when (subCommand) {
            "give" -> {
                if (args.size < 3) {
                    sendMessage(sender, "&c사용법: /cash give <플레이어> <금액>")
                    return true
                }
                giveCash(sender, args[1], args[2])
            }

            "set" -> {
                if (args.size < 3) {
                    sendMessage(sender, "&c사용법: /cash set <플레이어> <금액>")
                    return true
                }
                setCash(sender, args[1], args[2])
            }

            "top" -> showTopPlayers(sender)
            else -> sendMessage(sender, "&c사용법: /cash [give|set|top] [플레이어] [금액]")
        }

        return true
    }

    private fun giveCash(admin: Player, targetName: String, amountStr: String) {
        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sendMessage(admin, "&c플레이어를 찾을 수 없습니다.")
            return
        }

        try {
            val amount = amountStr.toInt()
            plugin.getCashManager()?.addCash(target.uniqueId, amount)

            sendMessage(admin, "&a" + target.name + "에게 " + amount + " 캐시를 지급했습니다.")
            sendMessage(target, "&a관리자로부터 $amount 캐시를 받았습니다.")
        } catch (e: NumberFormatException) {
            sendMessage(admin, "&c올바른 숫자를 입력해주세요.")
        }
    }

    private fun setCash(admin: Player, targetName: String, amountStr: String) {
        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sendMessage(admin, "&c플레이어를 찾을 수 없습니다.")
            return
        }

        try {
            val amount = amountStr.toInt()
            plugin.getCashManager()?.setCash(target.uniqueId, amount)

            sendMessage(admin, "&a" + target.name + "의 캐시를 " + amount + "로 설정했습니다.")
            sendMessage(target, "&e캐시가 " + amount + "로 설정되었습니다.")
        } catch (e: NumberFormatException) {
            sendMessage(admin, "&c올바른 숫자를 입력해주세요.")
        }
    }

    private fun showTopPlayers(player: Player) {
        val topPlayers: Map<UUID, Int> = plugin.getCashManager()?.getTopPlayers(10) ?: return
        var rank = 1
        sendMessage(player, "&6=== 캐시 랭킹 ===")
        for ((uuid, cash) in topPlayers) {
            val p: Player? = Bukkit.getPlayer(uuid)
            val name = p?.name ?: "Unknown"
            sendMessage(player, "&e$rank. &f$name &7- &a$cash 캐시")
            rank++
        }
    }
}
