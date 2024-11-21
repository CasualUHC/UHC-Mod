package net.casual.championships.common.ui.elements

import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.visuals.elements.UniversalElement
import net.casual.arcade.visuals.elements.component.MSPTComponentElement
import net.casual.arcade.visuals.elements.component.TPSComponentElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class PerformanceSidebarElement(private val buffer: Component): UniversalElement<SidebarComponent> {
    override fun get(server: MinecraftServer): SidebarComponent {
        val tps = Component.empty().append(this.buffer).append(TPSComponentElement.get(server)).mini()
        val mspt = Component.empty().append(MSPTComponentElement.get(server)).append(this.buffer).mini()
        return SidebarComponent.withCustomScore(tps, mspt)
    }
}