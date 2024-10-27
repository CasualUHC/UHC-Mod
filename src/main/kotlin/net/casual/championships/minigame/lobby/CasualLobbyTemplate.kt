package net.casual.championships.minigame.lobby

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.minigame.lobby.Lobby
import net.casual.arcade.minigame.template.area.PlaceableAreaTemplate
import net.casual.arcade.minigame.template.bossbar.TimerBossbarTemplate
import net.casual.arcade.minigame.template.countdown.CountdownTemplate
import net.casual.arcade.minigame.template.lobby.LobbyTemplate
import net.casual.arcade.minigame.template.lobby.SimpleLobbyTemplate
import net.casual.arcade.minigame.template.location.LocationTemplate
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.casual.championships.CasualMod
import net.casual.championships.duel.arena.DuelArenasTemplate
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel

class CasualLobbyTemplate(
    area: PlaceableAreaTemplate,
    spawn: LocationTemplate,
    countdown: CountdownTemplate,
    bossbar: TimerBossbarTemplate,
    val podium: LocationTemplate,
    val podiumView: LocationTemplate,
    val fireworkLocations: List<LocationTemplate>,
    val duelArenas: List<DuelArenasTemplate>
): SimpleLobbyTemplate(area, spawn, countdown, bossbar) {
    override fun create(level: ServerLevel): Lobby {
        return CasualLobby(
            this.area.create(level),
            this.spawn,
            this.bossbar,
            this.countdown,
            this.podium,
            this.podiumView,
            this.fireworkLocations,
            this.duelArenas
        )
    }

    override fun codec(): MapCodec<out LobbyTemplate> {
        return CODEC
    }

    companion object: CodecProvider<CasualLobbyTemplate> {
        override val ID: ResourceLocation = CasualMod.id("lobby")

        override val CODEC: MapCodec<out CasualLobbyTemplate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                PlaceableAreaTemplate.CODEC.fieldOf("area").forGetter(SimpleLobbyTemplate::area),
                LocationTemplate.CODEC.fieldOf("spawn").forGetter(SimpleLobbyTemplate::spawn),
                CountdownTemplate.CODEC.fieldOf("countdown").forGetter(SimpleLobbyTemplate::countdown),
                TimerBossbarTemplate.CODEC.fieldOf("bossbar").forGetter(SimpleLobbyTemplate::bossbar),
                LocationTemplate.CODEC.fieldOf("podium").forGetter(CasualLobbyTemplate::podium),
                LocationTemplate.CODEC.fieldOf("podium_view").forGetter(CasualLobbyTemplate::podiumView),
                LocationTemplate.CODEC.listOf().fieldOf("firework_locations")
                    .forGetter(CasualLobbyTemplate::fireworkLocations),
                DuelArenasTemplate.CODEC.listOf().encodedOptionalFieldOf("duel_arenas", listOf())
                    .forGetter(CasualLobbyTemplate::duelArenas)
            ).apply(instance, ::CasualLobbyTemplate)
        }
    }
}