package io.github.moregrayner.miniame.cashStone.manager


import io.github.moregrayner.miniame.cashStone.CashStone
import io.github.moregrayner.miniame.cashStone.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.TNTPrimed
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.max

class BattleManager(private val plugin: CashStone) {
    private val attackRightExpiry: MutableMap<UUID, Long> = HashMap()
    private val permanentBuffs: MutableMap<UUID, MutableSet<String>> =
        HashMap()
    private val cannonUses: MutableMap<UUID, Int> = HashMap()
    private val playersWithThrowRange: MutableSet<UUID> = HashSet()

    private val attackRightTasks: MutableMap<UUID, Int> = HashMap()

    // BattleManager.kt - grantAttackRight 수정
    fun grantAttackRight(playerUuid: UUID, durationSec: Int = 60) {
        // 기존 구독 취소
        attackRightTasks[playerUuid]?.let { Bukkit.getScheduler().cancelTask(it) }

        // 초기 공격권 부여
        attackRightExpiry[playerUuid] = System.currentTimeMillis() + (durationSec * 1000L)

        val player = Bukkit.getPlayer(playerUuid)
        if (player != null) {
            MessageUtil.sendMessage(player, "&a공격권 구독이 시작되었습니다!")
            MessageUtil.sendMessage(player, "&e60초마다 300 캐시가 차감됩니다.")
        }

        val taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, Runnable {
            val onlinePlayer = Bukkit.getPlayer(playerUuid) ?: run {
                removeAttackRight(playerUuid)
                return@Runnable
            }

            val subscriptionCost = 300
            val currentCash = plugin.getCashManager()?.getCash(playerUuid) ?: 0

            if (currentCash >= subscriptionCost) {
                if (plugin.getCashManager()?.subtractCash(playerUuid, subscriptionCost) == true) {
                    attackRightExpiry[playerUuid] = System.currentTimeMillis() + (durationSec * 1000L)
                    MessageUtil.sendMessage(onlinePlayer, "&a공격권이 갱신되었습니다. (잔액: ${currentCash - subscriptionCost})")
                } else {
                    cancelSubscription(playerUuid, "결제 처리 실패")
                }
            } else {
                cancelSubscription(playerUuid, "저런! 잔액이 부족해서 구독이 취소되었어요! (필요: $subscriptionCost, 보유: $currentCash)")
            }
        }, 20L * durationSec, 20L * durationSec)

