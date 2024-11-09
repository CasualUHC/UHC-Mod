package net.casual.championships.minigame

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.minigame.lobby.LobbyMinigameFactory
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.minigame.template.minigame.SimpleMinigamesTemplate
import net.casual.arcade.resources.pack.PackInfo
import net.casual.arcade.resources.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.casual.championships.CasualMod
import net.casual.championships.resources.CasualResourcePackHost
import net.minecraft.resources.ResourceLocation

class CasualChampionshipsTemplate(
    name: String = "default",
    lobby: MinigameFactory = LobbyMinigameFactory.DEFAULT,
    operators: List<String> = listOf(),
    minigames: List<MinigameFactory>,
    repeat: Boolean = true,
    private val additionalPacks: List<String>
): SimpleMinigamesTemplate(name, lobby, operators, minigames, repeat) {
    override fun getAdditionalPacks(): Iterable<PackInfo> {
        val packs = ArrayList<PackInfo>()
        for (pack in this.additionalPacks) {
            // TODO:
            @Suppress("DEPRECATION")
            val hosted = CasualResourcePackHost.getHostedPack(pack)
            if (hosted == null) {
                CasualMod.logger.error("Failed to load additional pack $pack")
                continue
            }
            packs.add(hosted.toPackInfo(!CasualMod.config.dev))
        }

        return CasualResourcePackHost.getCommonPacks().mapTo(packs) {
            it.toPackInfo(!CasualMod.config.dev)
        }
    }

    override fun codec(): MapCodec<out CasualChampionshipsTemplate> {
        return CODEC
    }

    companion object: CodecProvider<CasualChampionshipsTemplate> {
        override val ID: ResourceLocation = CasualMod.id("championships")

        override val CODEC: MapCodec<CasualChampionshipsTemplate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.encodedOptionalFieldOf("name", "default").forGetter(SimpleMinigamesTemplate::name),
                MinigameFactory.CODEC.fieldOf("lobby").forGetter(SimpleMinigamesTemplate::lobby),
                Codec.STRING.listOf().fieldOf("operators").forGetter(SimpleMinigamesTemplate::operators),
                MinigameFactory.CODEC.listOf().fieldOf("minigames").forGetter(SimpleMinigamesTemplate::minigames),
                Codec.BOOL.fieldOf("repeat").forGetter(SimpleMinigamesTemplate::repeat),
                Codec.STRING.listOf().encodedOptionalFieldOf("additional_packs", listOf())
                    .forGetter(CasualChampionshipsTemplate::additionalPacks)
            ).apply(instance, ::CasualChampionshipsTemplate)
        }
    }
}