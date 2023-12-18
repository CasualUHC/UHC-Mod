package net.casual.championships.extensions

import net.casual.arcade.extensions.DataExtension
import net.casual.arcade.utils.PlayerUtils.getExtension
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.border.WorldBorder

class PlayerUHCExtension: DataExtension {
    val border = WorldBorder()

    var halfHealthTicks = 0

    override fun getName(): String {
        return "UHC_PlayerUHC"
    }

    override fun deserialize(element: Tag) {
        element as CompoundTag
        this.halfHealthTicks = element.getInt("halfHealthTicks")
    }

    override fun serialize(): Tag {
        val tag = CompoundTag()
        tag.putInt("halfHealthTicks", this.halfHealthTicks)
        return tag
    }

    companion object {
        val ServerPlayer.uhc
            get() = this.getExtension(PlayerUHCExtension::class.java)
    }
}