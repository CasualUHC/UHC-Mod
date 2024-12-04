package net.casual.championships.common

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils
import net.casual.arcade.dimensions.utils.DimensionRegistries
import net.casual.arcade.resources.ArcadeResourcePacks
import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.resources.utils.ResourcePackUtils.addFont
import net.casual.arcade.resources.utils.ResourcePackUtils.addLangsFrom
import net.casual.arcade.resources.utils.ResourcePackUtils.addLangsFromData
import net.casual.arcade.resources.utils.ResourcePackUtils.addMissingItemModels
import net.casual.arcade.resources.utils.ResourcePackUtils.addSounds
import net.casual.arcade.scheduler.task.utils.TaskRegisties
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.level.ReducedMobSpawningRules
import net.casual.championships.common.task.GlowingBossbarTask
import net.casual.championships.common.task.GracePeriodBossbarTask
import net.casual.championships.common.util.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CommonMod: ModInitializer {
    const val MOD_ID = "casual_common"

    private val container = FabricLoader.getInstance().getModContainer(MOD_ID).get()

    val logger: Logger = LoggerFactory.getLogger("CasualCommon")

    val COMMON_PACK = NamedResourcePackCreator.named("common") {
        addAssetSource(MOD_ID)
        addLangsFromData(MOD_ID)
        addLangsFrom("minecraft", container.findPath("data/minecraft/lang").get())
        addFont(CommonComponents.Hud)
        addFont(CommonComponents.Gui)
        addFont(CommonComponents.Text)
        addFont(CommonComponents.Border)
        addSounds(CommonSounds)
        addMissingItemModels(MOD_ID)
        packDescription = Component.literal("Common resources used in CasualChampionships")
    }

    val COMMON_PACKS = listOf(
        ArcadeResourcePacks.PADDING_FONT_PACK,
        ArcadeResourcePacks.PLAYER_HEADS_PACK,
        ArcadeResourcePacks.HIDE_PLAYER_LIST_HEADS_PACK,
        ArcadeResourcePacks.MINI_MINECRAFT_FONT,
        ArcadeResourcePacks.SPACING_FONT_PACK,
        ArcadeResourcePacks.HIDE_PLAYER_LIST_PING_PACK,
        ArcadeResourcePacks.ACTION_BAR_FONT_PACK,
        ArcadeResourcePacks.MINI_ACTION_BAR_FONT_PACK,
        ArcadeResourcePacks.ARCADE_LANG_PACK,
        COMMON_PACK
    )

    override fun onInitialize() {
        CommonItems.noop()
        CommonEntities.noop()
        CommonSounds.noop()

        AntiCheat.registerEvents()

        Registry.register(DimensionRegistries.CUSTOM_MOB_SPAWNING_RULES, id("reduced_mob_cap"), ReducedMobSpawningRules)

        Registry.register(TaskRegisties.TASK_FACTORY, GracePeriodBossbarTask.id, GracePeriodBossbarTask)
        Registry.register(TaskRegisties.TASK_FACTORY, GlowingBossbarTask.id, GlowingBossbarTask)

        PolymerItemGroupUtils.registerPolymerItemGroup(
            id("menu"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                .title(Component.literal("Display Items"))
                .icon(DisplayItems::TICK)
                .alignedRight()
                .displayItems { _, output ->
                    output.acceptAll(DisplayItems.all())
                }
                .build()
        )
        PolymerItemGroupUtils.registerPolymerItemGroup(
            id("heads"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                .title(Component.literal("Head Items"))
                .icon(CommonItems.GOLDEN_HEAD::getDefaultInstance)
                .alignedRight()
                .displayItems { _, output ->
                    output.accept(CommonItems.GOLDEN_HEAD)
                    output.accept(CommonItems.PLAYER_HEAD)
                    output.accept(CommonItems.FORWARD_FACING_PLAYER_HEAD)
                }
                .build()
        )
    }

    fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
    }
}