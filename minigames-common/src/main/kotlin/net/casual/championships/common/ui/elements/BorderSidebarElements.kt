package net.casual.championships.common.ui.elements

import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.PlayerUtils.distanceToNearestBorder
import net.casual.arcade.visuals.elements.LevelSpecificElement
import net.casual.arcade.visuals.elements.PlayerSpecificElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.casual.championships.common.util.CommonComponents
import net.minecraft.ChatFormatting.*
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.border.BorderStatus

class BorderStatusElement(private val buffer: Component): LevelSpecificElement<SidebarComponent> {
    override fun get(level: ServerLevel): SidebarComponent {
        val phase = ((level.server.tickCount / 3) % 5) + 1
        val border = when (level.worldBorder.status) {
            BorderStatus.SHRINKING -> CommonComponents.Border.red(phase)
            BorderStatus.GROWING -> CommonComponents.Border.green(phase)
            else -> CommonComponents.Border.blue(phase)
        }
        val display = Component.empty()
            .append(this.buffer)
            .append(border)
            .append(SpacingFontResources.spaced(4))
            .append(CommonComponents.BORDER_INFO.mini())
        return SidebarComponent.withNoScore(display)
    }
}

class BorderDistanceElement(private val buffer: Component): PlayerSpecificElement<SidebarComponent> {
    override fun get(player: ServerPlayer): SidebarComponent {
        val vectorToBorder = player.distanceToNearestBorder()
        val multiplier = if (vectorToBorder.x < 0 || vectorToBorder.z < 0) -1 else 1
        val distanceToBorder = multiplier * vectorToBorder.length().toInt()

        val percent = distanceToBorder / (player.level().worldBorder.size / 2.0)
        val colour = if (percent > 0.4) DARK_GREEN else if (percent > 0.2) YELLOW else if (percent > 0.1) RED else DARK_RED
        val display = Component.empty().append(this.buffer).append(" ").append(CommonComponents.BORDER_DISTANCE.mini())
        val score = Component.literal(distanceToBorder.toString()).append(this.buffer).withStyle(colour)
        return SidebarComponent.withCustomScore(display, score.mini())
    }
}

class BorderSizeElement(private val buffer: Component): LevelSpecificElement<SidebarComponent> {
    override fun get(level: ServerLevel): SidebarComponent {
        val display = Component.empty().append(this.buffer).append(" ").append(CommonComponents.BORDER_RADIUS.mini())
        val score = Component.literal((level.worldBorder.size / 2.0).toInt().toString()).append(this.buffer)
        return SidebarComponent.withCustomScore(display, score.mini())
    }
}