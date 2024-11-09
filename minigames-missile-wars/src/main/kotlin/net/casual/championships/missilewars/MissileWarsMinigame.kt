package net.casual.championships.missilewars

import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.phase.Phase
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import java.util.*

class MissileWarsMinigame(server: MinecraftServer, uuid: UUID): Minigame(server, uuid) {
    override val id: ResourceLocation = ID

    override fun phases(): Collection<Phase<MissileWarsMinigame>> {
        TODO("Not yet implemented")
    }

    companion object {
        val ID = MissileWarsMod.id("missile_wars_minigame")
    }
}