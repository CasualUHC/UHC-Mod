package net.casualuhc.uhc.uhc

import net.casualuhc.arcade.map.PlaceableMap
import net.casualuhc.arcade.map.StructureMap
import net.casualuhc.arcade.math.Location
import net.casualuhc.arcade.utils.StructureUtils
import net.casualuhc.uhc.minigame.Dimensions
import net.casualuhc.uhc.minigame.uhc.UHCMinigame
import net.casualuhc.uhc.uhc.handlers.LobbyHandler
import net.casualuhc.uhc.uhc.handlers.ResourceHandler
import net.casualuhc.uhc.util.Config
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

class EasterUHC: UHCEvent, LobbyHandler {
    private val lobby: PlaceableMap

    init {
        this.lobby = StructureMap(
            StructureUtils.read(Config.resolve("lobbies/easter_lobby.nbt")),
            Vec3i(0, 320, 0),
            Dimensions.getLobbyLevel()
        )
    }

    override fun getTeamSize(): Int {
        return 5
    }

    override fun getLobbyHandler(): LobbyHandler {
        return this
    }

    override fun getResourcePackHandler(): ResourceHandler {
        return ResourceHandler.default()
    }

    override fun initialise(uhc: UHCMinigame) {

    }

    override fun getMap(): PlaceableMap {
        return this.lobby
    }

    override fun getSpawn(): Location {
        return Location(Dimensions.getLobbyLevel(), Vec3(-0.5, 298.0, -1.5), Vec2(-45.0F, 30.0F))
    }
}