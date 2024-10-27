package net.casual.championships.common.util

import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.chat.ChatFormatter
import net.casual.arcade.minigame.managers.MinigameChatManager
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.bold
import net.casual.arcade.utils.ComponentUtils.gold
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.PlayerUtils.distanceToNearestBorder
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.TeamUtils.getHexColor
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.visuals.elements.ComponentElements
import net.casual.arcade.visuals.elements.LevelSpecificElement
import net.casual.arcade.visuals.elements.PlayerSpecificElement
import net.casual.arcade.visuals.elements.SidebarElements
import net.casual.arcade.visuals.nametag.PlayerNameTag
import net.casual.arcade.visuals.predicate.EntityObserverPredicate
import net.casual.arcade.visuals.predicate.PlayerObserverPredicate
import net.casual.arcade.visuals.predicate.PlayerObserverPredicate.Companion.toPlayer
import net.casual.arcade.visuals.sidebar.Sidebar
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.casual.arcade.visuals.tab.PlayerListDisplay
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.ui.elements.SpectatorAndAdminTeamsElement
import net.casual.championships.common.ui.elements.TeammateElement
import net.casual.championships.common.ui.game.TeamSelectorGui
import net.casual.championships.common.ui.tab.CasualPlayerListEntries
import net.casual.championships.common.ui.tab.SimpleCasualPlayerListEntries
import net.casual.championships.common.util.CommonComponents.Text.CASUAL
import net.casual.championships.common.util.CommonComponents.Text.CHAMPIONSHIPS
import net.casual.championships.common.util.CommonComponents.Text.KIWITECH
import net.casual.championships.common.util.CommonComponents.Text.SERVER_HOSTED_BY
import net.minecraft.ChatFormatting.*
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.level.border.BorderStatus

object CommonUI {
    val INFO_ANNOUNCEMENT = ChatFormatter.createAnnouncement(Component.literal("[Info]").gold().bold().mini())
    val GAME_ANNOUNCEMENT = ChatFormatter.createAnnouncement(Component.literal("[Game]").lime().bold().mini())
    val READY_ANNOUNCEMENT = ChatFormatter.createAnnouncement(Component.literal("[Ready]").lime().bold().mini())

    fun MinigameChatManager.broadcastInfo(
        component: Component,
        players: Iterable<ServerPlayer> = this.getAllPlayers(),
        sound: Sound = Sound(CommonSounds.GLOBAL_SERVER_NOTIFICATION_LOW)
    ) {
        this.broadcastWithSound(component, sound, players, INFO_ANNOUNCEMENT)
    }

    fun MinigameChatManager.broadcastGame(
        component: Component,
        players: Iterable<ServerPlayer> = this.getAllPlayers(),
        sound: Sound = Sound(CommonSounds.GLOBAL_SERVER_NOTIFICATION)
    ) {
        this.broadcastWithSound(component, sound, players, GAME_ANNOUNCEMENT)
    }

    fun MinigameChatManager.broadcastWithSound(
        component: Component,
        sound: Sound = Sound(CommonSounds.GLOBAL_SERVER_NOTIFICATION),
        players: Iterable<ServerPlayer> = this.getAllPlayers(),
        formatter: ChatFormatter? = this.systemChatFormatter
    ) {
        this.broadcastTo(component, players, formatter)
        for (player in players) {
            player.sendSound(sound)
        }
    }

    fun createPlayingNameTag(
        predicate: PlayerObserverPredicate = EntityObserverPredicate.visibleObservee().toPlayer()
    ): PlayerNameTag {
        return PlayerNameTag({ it.displayName!! }, predicate)
    }

    fun createPlayingHealthTag(
        predicate: PlayerObserverPredicate = CommonPredicates.VISIBLE_OBSERVER_AND_SPEC_OR_TEAMMATES
    ): PlayerNameTag {
        return PlayerNameTag(
            { String.format("%.1f ", it.health / 2).literal().append(CommonComponents.Hud.HARDCORE_HEART) },
            predicate
        )
    }

    fun addTeammates(sidebar: Sidebar, size: Int): Sidebar {
        val buffer = ComponentUtils.space()
        val teammates = Component.empty().append(buffer).append(CommonComponents.TEAMMATES.mini())
        sidebar.addRow(SidebarElements.withNoScore(teammates))
        for (i in 0 until size) {
            sidebar.addRow(TeammateElement(i, buffer))
        }
        return sidebar
    }

