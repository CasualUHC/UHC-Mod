package net.casual.championships.uhc

import net.casual.arcade.dimensions.level.vanilla.VanillaDimension
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder.Companion.bool
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder.Companion.enumeration
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder.Companion.float64
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder.Companion.time
import net.casual.arcade.utils.ItemUtils.hideAttributeTooltips
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.utils.ItemUtils.potion
import net.casual.arcade.utils.TimeUtils.Minutes
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.minigame.CasualSettings
import net.casual.championships.uhc.border.UHCBorderSize
import net.casual.championships.uhc.border.UHCBorderStage
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions

class UHCSettings(private val uhc: UHCMinigame): CasualSettings(uhc) {
    var glowing by this.register(bool {
        name = "glowing"
        display = Items.GLOWSTONE_DUST.named("Glowing")
        value = false
        defaults.options(this)
        listener { _, _, _ ->
            for (player in uhc.players.playing) {
                player.setGlowingTag(!player.hasGlowingTag())
                player.setGlowingTag(!player.hasGlowingTag())
            }
        }
    })

    var borderSizeMultiplier by this.register(float64 {
        name = "border_size_multiplier"
        display = DisplayItems.BORDER_RADIUS.named("Border Size Multiplier")
        value = 1.0
        option("one_third", Items.SCAFFOLDING.named("0.33x Size"), 1.0 / 3.0)
        option("half", Items.ANVIL.named("0.5x Size"), 0.5)
        option("two_thirds", Items.GREEN_STAINED_GLASS_PANE.named("0.66x Size"), 2.0 / 3.0)
        option("normal", Items.LIME_STAINED_GLASS_PANE.named("1x Size"), 1.0)
        option("three_halves", Items.RED_STAINED_GLASS_PANE.named("1.5x Size"), 1.5)
        option("double", Items.RED_STAINED_GLASS_PANE.named("2x Size"), 2.0)
    })

    var borderTime by this.register(time {
        name = "border_completion_time"
        display = DisplayItems.BORDER_DISTANCE.named("Border Completion Time")
        value = 120.Minutes
        option("ten_minutes", Items.CAKE.named("10 Minutes"), 10.Minutes)
        option("thirty_minutes", Items.SCULK_SENSOR.named("30 Minutes"), 30.Minutes)
        option("two_hours", Items.GREEN_STAINED_GLASS_PANE.named("2 Hours"), 120.Minutes)
        option("two_and_half_hours", Items.YELLOW_STAINED_GLASS_PANE.named("2.5 Hours"), 150.Minutes)
        option("three_hours", Items.RED_STAINED_GLASS_PANE.named("3 Hours"), 180.Minutes)
    })

    var startingDimension by this.register(enumeration<VanillaDimension> {
        name = "starting_dimension"
        display = Items.GRASS_BLOCK.named("Starting Dimension")
        value = VanillaDimension.Overworld
        defaults.options(this, VanillaDimension::class.java)
    })

    var gracePeriod by this.register(time {
        name = "grace_period"
        display = Items.SHIELD.named("Grace Period")
        value = 10.Minutes
        option("one_minute", Items.CAKE.named("1 Minute"), 1.Minutes)
        option("two_minutes", Items.CAKE.named("2 Minutes"), 2.Minutes)
        option("five_minutes", Items.CAKE.named("5 Minutes"), 5.Minutes)
        option("ten_minutes", Items.CAKE.named("10 Minutes"), 10.Minutes)
        option("twenty_minutes", Items.CAKE.named("20 Minutes"), 20.Minutes)
    })

    var portalEscapeTime by this.register(time {
        name = "portal_escape_time"
        display = Items.OBSIDIAN.named("Portal Escape Time")
        value = 30.Seconds
        option("none", Items.CLOCK.named("None"), 0.Seconds)
        option("ten_seconds", Items.CLOCK.named("10 Seconds"), 10.Seconds)
        option("twenty_seconds", Items.CLOCK.named("20 Second"), 20.Seconds)
        option("thirty_seconds", Items.CLOCK.named("30 Seconds"), 30.Seconds)
        option("sixty_seconds", Items.CLOCK.named("60 Seconds"), 60.Seconds)
    })

    var bowCooldown by this.register(time {
        name = "bow_cooldown"
        display = Items.BOW.named("Bow Cooldown")
        value = 1.Seconds
        option("none", Items.CLOCK.named("None"), 0.Seconds)
        option("half_second", Items.CLOCK.named("0.5 Seconds"), 10.Ticks)
        option("one_second", Items.CLOCK.named("1 Second"), 1.Seconds)
        option("two_seconds", Items.CLOCK.named("2 Seconds"), 2.Seconds)
        option("three_seconds", Items.CLOCK.named("3 Seconds"), 3.Seconds)
        option("five_seconds", Items.CLOCK.named("5 Seconds"), 5.Seconds)
    })

    var health by this.register(float64 {
        name = "health"
        display = Items.POTION.named("Health").potion(Potions.HEALING).hideAttributeTooltips()
        value = 1.0
        option("triple", Items.GREEN_STAINED_GLASS_PANE.named("Triple"), 2.0)
        option("double", Items.YELLOW_STAINED_GLASS_PANE.named("Double"), 1.0)
        option("normal", Items.RED_STAINED_GLASS_PANE.named("Normal"), 0.0)
    })

    var endGameGlow by this.register(bool {
        name = "end_game_glow"
        display = Items.SPECTRAL_ARROW.named("End Game Glow")
        value = true
        defaults.options(this)
    })

    var friendlyPlayerGlow by this.register(bool {
        name = "friendly_player_glow"
        display = Items.GOLDEN_CARROT.named("Friendly Player Glow")
        value = true
        defaults.options(this)
    })

    var playerDropsGapple by this.register(bool {
        name = "player_drops_gapple"
        display = Items.GOLDEN_APPLE.named("Player Drops Gapple")
        value = false
        defaults.options(this)
    })

    var playerDropsHead by this.register(bool {
        name = "player_drops_head"
        display = Items.PLAYER_HEAD.named("Player Drops Head")
        value = true
        defaults.options(this)
    })

    var opPotions by this.register(bool {
        name = "op_potions"
        display = Items.SPLASH_POTION.named("OP Potions").potion(Potions.STRONG_HARMING).hideAttributeTooltips()
        value = false
        defaults.options(this)
    })

    var generatePortals by this.register(bool {
        name = "generate_portals"
        display = Items.CRYING_OBSIDIAN.named("Generate Portals")
        value = true
        defaults.options(this)
    })

    var soloBuff by this.register(bool {
        name = "solo_buff"
        display = Items.LINGERING_POTION.named("Solo Buff").potion(Potions.REGENERATION).hideAttributeTooltips()
        value = true
        defaults.options(this)
    })

    var borderSize by this.register(enumeration<UHCBorderSize> {
        name = "border_size"
        display = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE.named("World Border Size").hideAttributeTooltips()
        value = UHCBorderSize.Start
        defaults.options(this, UHCBorderSize::class.java)
    })

    var borderStageSetting = this.register(enumeration<UHCBorderStage> {
        name = "border_stage"
        display = Items.BARRIER.named("World Border Stage")
        value = UHCBorderStage.First
        defaults.options(this, UHCBorderStage::class.java)
        listener { setting, _, value ->
            setting.setQuietly(value)
            uhc.moveWorldBorders(value, borderSize, true)
        }
    })

    var replay by this.register(bool {
        name = "replay"
        display = Items.END_PORTAL_FRAME.named("Server Replay")
        value = true
        defaults.options(this)
    })

    var borderStage by this.borderStageSetting
}