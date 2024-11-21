package net.casual.championships.duel

import net.casual.arcade.commands.function
import net.casual.arcade.minigame.ready.ReadyHandler
import net.casual.arcade.minigame.ready.ReadyState
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.red
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Nameable

class DuelRequester(
    private val requester: ServerPlayer,
    private val players: Collection<ServerPlayer>
): ReadyHandler<ServerPlayer> {
    override fun format(readier: ServerPlayer): Component {
        return (readier as Nameable).displayName
    }

    override fun broadcastReadyCheck(receiver: ServerPlayer, ready: () -> Unit, notReady: () -> Unit) {
        val message = Component.empty()
            .append(Component.translatable("casual.duel.challenge", requester.displayName))
            .append(SpacingFontResources.spaced(4))
            .append(
                Component.literal("[")
                    .append(Component.translatable("casual.duel.challenge.accept"))
                    .append("]")
                    .function { ready.invoke() }
                    .lime()
            )
            .append(SpacingFontResources.spaced(4))
            .append(
                Component.literal("[")
                    .append(Component.translatable("casual.duel.challenge.decline"))
                    .append("]")
                    .function { notReady.invoke() }
                    .red()
            ).mini()
        this.broadcastTo(message, receiver)
    }

    override fun onReady(readier: ServerPlayer, previous: ReadyState): Boolean {
        val message = Component.translatable("casual.duel.accepted", this.format(readier)).lime().mini()
        this.broadcast(message)
        return true
    }

    override fun onNotReady(readier: ServerPlayer, previous: ReadyState): Boolean {
        if (previous != ReadyState.Ready) {
            val message = Component.translatable("casual.duel.declined", this.format(readier)).red().mini()
            this.broadcast(message)
            return true
        }
        return false
    }

    override fun onAllReady() {

    }

    private fun broadcast(message: Component) {
        for (player in this.players) {
            this.broadcastTo(message, player)
        }
    }

    fun broadcastTo(message: Component, player: ServerPlayer) {
        player.sendSystemMessage(Component.empty().append(DUEL_PREFIX).append(" ").append(message))
    }

    companion object {
        val DUEL_PREFIX = Component.literal("[⚔]").lime()
    }
}