    fun addBorderDistanceAndRadius(sidebar: Sidebar) {
        val buffer = ComponentUtils.space()
        sidebar.addRow(LevelSpecificElement.cached { level ->
            val phase = ((level.server.tickCount / 3) % 5) + 1
            val border = when (level.worldBorder.status) {
                BorderStatus.SHRINKING -> CommonComponents.Border.red(phase)
                BorderStatus.GROWING -> CommonComponents.Border.green(phase)
                else -> CommonComponents.Border.blue(phase)
            }
            val display = Component.empty()
                .append(buffer)
                .append(border)
                .append(ComponentUtils.space())
                .append(CommonComponents.BORDER_INFO.mini())
            SidebarComponent.withNoScore(display)
        })
        sidebar.addRow { player ->
            val vectorToBorder = player.distanceToNearestBorder()
            val multiplier = if (vectorToBorder.x < 0 || vectorToBorder.z < 0) -1 else 1
            val distanceToBorder = multiplier * vectorToBorder.length().toInt()

            val percent = distanceToBorder / (player.level().worldBorder.size / 2.0)
            val colour = if (percent > 0.4) DARK_GREEN else if (percent > 0.2) YELLOW else if (percent > 0.1) RED else DARK_RED
            val display = Component.empty().append(buffer).append(" ").append(CommonComponents.BORDER_DISTANCE.mini())
            val score = distanceToBorder.toString().literal().append(buffer).withStyle(colour)
            SidebarComponent.withCustomScore(display, score.mini())
        }
        sidebar.addRow(LevelSpecificElement.cached { level ->
            val display = Component.empty().append(buffer).append(" ").append(CommonComponents.BORDER_RADIUS.mini())
            val score = (level.worldBorder.size / 2.0).toInt().toString().literal().append(buffer)
            SidebarComponent.withCustomScore(display, score.mini())
        })
    }

    fun createTeamMinigameTabDisplay(minigame: Minigame<*>): PlayerListDisplay {
        val display = PlayerListDisplay(CasualPlayerListEntries(minigame))
        addCasualFooterAndHeader(minigame, display)
        return display
    }

    fun createSimpleTabDisplay(minigame: Minigame<*>): PlayerListDisplay {
        val display = PlayerListDisplay(SimpleCasualPlayerListEntries(minigame))
        addCasualFooterAndHeader(minigame, display)
        return display
    }

    fun addCasualFooterAndHeader(minigame: Minigame<*>, display: PlayerListDisplay) {
        val hostedByKiwiTech = Component.empty()
            .append(SERVER_HOSTED_BY)
            .append(ComponentUtils.space())
            .append(KIWITECH)

        val baseFooter = PlayerSpecificElement<Component> { player ->
            val ping = player.connection.latency()
            val colour = when {
                ping < 80 -> DARK_GREEN
                ping < 120 -> GREEN
                ping < 180 -> YELLOW
                ping < 240 -> RED
                else -> DARK_RED
            }
            val formatted = Component.literal("$ping").withStyle(colour)
            Component.empty()
                .append(Component.translatable("casual.tab.ping", formatted).mini())
                .append("\n")
                .append(hostedByKiwiTech)
        }

        val spectatorAndAdmins = SpectatorAndAdminTeamsElement(minigame).cached()
        val footer = spectatorAndAdmins.merge<_, Component>(baseFooter) { a, b ->
            a.map { Component.empty().append(it).append("\n\n") }.orElse(Component.empty()).append(b)
        }
        display.setDisplay(
            ComponentElements.of("\n".literal().append(CASUAL).append(" ").append(CHAMPIONSHIPS).append("\n")),
            footer
        )
    }

    fun createTeamSelectionGui(minigame: Minigame<*>, player: ServerPlayer): TeamSelectorGui {
        val selections = minigame.teams.getAllNonSpectatorOrAdminTeams().sortedBy { it.name }.map {
            val flag = DisplayItems.FLAG
            val color = it.getHexColor()
            if (color != null) {
                flag.set(DataComponents.DYED_COLOR, DyedItemColor(color, false))
            }
            flag.set(DataComponents.CUSTOM_NAME, it.formattedDisplayName.mini())
            TeamSelectorGui.Selection(it, flag)
        }
        return TeamSelectorGui(player, selections)
    }
}