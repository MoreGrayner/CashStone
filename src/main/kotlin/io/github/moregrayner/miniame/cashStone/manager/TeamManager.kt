package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.model.Team
import io.github.moregrayner.miniame.cashStone.utils.ItemUtil
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.*


class TeamManager(private val plugin: CashStone) {

    private val teams: MutableList<Team> = ArrayList()
    private val teamCaptains: Queue<UUID> = LinkedList()
    private var currentPickingCaptain: UUID? = null
    private val eligibleCaptains: MutableList<UUID> = mutableListOf()
    private val availablePlayers: MutableList<UUID> = ArrayList()
    private val playerTeams: MutableMap<UUID, Team> = HashMap()
    private val recruitmentItem: ItemStack = ItemUtil.createCustomItem(
        Material.ENCHANTED_BOOK, 1,
        "&6팀원 모집 표식",
        listOf(
            "&7우클릭으로 팀원을 모집하세요!",
            "&e팀장만 사용 가능합니다."
        )
    )

    fun assignCaptains(numTeams: Int) {
        if ((plugin.getGameManager()?.getParticipants()?.size ?: 0) < numTeams * 2) {
            MessageUtil.broadcastMessage("&c팀을 만들기에 플레이어가 부족합니다!")
            return
        }

        val participants = plugin.gameManager?.participants?.let { ArrayList(it) }
        participants?.shuffle()

        for (i in 0 until numTeams) {
            val captainUuid = participants?.get(i)
            val captain = captainUuid?.let { Bukkit.getPlayer(it) }

            if (captain != null) {
                val team = Team("팀 ${i + 1}", captainUuid)
                teams.add(team)
                teamCaptains.offer(captainUuid)
                playerTeams[captainUuid] = team

                MessageUtil.sendMessage(captain, "&a당신이 ${team.name}의 팀장으로 선정되었습니다!")
            }
        }

        for (i in numTeams until (participants?.size ?: 0 )) {
            participants?.get(i)?.let { availablePlayers.add(it) }
        }

        MessageUtil.broadcastMessage("&e팀장이 선정되었습니다! 팀원 모집을 시작합니다.")
        showTeamStatus()
    }

    fun startTeamSelection() {
        if (teams.isEmpty()) {
            assignCaptains(2)
        }

        if (teamCaptains.isNotEmpty()) {
            startPickingCycle()
        }
    }

    // TeamManager.kt - startPickingCycle 메서드
    private fun startPickingCycle() {
        if (availablePlayers.isEmpty() || teamCaptains.isEmpty()) {
            finishTeamSelection()
            return
        }

        currentPickingCaptain = teamCaptains.poll()
        val captain = Bukkit.getPlayer(currentPickingCaptain!!)

        if (captain == null || !captain.isOnline) {
            startPickingCycle()
            return
        }

        captain.inventory.addItem(recruitmentItem.clone())

        MessageUtil.sendMessage(captain, "&a당신의 차례입니다! 30초 안에 팀원을 선택하세요!")
        MessageUtil.broadcastMessage("&e${captain.name} 팀장이 팀원을 선택 중입니다...")

        showAvailablePlayers()

        // 타임아웃 핸들러
        object : BukkitRunnable() {
            override fun run() {
                // 여전히 이 팀장의 차례인지 확인
                if (currentPickingCaptain != null && currentPickingCaptain == captain.uniqueId) {
                    if (availablePlayers.isNotEmpty()) {
                        // 랜덤 선택
                        val randomPlayer = availablePlayers.random()
                        recruitPlayer(captain.uniqueId, randomPlayer)
                        MessageUtil.broadcastMessage("&c시간 초과! 랜덤으로 선택되었습니다!")
                    }
                    nextTurn()
                }
            }
        }.runTaskLater(plugin, 600L)
    }

    fun recruitPlayer(captainUuid: UUID, targetUuid: UUID): Boolean {
        if (currentPickingCaptain != captainUuid) {
            return false
        }

        if (!availablePlayers.contains(targetUuid)) {
            return false
        }

        val team = playerTeams[captainUuid] ?: return false

        val target = Bukkit.getPlayer(targetUuid)
        val captain = Bukkit.getPlayer(captainUuid)

        if (target == null || captain == null) {
            return false
        }

        team.addMember(targetUuid)
        playerTeams[targetUuid] = team
        availablePlayers.remove(targetUuid)

        captain.inventory.remove(recruitmentItem)

        MessageUtil.sendMessage(target, "&a${team.name}에 선택되었습니다!")
        MessageUtil.broadcastMessage("&e${target.name}이(가) ${team.name}에 선택되었습니다!")

        showTeamStatus()
        nextTurn()

        return true
    }

    private fun nextTurn() {
        val captain = currentPickingCaptain?.let { Bukkit.getPlayer(it) }
        captain?.inventory?.remove(recruitmentItem)

        currentPickingCaptain?.let {
            if (availablePlayers.isNotEmpty()) {
                teamCaptains.offer(it)
            }
        }
        currentPickingCaptain = null

        object : BukkitRunnable() {
            override fun run() {
                startPickingCycle()
            }
        }.runTaskLater(plugin, 20L)
    }

    private fun finishTeamSelection() {
        MessageUtil.broadcastMessage("&a팀 선택이 완료되었습니다!")
        showFinalTeams()

        object : BukkitRunnable() {
            override fun run() {
                plugin.gameManager?.startBattlePhase()
            }
        }.runTaskLater(plugin, 100L)
    }

