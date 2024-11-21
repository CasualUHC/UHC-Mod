package net.casual.championships.common.util

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.casual.arcade.commands.function
import net.casual.arcade.commands.success
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.TeamUtils.getOnlinePlayers
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.network.chat.Component

object CommonCommands {
    private val NOT_SPECTATOR = SimpleCommandExceptionType(CommonComponents.NOT_SPECTATING)
    private val NO_TEAM = SimpleCommandExceptionType(CommonComponents.NO_TEAM)

    fun toggleFullbright(
        minigame: Minigame,
        context: CommandContext<CommandSourceStack>
    ): Int {
        val player = context.source.playerOrException
        val toggle = if (minigame.effects.hasFullbright(player)) {
            minigame.effects.removeFullbright(player)
            CommonComponents.DISABLED
        } else {
            minigame.effects.addFullbright(player)
            CommonComponents.ENABLED
        }
        return context.source.success(CommonComponents.TOGGLE_FULLBRIGHT.generate(toggle))
    }

    fun toggleTeamGlow(
        minigame: Minigame,
        context: CommandContext<CommandSourceStack>
    ): Int {
        val player = context.source.playerOrException
        val toggle = if (minigame.tags.has(player, CommonTags.HAS_TEAM_GLOW)) {
            minigame.tags.remove(player, CommonTags.HAS_TEAM_GLOW)
            CommonComponents.DISABLED
        } else {
            minigame.tags.add(player, CommonTags.HAS_TEAM_GLOW)
            CommonComponents.ENABLED
        }
        val team = player.team
        if (team != null) {
            for (teammate in team.getOnlinePlayers()) {
                minigame.effects.forceUpdate(teammate, player)
            }
        }
        return context.source.success(CommonComponents.TOGGLE_TEAMGLOW.generate(toggle))
    }

    fun broadcastPositionToTeammates(
        minigame: Minigame,
        context: CommandContext<CommandSourceStack>
    ): Int {
        val player = context.source.playerOrException
        val team = player.team

        if (team === null) {
            throw NO_TEAM.create()
        }

        val position = player.position()
        val location = Component.literal("[%.0f, %.0f, %.0f]".format(position.x, position.y, position.z)).lime().function {
            it.player.lookAt(EntityAnchorArgument.Anchor.EYES, position)
        }
        minigame.chat.broadcastAsPlayerTo(
            player,
            CommonComponents.BROADCAST_POSITION.generate(location),
            team.getOnlinePlayers(),
            minigame.chat.teamChatFormatter
        )
        return Command.SINGLE_SUCCESS
    }

    fun openSpectatingScreen(
        minigame: Minigame,
        context: CommandContext<CommandSourceStack>
    ): Int {
        val player = context.source.playerOrException
        if (!minigame.players.isAdmin(player) && minigame.players.isPlaying(player)) {
            throw NOT_SPECTATOR.create()
        }

        CommonUI.createTeamSelectionGui(minigame, player).open()
        return Command.SINGLE_SUCCESS
    }
}