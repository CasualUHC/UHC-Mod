package net.casual.championships.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.casual.arcade.commands.CommandTree
import net.casual.championships.common.ui.MinesweeperGui
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object MinesweeperCommand: CommandTree {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("minesweeper").executes(this::execute)
    }

    fun execute(context: CommandContext<CommandSourceStack>): Int {
        MinesweeperGui(context.source.playerOrException).open()
        return Command.SINGLE_SUCCESS
    }
}