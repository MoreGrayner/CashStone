package io.github.moregrayner.miniame.cashStone.utils

import org.bukkit.Bukkit
import org.bukkit.Location


object LocationUtil {
    fun locationToString(location: Location?): String? {
        if (location == null) return null

        return location.world.name + "," +
                location.x + "," +
                location.y + "," +
                location.z + "," +
                location.yaw + "," +
                location.pitch
    }

    fun stringToLocation(locationString: String?): Location? {
        if (locationString == null) return null

        val parts = locationString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size != 6) return null

        return try {
            Location(
                Bukkit.getWorld(parts[0]),
                parts[1].toDouble(),
                parts[2].toDouble(),
                parts[3].toDouble(),
                parts[4].toFloat(),
                parts[5].toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
}