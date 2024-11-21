package net.casual.championships.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.success
import net.casual.arcade.minigame.utils.MinigameUtils.requiresAdminOrPermission
import net.casual.championships.CasualMod
import net.casual.championships.minigame.CasualMinigames
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument

@Suppress("UnstableApiUsage")
object CasualCommand: CommandTree {
    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("casual").requiresAdminOrPermission().then(
            Commands.literal("team").then(
                Commands.literal("create").executes(this::createTeams)
            ).then(
                Commands.literal("reload").executes(this::reloadTeams)
            )
        ).then(
            Commands.literal("config").then(
                Commands.literal("reload").executes(this::reloadConfig)
            )
        ).then(
            Commands.literal("resources").then(
                Commands.literal("reload").executes(this::reloadResources)
            )
        ).then(
            Commands.literal("lobby").executes(this::returnToLobby)
        ).then(
            Commands.literal("floodgates").then(
                Commands.literal("open").executes { this.floodgates(it, true) }
            ).then(
                Commands.literal("close").executes { this.floodgates(it, false) }
            )
        ).then(
            Commands.literal("winners").then(
                Commands.literal("clear").executes {
                    CasualMinigames.winners.clear(); 1
                }
            ).then(
                Commands.literal("add").then(
                    Commands.argument("players", EntityArgument.players()).executes {
                        val players = EntityArgument.getPlayers(it, "players")
                        for (player in players) {
                            CasualMinigames.winners.add(player.scoreboardName)
                        }
                        1
                    }
                )
            )
        )
    }

    private fun createTeams(context: CommandContext<CommandSourceStack>): Int {
        CasualMinigames.createTeams(context.source.server)
        return 1
    }

    private fun reloadTeams(context: CommandContext<CommandSourceStack>): Int {
        CasualMinigames.reloadTeams(context.source.server)
        return 1
    }

    private fun reloadConfig(context: CommandContext<CommandSourceStack>): Int {
        CasualMod.reload()
        return context.source.success("Successfully reloaded config", true)
    }

    private fun reloadResources(context: CommandContext<CommandSourceStack>): Int {
        CasualMinigames.reloadResourcePacks(context.source.server).thenRun {
            context.source.success("Successfully reloaded resources, resending packs...")
        }
        return context.source.success("Reloading resources...")
    }

    private fun returnToLobby(context: CommandContext<CommandSourceStack>): Int {
        CasualMinigames.getMinigames().returnToLobby()
        return context.source.success("Returning to lobby...")
    }

    private fun floodgates(context: CommandContext<CommandSourceStack>, open: Boolean): Int {
        CasualMinigames.floodgates = open
        return context.source.success("Successfully ${if (open) "opened" else "closed"} the floodgates", true)
    }
}