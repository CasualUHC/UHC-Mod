package net.casual.championships.common.ui.elements

import net.casual.arcade.minigame.Minigame
import net.casual.arcade.utils.ComponentUtils.green
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.visuals.elements.UniversalElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class MinigamePhaseSidebarElement(
    private val minigame: Minigame,
    private val buffer: Component
): UniversalElement<SidebarComponent> {
    override fun get(server: MinecraftServer): SidebarComponent {
        return SidebarComponent.withCustomScore(
            Component.empty().append(this.buffer).append("Phase:").mini(),
            Component.literal(this.minigame.phase.id).append(this.buffer).green().mini()
        )
    }
}