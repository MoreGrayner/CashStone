package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import io.github.moregrayner.miniame.cashStone.CashStone
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.max

class CashManager(private val plugin: CashStone) {
    private val playerCash: MutableMap<UUID, Int> = HashMap()
    private val pendingCharges: MutableMap<UUID, Int> = HashMap()
    private val totalChargedAmount: MutableMap<UUID, Int> = HashMap() // 추가: 누적 충전액
    var totalPrizePool: Int

    init {
        this.totalPrizePool = 0

        loadCashData()
    }

    private fun loadCashData() {
        if (plugin.cashData?.contains("players") == true) {
            for (uuidString in plugin.cashData!!.getConfigurationSection("players")?.getKeys(false)!!) {
                try {
                    val uuid = UUID.fromString(uuidString)
                    val cash: Int = plugin.cashData!!.getInt("players.$uuidString.cash", 0)
                    val charged: Int = plugin.cashData!!.getInt("players.$uuidString.total-charged", 0)

                    playerCash[uuid] = cash
                    totalChargedAmount[uuid] = charged
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("잘못된 UUID 형식: $uuidString")
                }
            }
        }
        totalPrizePool = plugin.cashData?.getInt("prize-pool", 0) ?: 0
    }

    private fun saveCashData() {
        for ((key, value) in playerCash) {
            val path = "players.$key"
            plugin.cashData?.set("$path.cash", value)
            plugin.cashData?.set("$path.total-charged", totalChargedAmount.getOrDefault(key, 0))

            val player = Bukkit.getPlayer(key)
            if (player != null) {
                plugin.cashData?.set("$path.name", player.name)
                plugin.cashData?.set("$path.last-seen", System.currentTimeMillis())
            }
        }
        plugin.cashData?.set("prize-pool", totalPrizePool)
        plugin.saveCashData()
    }


    fun getCash(playerUuid: UUID): Int {
        return playerCash.getOrDefault(playerUuid, 0)
    }

    internal fun addCash(playerUuid: UUID, amount: Int) {
        val currentCash = getCash(playerUuid)
        playerCash[playerUuid] = currentCash + amount
        saveCashData()
    }

    fun subtractCash(playerUuid: UUID, amount: Int): Boolean {
        val currentCash = getCash(playerUuid)
        if (currentCash < amount) {
            return false
        }

        playerCash[playerUuid] = currentCash - amount
        saveCashData()
        return true
    }

    fun setCash(playerUuid: UUID, amount: Int) {
        playerCash[playerUuid] = max(0.0, amount.toDouble()).toInt()
        saveCashData()
    }

    fun confirmCashCharge(playerUuid: UUID): Boolean {
        val pendingAmount = pendingCharges[playerUuid] ?: return false
        val player = Bukkit.getPlayer(playerUuid) ?: return false

        val paymentSuccess = processRealPayment(player, pendingAmount)

        if (paymentSuccess) {
            addCash(playerUuid, pendingAmount)
            totalPrizePool += pendingAmount

            // 누적 충전액 기록
            val currentCharged = totalChargedAmount.getOrDefault(playerUuid, 0)
            totalChargedAmount[playerUuid] = currentCharged + pendingAmount

            MessageUtil.sendMessage(player, "&a결제가 완료되었습니다!")
            MessageUtil.sendMessage(player, "&e$pendingAmount 캐시가 충전되었습니다.")
            MessageUtil.broadcastMessage("&b${player.name}&f님이 ${pendingAmount}원을 충전했습니다! 총 상금: ${totalPrizePool}원")

            pendingCharges.remove(playerUuid)
            saveCashData()
            return true
        } else {
            MessageUtil.sendMessage(player, "&c결제에 실패했습니다. 다시 시도해주세요.")
            pendingCharges.remove(playerUuid)
            return false
        }
    }

    private fun getTopChargers(): List<Pair<UUID, Int>> {
        return totalChargedAmount.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { Pair(it.key, it.value) }
    }

    fun announceTopChargers() {
        val topChargers = getTopChargers()

        if (topChargers.isEmpty()) {
            MessageUtil.broadcastMessage("&e이번 게임에서는 충전한 플레이어가 없습니다.")
            return
        }

        MessageUtil.broadcastMessage("")
        MessageUtil.broadcastMessage("&6&l========================================")
        MessageUtil.broadcastMessage("&f&l         오늘의 흑우 TOP 3")
        MessageUtil.broadcastMessage("&6&l========================================")
        MessageUtil.broadcastMessage("")

        val medals = listOf("&0와!", "&8와!", "&7와!")

        var totalCharged = 0

        topChargers.forEachIndexed { index, (uuid, amount) ->
            val player = Bukkit.getPlayer(uuid)
            val playerName = player?.name ?: "알 수 없음"
            totalCharged += amount

            MessageUtil.broadcastMessage("${medals[index]} &f${index + 1}위: &b${playerName} &7- &e${amount}원!")
        }

        MessageUtil.broadcastMessage("")
        MessageUtil.broadcastMessage("&6&l========================================")
        MessageUtil.broadcastMessage("&e총 누적 현질 금액: &c${totalCharged}원")
        MessageUtil.broadcastMessage("&6&l========================================")
        MessageUtil.broadcastMessage("")
    }

