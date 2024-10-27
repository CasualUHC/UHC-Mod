package net.casual.championships.common.items

import eu.pb4.polymer.core.api.utils.PolymerUtils
import net.casual.arcade.utils.ComponentUtils.gold
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile

class GoldenHeadItem(properties: Properties): HeadItem(properties) {
    override fun getResolvableProfile(stack: ItemStack): ResolvableProfile {
        return PolymerUtils.createProfileComponent(GOLDEN, null)
    }

    override fun addEffects(player: ServerPlayer) {
        player.addEffect(MobEffectInstance(REGENERATION, 50, 3))
        player.addEffect(MobEffectInstance(MOVEMENT_SPEED, 20 * 20, 1))
        player.addEffect(MobEffectInstance(SATURATION, 5, 4))

        player.addEffect(MobEffectInstance(ABSORPTION, 120 * 20, 0))
        player.addEffect(MobEffectInstance(DAMAGE_RESISTANCE, 5 * 20, 0))
    }

    override fun getName(stack: ItemStack): Component {
        return CommonComponents.GOLDEN_HEAD.gold()
    }

    private companion object {
        const val GOLDEN = "ewogICJ0aW1lc3RhbXAiIDogMTY3MDg2MDkyNTE4MywKICAicHJvZmlsZUlkIiA6ICI1N2E4NzA0ZGIzZjQ0YzhmYmVhMDY0Njc1MDExZmU3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJQaGFudG9tVHVwYWMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjk4Nzg4NTM2NTRjM2JiMjZmZDMzZTgwZjhlZDNjZGYwM2FiMzI0N2Y3MzY3ODQ2NGUwNmRhMTQzZGJkMGMxNyIKICAgIH0sCiAgICAiQ0FQRSIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjM0MGMwZTAzZGQyNGExMWIxNWE4YjMzYzJhN2U5ZTMyYWJiMjA1MWIyNDgxZDBiYTdkZWZkNjM1Y2E3YTkzMyIKICAgIH0KICB9Cn0"
    }
}