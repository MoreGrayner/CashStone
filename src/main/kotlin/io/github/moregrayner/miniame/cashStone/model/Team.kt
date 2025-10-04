package io.github.moregrayner.miniame.cashStone.model

import java.util.*


class Team(
    var name: String, var captain: UUID
) {
    internal val members: MutableList<UUID> = ArrayList()

    init {
        members.add(captain)
    }

    fun addMember(memberUuid: UUID) {
        if (!members.contains(memberUuid)) {
            members.add(memberUuid)
        }
    }

    fun removeMember(memberUuid: UUID) {
        members.remove(memberUuid)


        if (captain == memberUuid && members.isNotEmpty()) {
            captain = members[0]
        }
    }

    fun isMember(playerUuid: UUID): Boolean {
        return members.contains(playerUuid)
    }

    fun getMembers(): List<UUID> {
        return ArrayList(members)
    }

    val size: Int
        get() = members.size
}