    fun resetChargeStats() {
        totalChargedAmount.clear()
        saveCashData()
    }

    private fun processRealPayment(player: Player, amount: Int): Boolean {
        plugin.logger.info("[결제] " + player.name + " - " + amount + "원")
        return true
    }

    fun isNewPlayer(playerUuid: UUID): Boolean {
        return !plugin.cashData?.contains("players.$playerUuid")!!
    }

    fun updateChargeAmount(playerUuid: UUID, increment: Int) {
        val currentAmount = pendingCharges.getOrDefault(playerUuid, 0)
        val newAmount = currentAmount + increment
        pendingCharges[playerUuid] = newAmount

        val player = Bukkit.getPlayer(playerUuid)
        if (player != null) {
            MessageUtil.sendMessage(player, "&e충전 금액: ${newAmount}원")
            MessageUtil.sendMessage(player, "&7결제 확정 버튼을 눌러주세요.")
        }
    }

    fun getPendingCharge(playerUuid: UUID): Int {
        return pendingCharges.getOrDefault(playerUuid, 0)
    }

    fun clearPendingCharge(playerUuid: UUID) {
        pendingCharges.remove(playerUuid)
    }

    fun distributeWinnings(winnerTeamName: String?) {
        if (totalPrizePool <= 0) {
            MessageUtil.broadcastMessage("&e상금이 없습니다.")
            return
        }


        // 우승 팀 찾기
        val winners: MutableList<UUID> = ArrayList()
        for (team in plugin.getTeamManager()?.getTeams()!!) {
            if (team.name == winnerTeamName) {
                winners.addAll(team.getMembers())
                break
            }
        }

        if (winners.isEmpty()) {
            MessageUtil.broadcastMessage("&c우승 팀을 찾을 수 없습니다.")
            return
        }


        // 상금을 팀원 수로 나눠서 분배
        val prizePerPlayer = totalPrizePool / winners.size

        MessageUtil.broadcastMessage("&a=== 상금 분배 ===")
        MessageUtil.broadcastMessage("&e총 상금: " + totalPrizePool + "원")
        MessageUtil.broadcastMessage("&e우승 팀원 수: " + winners.size + "명")
        MessageUtil.broadcastMessage("&e1인당 상금: " + prizePerPlayer + "원")

        for (winnerUuid in winners) {
            addCash(winnerUuid, prizePerPlayer)

            val winner = Bukkit.getPlayer(winnerUuid)
            if (winner != null) {
                MessageUtil.sendMessage(winner, "&a축하합니다! " + prizePerPlayer + "원을 획득했습니다!")
                winner.sendTitle("&6우승!", "&e+" + prizePerPlayer + "원", 10, 60, 20)
            }
        }


        // 상금 풀 초기화는 새 게임 시작 시
    }

    fun resetForNewGame() {
        // 새 게임 시작 시 상금 풀만 초기화
        // 플레이어 캐시는 유지
        totalPrizePool = 0
        pendingCharges.clear()
        saveCashData()
    }

    fun giveMiningReward(playerUuid: UUID, oreType: Material?) {
        var reward = 0

        reward = when (oreType) {
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE -> 100
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> 150
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> 250
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> 400
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE -> 300
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE -> 350
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE -> 1000
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE -> 1500
            Material.ANCIENT_DEBRIS -> 3000
            else -> return
        }

        addCash(playerUuid, reward)

        val player = Bukkit.getPlayer(playerUuid)
        if (player != null) {
            MessageUtil.sendMessage(player, "&a+$reward 캐시 (광물 판매)")
        }
    }

    fun showCashInfo(player: Player) {
        val cash = getCash(player.uniqueId)

        MessageUtil.sendMessage(player, "&6=== 캐시 정보 ===")
        MessageUtil.sendMessage(player, "&e보유 캐시: $cash")
        MessageUtil.sendMessage(player, "&e총 상금 풀: " + totalPrizePool + "원")

        if (pendingCharges.containsKey(player.uniqueId)) {
            MessageUtil.sendMessage(player, "&c결제 대기 중: " + pendingCharges[player.uniqueId] + "원")
        }
    }

    fun getTopPlayers(limit: Int): Map<UUID, Int> {
        return playerCash.entries.stream()
            .sorted(java.util.Map.Entry.comparingByValue<UUID, Int>().reversed())
            .limit(limit.toLong())
            .collect(
                { LinkedHashMap() },
                { m: LinkedHashMap<UUID, Int>, e: Map.Entry<UUID, Int> ->
                    m[e.key] = e.value
                },
                { _: LinkedHashMap<UUID, Int>?, _: LinkedHashMap<UUID, Int>? -> })
    }
}