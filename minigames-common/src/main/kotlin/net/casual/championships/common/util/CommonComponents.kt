package net.casual.championships.common.util

import net.casual.arcade.resources.font.FontResources
import net.casual.arcade.resources.font.IndexedFontResources
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.crimson
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.ComponentUtils.translatable
import net.casual.arcade.utils.ComponentUtils.translatableWithArgs
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ComponentUtils.withSpacingFont
import net.casual.championships.common.CommonMod
import net.minecraft.core.Direction
import net.minecraft.core.Direction8
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object CommonComponents {
    val WELCOME = translatableWithArgs("casual.welcome")
    val YES by translatable("casual.yes")
    val NO by translatable("casual.no")
    val GOOD_LUCK by translatable("casual.goodLuck")

    val HOSTED_BY_MESSAGE by translatable("casual.hostedBy")

    val TOGGLE_TEAMGLOW = translatableWithArgs("casual.tags.teamglow")
    val TOGGLE_FULLBRIGHT = translatableWithArgs("casual.tags.fullbright")

    val ENABLE by translatable("casual.toggle.enable") { lime() }
    val DISABLE by translatable("casual.toggle.disable") { crimson() }
    val ENABLED by translatable("casual.toggle.enabled") { lime() }
    val DISABLED by translatable("casual.toggle.disabled") { crimson() }

    val PREVIOUS by translatable("casual.gui.previous")
    val NEXT by translatable("casual.gui.next")
    val EXIT by translatable("casual.gui.exit")
    val BACK by translatable("casual.gui.back")
    val SPECTATOR_TITLE by translatable("casual.gui.spectator.title")
    val SPECTATORS by translatable("casual.gui.spectators")
    val ADMINS by translatable("casual.gui.admins")
    val CONFIGURE by translatable("casual.gui.configure")
    val CONFIRM by translatable("casual.gui.confirm")
    val CANCEL by translatable("casual.gui.cancel")

    val TEAMS by translatable("casual.team.teams")
    val TEAMMATES by translatable("casual.team.teammates")
    val ADDED_TO_TEAM = translatableWithArgs("casual.team.added")

    val NO_TEAM by translatable("casual.team.noTeam")
    val NOT_SPECTATING by translatable("casual.spectator.notSpectating")

    val BROADCAST_POSITION = translatableWithArgs("casual.position.broadcast")

    val MINESWEEPER_DESC_1 by translatable("casual.minesweeper.desc.1")
    val MINESWEEPER_DESC_2 by translatable("casual.minesweeper.desc.2")
    val MINESWEEPER_DESC_3 by translatable("casual.minesweeper.desc.3")
    val MINESWEEPER_DESC_4 by translatable("casual.minesweeper.desc.4")
    val MINESWEEPER_FLAGS by translatable("casual.minesweeper.flags")
    val MINESWEEPER_MINE by translatable("casual.minesweeper.mine")
    val MINESWEEPER_FLAG by translatable("casual.minesweeper.flag")
    val MINESWEEPER_TIMER by translatable("casual.minesweeper.timer")
    val MINESWEEPER_PLAY_AGAIN by translatable("casual.minesweeper.playAgain")
    val MINESWEEPER_WON = translatableWithArgs("casual.minesweeper.won")
    val MINESWEEPER_LOST by translatable("casual.minesweeper.lost")
    val MINESWEEPER_RECORD = translatableWithArgs("casual.minesweeper.record")

    val DIRECTION by translatable("casual.game.direction")
    val NORTH by translatable("casual.game.direction.north")
    val NORTH_EAST by translatable("casual.game.direction.northEast")
    val EAST by translatable("casual.game.direction.east")
    val SOUTH_EAST by translatable("casual.game.direction.southEast")
    val SOUTH by translatable("casual.game.direction.south")
    val SOUTH_WEST by translatable("casual.game.direction.southWest")
    val WEST by translatable("casual.game.direction.west")
    val NORTH_WEST by translatable("casual.game.direction.northWest")

    val BORDER_INFO by translatable("casual.game.borderInfo")
    val BORDER_DISTANCE by translatable("casual.game.borderDistance")
    val BORDER_RADIUS by translatable("casual.game.borderRadius")
    val INSIDE_BORDER = translatableWithArgs("casual.game.insideBorder")
    val BORDER_INITIAL_GRACE = translatableWithArgs("casual.game.grace.first")
    val BORDER_GENERIC_GRACE = translatableWithArgs("casual.game.grace.generic")
    val BORDER_GRACE_OVER by translatable("casual.game.grace.over")
    val BORDER_RESUMED by translatable("casual.game.borderResumed")
    val BORDER_PAUSED by translatable("casual.game.borderPaused")
    val HAS_BEEN_ELIMINATED = translatableWithArgs("casual.game.eliminated")
    val GAME_WON = translatableWithArgs("casual.game.won")

    val GOLDEN_HEAD by translatable("casual.item.goldenHead")
    val HEAD_TOOLTIP by translatable("casual.item.head.tooltip")

    val STARTING_IN = translatableWithArgs("casual.game.startingIn")
    val STARTING_SOON by translatable("casual.game.startingSoon")
    val TIME_ELAPSED = translatableWithArgs("casual.game.timeElapsed")
    val GRACE = translatableWithArgs("casual.game.grace")
    val GLOWING = translatableWithArgs("casual.game.glowing")

    val STARTING_IN_BACKGROUNDED = backgrounded(STARTING_IN, Hud.BACKGROUND_240) { mini() }
    val STARTING_SOON_BACKGROUNDED = backgrounded({ STARTING_SOON }, Hud.BACKGROUND_240) { mini() }
    val TIME_ELAPSED_BACKGROUNDED = backgrounded(TIME_ELAPSED, Hud.BACKGROUND_240) { mini() }
    val GRACE_BACKGROUNDED = backgrounded(GRACE, Hud.BACKGROUND_180) { mini() }
    val GLOWING_BACKGROUNDED = backgrounded(GLOWING, Hud.BACKGROUND_180) { mini() }

    fun backgrounded(
        generator: ComponentUtils.ComponentGenerator,
        background: Component,
        modifier: (MutableComponent.() -> Unit)? = null
    ): ComponentUtils.ComponentGenerator {
        val key = ComponentUtils.getTranslationKeyOf(generator.generate())
        return ComponentUtils.ComponentGenerator {
            Component.empty().apply {
                append(Component.translatable("$key.space.1").withSpacingFont())
                append(background.copy().shadowless())
                append(Component.translatable("$key.space.2").withSpacingFont())
                val component = generator.generate(*it)
                modifier?.invoke(component)
                append(component)
            }
        }
    }

    fun direction(direction: Direction): MutableComponent {
        return when (direction) {
            Direction.NORTH -> NORTH
            Direction.EAST -> EAST
            Direction.SOUTH -> SOUTH
            Direction.WEST -> WEST
            else -> Component.literal(direction.getName())
        }
    }

    fun direction(direction: Direction8): MutableComponent {
        return when (direction) {
            Direction8.NORTH -> NORTH
            Direction8.NORTH_EAST -> NORTH_EAST
            Direction8.EAST -> EAST
            Direction8.SOUTH_EAST -> SOUTH_EAST
            Direction8.SOUTH -> SOUTH
            Direction8.SOUTH_WEST -> SOUTH_WEST
            Direction8.WEST -> WEST
            Direction8.NORTH_WEST -> NORTH_WEST
        }
    }

    object Gui: FontResources(CommonMod.id("gui_font")) {
        val GENERIC_54 = bitmap(at("gui/generic_54.png"), 13, 256)
        val TEAM_SELECTOR = bitmap(at("gui/team_selector.png"), 13, 256)
        val TEAM_PLAYER_SELECTOR = bitmap(at("gui/team_player_selector.png"), 13, 256)
        val PLAYER_SELECTOR = bitmap(at("gui/player_selector.png"), 13, 256)

        val DUELS = bitmap(at("gui/duels_menu.png"), 13, 256)
        val DUEL_SETTINGS = bitmap(at("gui/duels_settings_menu.png"), 13, 256)

        val MINESWEEPER_MENU = bitmap(at("gui/minesweeper_menu.png"), 19, 227)

        @JvmStatic
        fun createDoubleChestGui(title: Component): MutableComponent {
            return Component.empty()
                .append(SpacingFontResources.spaced(-8))
                .append(GENERIC_54.copy().white())
                .append(SpacingFontResources.spaced(-169))
                .append(title)
        }
    }

    object Hud: FontResources(CommonMod.id("hud_font")) {
        val BACKGROUND_240 = bitmap(at("backgrounds/background240.png"), 10, 12)
        val BACKGROUND_180 = bitmap(at("backgrounds/background180.png"), 10, 12)
        val BACKGROUND_120 = bitmap(at("backgrounds/background120.png"), 10, 12)
        val BACKGROUND_40 = bitmap(at("backgrounds/background40.png"), 10, 12)
        val HARDCORE_HEART = bitmap(at("hud/hardcore_heart.png"), 7, 7)
        val NO_CONNECTION = bitmap(at("hud/no_connection.png"), 7, 7)
        val UNAVAILABLE = bitmap(at("hud/cross.png"), 7, 7)
        val KILLS_COUNT = bitmap(at("hud/kills.png"), 8, 8)
        val PLAYER_COUNT = bitmap(at("hud/players.png"), 8, 8)
        val EPIC_CHAT_ICON = bitmap(at("hud/epic_chat_icon.png"), 7, 7)
        val EPIC_CHAT_ICON_M54 = bitmap(at("hud/epic_chat_icon.png"), -56, 7)
    }

    object Text: FontResources(CommonMod.id("text_font")) {
        val CASUAL = bitmap(at("text/casual.png"), 8, 9)
        val CHAMPIONSHIPS = bitmap(at("text/championships.png"), 8, 9)

        val SERVER_HOSTED_BY = bitmap(at("text/server_hosted_by.png"), 8, 9)
        val KIWITECH = bitmap(at("text/kiwitech.png"), 8, 9)

        val WELCOME_TO_CASUAL_CHAMPIONSHIPS = bitmap(at("text/welcome_to_casual_championships.png"), 6, 16)
    }

    object Border: IndexedFontResources(CommonMod.id("border_font")) {
        val BORDER_DISTANCE = bitmap(at("border/border_distance.png"), 8, 8)
        val BORDER_RADIUS = bitmap(at("border/border_radius.png"), 8, 8)

        init {
            for (i in 1..5) {
                indexed { bitmap(at("border/blue_border_$i.png"), 8, 8) }
            }
            for (i in 1..5) {
                indexed { bitmap(at("border/red_border_$i.png"), 8, 8) }
            }
            for (i in 1..5) {
                indexed { bitmap(at("border/green_border_$i.png"), 8, 8) }
            }
        }

        fun blue(index: Int): Component {
            return this.get(index - 1)
        }

        fun red(index: Int): Component {
            return this.get(index + 4)
        }

        fun green(index: Int): Component {
            return this.get(index + 9)
        }
    }
}