    private fun showTeamStatus() {
        MessageUtil.broadcastMessage("&6=== 현재 팀 상황 ===")
        for (team in teams) {
            val sb = StringBuilder()
            sb.append("&b").append(team.name).append("&f: ")

            val captain = Bukkit.getPlayer(team.captain)
            if (captain != null) {
                sb.append("&a").append(captain.name).append(" (팀장)")
            }

            for (memberUuid in team.members) {
                if (memberUuid != team.captain) {
                    val member = Bukkit.getPlayer(memberUuid)
                    if (member != null) {
                        sb.append(" &f").append(member.name)
                    }
                }
            }

            MessageUtil.broadcastMessage(sb.toString())
        }
    }

    private fun showAvailablePlayers() {
        val sb = StringBuilder("&e선택 가능한 플레이어: ")
        for (uuid in availablePlayers) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                sb.append("&f").append(player.name).append(" ")
            }
        }
        MessageUtil.broadcastMessage(sb.toString())
    }

    private fun showFinalTeams() {
        MessageUtil.broadcastMessage("&6=== 최종 팀 구성 ===")
        for (team in teams) {
            val sb = StringBuilder()
            sb.append("&b").append(team.name).append(" &f(").append(team.members.size).append("명): ")

            for (memberUuid in team.members) {
                val member = Bukkit.getPlayer(memberUuid)
                if (member != null) {
                    if (memberUuid == team.captain) {
                        sb.append("&a").append(member.name).append("(팀장) ")
                    } else {
                        sb.append("&f").append(member.name).append(" ")
                    }
                }
            }

            MessageUtil.broadcastMessage(sb.toString())
        }
    }

    fun teleportTeamsToBattle(battleArena: Location?, coreManager: CoreManager) {
        if (battleArena == null) return

        val random = Random()
        val spawnRange = 100 //+-

        for (team in teams) {
            val x = battleArena.x + random.nextInt(spawnRange * 2) - spawnRange
            val z = battleArena.z + random.nextInt(spawnRange * 2) - spawnRange

            var y = battleArena.y
            val loc = battleArena.world?.getHighestBlockYAt(x.toInt(), z.toInt())?.toDouble() ?: y
            y = loc - 5

            val teamSpawn = Location(battleArena.world, x, y, z)
            teamSpawn.yaw = random.nextFloat() * 360

            for (dx in -10..10) {
                for (dz in -4..4) {
                    for (dy in -2..0) {
                        val block = teamSpawn.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble()).block
                        block.type = org.bukkit.Material.GRASS_BLOCK
                    }
                }
            }

            for (memberUuid in team.members) {
                Bukkit.getPlayer(memberUuid)?.let { member ->
                    if (member.isOnline) {
                        member.teleport(teamSpawn)
                        member.sendTitle("&c전투 시작!", "&e${team.name}", 10, 40, 10)
                    }
                }
            }

            val coreLoc = teamSpawn.clone().add(0.0, 20.0, 0.0)
            val stand = coreLoc.world?.spawn(coreLoc, ArmorStand::class.java) { armorStand ->
                armorStand.isInvisible = false
                armorStand.isCustomNameVisible = true
                armorStand.customName = "§c${team.name} §f코어"
                armorStand.isMarker = true
                armorStand.equipment.helmet = ItemStack(org.bukkit.Material.DIAMOND_BLOCK)
            }

            stand?.let { coreManager.registerTeamCore(team, it) }
        }
    }

    fun determineWinner(): String {
        var winner: Team? = null
        var maxAlive = 0

        for (team in teams) {
            var aliveCount = 0
            for (memberUuid in team.members) {
                val member = Bukkit.getPlayer(memberUuid)
                if (member != null && member.isOnline && !member.isDead) {
                    aliveCount++
                }
            }

            if (aliveCount > maxAlive) {
                maxAlive = aliveCount
                winner = team
            }
        }

        return if (winner != null) {
            "${winner.name} (생존자 :$maxAlive 명)"
        } else {
            "무승부"
        }
    }

    fun skipCurrentCaptainTurn() {
        if (currentPickingCaptain == null) return

        eligibleCaptains.remove(currentPickingCaptain)
        currentPickingCaptain = eligibleCaptains.firstOrNull()
        currentPickingCaptain?.let { announceNewCaptain(it) }
    }

    private fun announceNewCaptain(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        player?.let {
            MessageUtil.sendMessage(it, "&e당신이 새로운 팀장으로 지정되었습니다!")
        }
    }

    fun reset() {
        teams.clear()
        teamCaptains.clear()
        availablePlayers.clear()
        playerTeams.clear()
        currentPickingCaptain = null
    }

    fun getTeams(): List<Team> {
        return teams
    }

    fun getPlayerTeam(playerUuid: UUID): Team? {
        return playerTeams[playerUuid]
    }

    fun isCurrentPickingCaptain(playerUuid: UUID): Boolean {
        return currentPickingCaptain != null && currentPickingCaptain == playerUuid
    }

    fun isTeamCaptain(playerUuid: UUID): Boolean {
        val team = playerTeams[playerUuid]
        return team != null && team.captain == playerUuid
    }

    fun getRecruitmentItem(): ItemStack {
        return recruitmentItem
    }

    fun removeTeam(team: Team) {
        teams.remove(team)
        for (memberUuid in team.members) {
            playerTeams.remove(memberUuid)
        }
        teamCaptains.remove(team.captain)
    }
}
