package net.casual.championships.uhc

import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ResourcePackUtils.addFont
import net.casual.arcade.utils.ResourcePackUtils.addLangsFromData
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object UHCMod {
    private const val MOD_ID = "casual_uhc"

    internal val logger: Logger = LoggerFactory.getLogger("CasualUHC")

    val UHC_PACK = NamedResourcePackCreator.named("uhc") {
        addAssetSource(MOD_ID)
        addLangsFromData(MOD_ID)
        addFont(UHCComponents.Bitmap)
        packDescription = "Resources for CasualChampionships UHC minigame".literal()
    }

    internal fun id(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }
}