package net.casual.championships.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.literal
import net.casual.championships.common.util.CommonUI
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument

object ViewCommand: CommandTree {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("view") {
            literal("inventory") {
                argument("target", EntityArgument.player()) {
                    executes(::viewPlayerInventory)
                }
            }
        }
    }

    private fun viewPlayerInventory(context: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(context, "target")
        val gui = CommonUI.createPlayerInventoryViewGui(target, context.source.playerOrException)
        gui.open()
        return Command.SINGLE_SUCCESS
    }
}