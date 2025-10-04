package io.github.moregrayner.miniame.cashStone.commands

import io.github.moregrayner.miniame.cashStone.CashStone
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ShopCommand(private val plugin: CashStone) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return true
        }

        plugin.getShopManager()?.openMainShop(sender)
        return true
    }
}