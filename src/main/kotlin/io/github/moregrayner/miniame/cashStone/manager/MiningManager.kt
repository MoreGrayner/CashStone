package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class MiningManager(cashStone: CashStone) {

    private val miningLevels: MutableMap<UUID, Int> = HashMap()

    fun getMiningLevel(playerUuid: UUID): Int {
        return miningLevels.getOrDefault(playerUuid, 0)
    }

    fun setMiningLevel(playerUuid: UUID, level: Int) {
        miningLevels[playerUuid] = level.coerceIn(0, 3)

        val player: Player? = Bukkit.getPlayer(playerUuid)
        if (player != null) {
            val levelName = getLevelName(level)
            MessageUtil.sendMessage(player, "&a채굴권이 $levelName 로 업그레이드되었습니다!")
        }
    }

    private fun getLevelName(level: Int): String {
        return when (level) {
            0 -> "없음 (채굴 불가)"
            1 -> "1단계 (일반 채굴)"
            2 -> "2단계 (성급함 1)"
            3 -> "3단계 (성급함 2)"
            else -> "알 수 없음"
        }
    }

    fun reset() {
        miningLevels.clear()
    }
}
