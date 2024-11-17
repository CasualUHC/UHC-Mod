package net.casual.championships.common.ui

import net.casual.arcade.utils.ItemUtils
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.visuals.screen.PlayerInventoryViewGui
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonItems
import net.minecraft.server.level.ServerPlayer

class CasualPlayerInventoryViewGui(
    observee: ServerPlayer,
    observer: ServerPlayer
): PlayerInventoryViewGui(observee, observer) {
    override fun loadBackground() {
        val name = this.observee.displayName!!
        this.title = CommonComponents.Gui.createDoubleChestGui(name)

        val head = ItemUtils.createPlayerHead(this.observee, CommonItems.FORWARD_FACING_PLAYER_HEAD).named(name)
        this.setSlot(3, head)
    }
}