        attackRightTasks[playerUuid] = taskId
    }

    private fun cancelSubscription(playerUuid: UUID, reason: String) {
        attackRightTasks[playerUuid]?.let {
            Bukkit.getScheduler().cancelTask(it)
        }
        attackRightTasks.remove(playerUuid)

        attackRightExpiry.remove(playerUuid)

        val player = Bukkit.getPlayer(playerUuid)
        if (player != null) {
            MessageUtil.sendMessage(player, "&c공격권 구독이 취소되었습니다.")
            MessageUtil.sendMessage(player, "&c사유: $reason")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        }
    }

    fun hasAttackRight(playerUuid: UUID): Boolean {
        val expiry = attackRightExpiry[playerUuid] ?: return false

        if (System.currentTimeMillis() > expiry) {
            attackRightExpiry.remove(playerUuid)
            return false
        }

        return true
    }

    fun removeAttackRight(playerUuid: UUID) {
        attackRightExpiry.remove(playerUuid)

        val player = Bukkit.getPlayer(playerUuid)
        if (player != null) {
            MessageUtil.sendMessage(player, "&c공격권이 만료되었습니다.")
        }
    }

    fun applyPermanentBuff(player: Player, buffType: String) {
        val playerUuid = player.uniqueId
        val buffs = permanentBuffs.computeIfAbsent(playerUuid) { _: UUID? -> HashSet() }

        if (buffs.contains(buffType)) {
            MessageUtil.sendMessage(player, "&c이미 해당 버프를 보유하고 있습니다!")
            return
        }

        buffs.add(buffType)

        when (buffType) {
            "speed_jump" -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 1, false, false))
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, Int.MAX_VALUE, 1, false, false))
                MessageUtil.sendMessage(player, "&a버프 구매됨: [토끼발]")
            }

            "knockback_resistance" -> {
                MessageUtil.sendMessage(player, "&a버프 구매됨: [부동세]")
            }

            "throw_range" -> {
                playersWithThrowRange.add(playerUuid)
                MessageUtil.sendMessage(player, "&a힘 버프가 적용되었습니다! (투척 사정거리 1.5배)")
            }
        }
    }



    fun throwStone(player: Player): Boolean {
        val playerUuid = player.uniqueId


        // 공격권 확인
        if (!hasAttackRight(playerUuid)) {
            MessageUtil.sendMessage(player, "&f저런! 공격권이 없군요! 구독해서 공격할 수 있는 권한을 구매해 보세요!")
            return false
        }


        // 돌 투척
        val eyeLoc = player.eyeLocation
        val direction = eyeLoc.direction


        // 사정거리 계산 (기본 + 힘 버프)
        val multiplier = if (playersWithThrowRange.contains(playerUuid)) 1.5 else 1.0
        direction.multiply(2.0 * multiplier) // 기본 투척력의 1.5배

        val stone = player.world.spawn(eyeLoc, Snowball::class.java)
        stone.velocity = direction
        stone.shooter = player


        // 20블록 낙차 설정을 위한 중력 조정
        object : BukkitRunnable() {
            var ticks: Int = 0
            override fun run() {
                if (stone.isDead || !stone.isValid) {
                    cancel()
                    return
                }

                val velocity = stone.velocity
                velocity.setY(velocity.y - 0.5) // 중력 강화로 20블록 낙차 구현
                stone.velocity = velocity

                ticks++
                if (ticks > 200) { // 10초 후 자동 제거
                    stone.remove()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)

        return true
    }

    fun useCannon(player: Player): Boolean {
        val playerUuid = player.uniqueId

        // 손에 든 아이템 확인
        val cannonItem = player.inventory.itemInMainHand
        if (!cannonItem.hasItemMeta() || cannonItem.itemMeta?.customModelData == null) {
            MessageUtil.sendMessage(player, "&c대포가 아닙니다!")
            return false
        }

        val remainingUses = cannonItem.itemMeta!!.customModelData
        if (remainingUses <= 0) {
            MessageUtil.sendMessage(player, "&c대포를 모두 사용했습니다!")
            player.inventory.remove(cannonItem)
            return false
        }

        // 공격권 확인
        if (!hasAttackRight(playerUuid)) {
            MessageUtil.sendMessage(player, "&f저런! 공격권이 없군요! 구독해서 공격할 수 있는 권한을 구매해 보세요!")
            return false
        }

        // TNT 발사
        val eyeLoc = player.eyeLocation
        val direction = eyeLoc.direction

        val baseMultiplier = if (playersWithThrowRange.contains(playerUuid)) 1.5 else 1.0
        val cannonMultiplier = baseMultiplier * 1.5

        for (i in 0..4) {
            object : BukkitRunnable() {
                override fun run() {
                    val tntDirection = direction.clone()
                    tntDirection.multiply(1.8 * cannonMultiplier)

                    tntDirection.add(
                        Vector(
                            (Math.random() - 0.5) * 0.2,
                            (Math.random() - 0.5) * 0.1,
                            (Math.random() - 0.5) * 0.2
                        )
                    )

                    val tnt = player.world.spawn(eyeLoc.clone(), TNTPrimed::class.java)
                    tnt.velocity = tntDirection
                    tnt.fuseTicks = 80
                }
            }.runTaskLater(plugin, i * 5L)
        }

        // 남은 횟수 감소 및 업데이트
        val newRemaining = remainingUses - 1
        updateCannonDisplay(cannonItem, newRemaining)

        MessageUtil.sendMessage(player, "&c대포 발사! ($newRemaining/5)")

        return true
    }

    private fun updateCannonDisplay(item: ItemStack, remaining: Int) {
        val meta = item.itemMeta ?: return

        meta.setCustomModelData(remaining)

        when (remaining) {
            5 -> {
                item.type = Material.TNT
                meta.setDisplayName("§c대포 (5/5)")
            }
            4 -> {
                item.type = Material.FIRE_CHARGE
                meta.setDisplayName("§c대포 (4/5)")
            }
            3 -> {
                item.type = Material.FIREWORK_ROCKET
                meta.setDisplayName("§c대포 (3/5)")
            }
            2 -> {
                item.type = Material.GUNPOWDER
                meta.setDisplayName("§c대포 (2/5)")
            }
            1 -> {
                item.type = Material.SUGAR
                meta.setDisplayName("§c대포 (1/5)")
            }
            0 -> {
                item.type = Material.BARRIER
                meta.setDisplayName("§c대포 (0/5) - 소진됨")
            }
        }

        item.itemMeta = meta
    }

    fun handlePlayerDeath(player: Player) {
        val playerUuid = player.uniqueId


        // 공격권 제거
        if (attackRightExpiry.containsKey(playerUuid)) {
            removeAttackRight(playerUuid)
        }


        // 아이템 절반 드롭 (네더의 별 제외)
        val inventory = player.inventory
        val toDrop: MutableList<ItemStack> = ArrayList()

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item != null && item.type != Material.NETHER_STAR) {
                if (Math.random() < 0.5) { // 50% 확률로 드롭
                    toDrop.add(item.clone())
                    inventory.setItem(i, null)
                }
            }
        }


        // 드롭된 아이템들을 땅에 떨어뜨림
        val dropLoc = player.location
        for (item in toDrop) {
            dropLoc.world.dropItemNaturally(dropLoc, item)
        }
    }

    fun applyWindCharge(player: Player, targetLocation: Location) {
        if (getPlayerBuffs(player.uniqueId).contains("knockback_resistance")) {
            return
        }

        val direction = targetLocation.toVector().subtract(player.location.toVector()).normalize()
        direction.multiply(2.0)
        direction.setY(max(direction.y, 0.5))

        player.velocity = direction
    }

    fun applyMarkerKnockback(target: Player, knockbackDirection: Vector) {
        // 부동세 버프 체크
        if (getPlayerBuffs(target.uniqueId).contains("knockback_resistance")) {
            return
        }

        knockbackDirection.multiply(1.5) // 기존 넉백 처리
        knockbackDirection.setY(max(knockbackDirection.y, 0.3))

        target.velocity = knockbackDirection
    }


    fun checkAttackRights() {
        // 스케줄러에서 호출 - 만료된 공격권 제거
        val toRemove: MutableList<UUID> = ArrayList()
        val now = System.currentTimeMillis()

        for ((key, value) in attackRightExpiry) {
            if (now > value) {
                toRemove.add(key)
            }
        }

        for (uuid in toRemove) {
            removeAttackRight(uuid)
        }
    }

    fun startMarkerGlowTask() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    // 플레이어가 표식 소지인지 확인
                    val hasMarker = player.inventory.contents.any { it?.isNamed("표식") == true }
                    if (hasMarker) {
                        player.addPotionEffect(
                            PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false)
                        )
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L) // 10틱마다 반복 체크
    }

    private fun ItemStack?.isNamed(name: String): Boolean {
        if (this == null) return false
        return this.type == Material.ENCHANTED_BOOK && this.hasItemMeta() &&
                this.itemMeta?.displayName?.contains(name) == true
    }



    fun reset() {
        attackRightExpiry.clear()
        permanentBuffs.clear()
        cannonUses.clear()
        playersWithThrowRange.clear()


        // 모든 온라인 플레이어의 포션 효과 제거
        for (player in Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SPEED)
            player.removePotionEffect(PotionEffectType.JUMP_BOOST)
        }
    }

    // Getters
    fun hasThrowRange(playerUuid: UUID): Boolean {
        return playersWithThrowRange.contains(playerUuid)
    }

    fun getCannonUses(playerUuid: UUID): Int {
        return cannonUses.getOrDefault(playerUuid, 0)
    }

    fun getAttackRightExpiry(playerUuid: UUID): Long {
        return attackRightExpiry.getOrDefault(playerUuid, 0L)
    }

    fun getPlayerBuffs(playerUuid: UUID): Set<String> {
        return permanentBuffs.getOrDefault(playerUuid, HashSet())
    }
}