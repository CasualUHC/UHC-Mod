package net.casual.championships.common.util

import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.visuals.screen.SelectionGuiComponents
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.util.CommonComponents.BACK
import net.casual.championships.common.util.CommonComponents.EXIT
import net.casual.championships.common.util.CommonComponents.NEXT
import net.casual.championships.common.util.CommonComponents.PREVIOUS
import net.minecraft.network.chat.Component

object CommonScreens {
    private val COMPONENTS: SelectionGuiComponents = SelectionGuiComponents.Builder().apply {
        next(DisplayItems.GREEN_LONG_RIGHT.named(NEXT), DisplayItems.GREY_GREEN_LONG_RIGHT.named(NEXT))
        previous(DisplayItems.GREEN_LONG_LEFT.named(PREVIOUS), DisplayItems.GREY_GREEN_LONG_LEFT.named(PREVIOUS))
        back(DisplayItems.CROSS.named(BACK), DisplayItems.CROSS.named(EXIT))
    }

    fun named(title: Component): SelectionGuiComponents {
        return SelectionGuiComponents.Builder(COMPONENTS).title(CommonComponents.Gui.createDoubleChestGui(title))
    }
}