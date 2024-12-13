package net.casual.championships.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.casual.arcade.commands.*
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ComponentArgument
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.SlotArgument
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.SlotAccess

object RenameCommand: CommandTree {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("rename") {
            literal("item") {
                argument("player", EntityArgument.player()) {
                    argument("slot", SlotArgument.slot()) {
                        argument("name", ComponentArgument.textComponent(buildContext)) {
                            executes(::renamePlayerItem)
                        }
                    }
                }
            }
        }
    }

    private fun renamePlayerItem(context: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(context, "player")
        val slot = SlotArgument.getSlot(context, "slot")
        val name = ComponentArgument.getComponent(context, "name")
        val access = player.getSlot(slot)
        if (access == SlotAccess.NULL) {
            return context.source.fail("Tried to rename item in unknown slot")
        }
        val stack = access.get()
        if (stack.isEmpty) {
            return context.source.fail("Cannot rename empty item")
        }
        stack.set(DataComponents.CUSTOM_NAME, name)
        return context.source.success("Successfully renamed item")
    }
}