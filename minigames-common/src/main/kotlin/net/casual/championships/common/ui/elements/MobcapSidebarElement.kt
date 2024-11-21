package net.casual.championships.common.ui.elements

import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.visuals.elements.LevelSpecificElement
import net.casual.arcade.visuals.elements.component.MobcapComponentElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel

object MobcapSidebarElement: LevelSpecificElement<SidebarComponent> {
    override fun get(level: ServerLevel): SidebarComponent {
        return SidebarComponent.withNoScore(Component.empty().append(MobcapComponentElement.get(level)).mini())
    }
}