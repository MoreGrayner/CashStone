package io.github.moregrayner.miniame.cashStone.utils

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player


object MessageUtil {
    fun sendMessage(player: Player, message: String?) {
        player.sendMessage(colorize(message))
    }

    fun broadcastMessage(message: String?) {
        Bukkit.broadcastMessage(colorize(message))
    }

    internal fun colorize(message: String?): String {
        return ChatColor.translateAlternateColorCodes('&', message!!)
    }
}