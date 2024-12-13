package net.casual.championships.uhc.advancement

import com.google.gson.JsonObject
import net.casual.arcade.events.BuiltInEventPhases
import net.casual.arcade.events.server.entity.EntityDeathEvent
import net.casual.arcade.events.server.player.*
import net.casual.arcade.minigame.annotation.During
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.annotation.ListenerFlags
import net.casual.arcade.minigame.annotation.ListenerFlags.IS_PLAYING
import net.casual.arcade.minigame.annotation.MinigameEventListener
import net.casual.arcade.minigame.stats.ArcadeStats
import net.casual.arcade.minigame.stats.Stat.Companion.increment
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.scheduler.task.impl.PlayerTask
import net.casual.arcade.utils.ItemUtils.isOf
import net.casual.arcade.utils.JsonUtils.array
import net.casual.arcade.utils.JsonUtils.toJsonStringArray
import net.casual.arcade.utils.PlayerUtils.getKillCreditWith
import net.casual.arcade.utils.PlayerUtils.grantAdvancement
import net.casual.arcade.utils.PlayerUtils.isSurvival
import net.casual.arcade.utils.TimeUtils.Minutes
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.isInStructure
import net.casual.championships.common.event.PlayerCheatEvent
import net.casual.championships.common.util.CommonStats
import net.casual.championships.common.util.CommonTags
import net.casual.championships.uhc.GAME_OVER_ID
import net.casual.championships.uhc.UHCMinigame
import net.casual.championships.uhc.UHCPhase
import net.casual.championships.uhc.UHCStats
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.inventory.FurnaceMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ConcretePowderBlock
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.levelgen.structure.BuiltinStructures

class UHCAdvancementManager(
    private val uhc: UHCMinigame
): MinigameEventListener {
    private val claimed = HashSet<UHCRaceAdvancement>()

    fun grantFinalAdvancements(winners: List<ServerPlayer>) {
        val alive = winners.filter { it.isSurvival }
        if (alive.size == 1) {
            alive[0].grantAdvancement(UHCAdvancements.LAST_MAN_STANDING)
        }

        for (player in winners) {
            player.grantAdvancement(UHCAdvancements.WINNER)
            this.uhc.stats.getOrCreateStat(player, CommonStats.WON).modify { true }
        }

        val lowest = DamageCounter(Float.POSITIVE_INFINITY)
        val highest = DamageCounter(Float.NEGATIVE_INFINITY)
        for (player in this.uhc.players) {
            if (this.uhc.tags.has(player, CommonTags.HAS_PARTICIPATED)) {
                val current = this.uhc.stats.getOrCreateStat(player, ArcadeStats.DAMAGE_DEALT).value
                if (lowest.damage > current) {
                    lowest.players.clear()
                    lowest.players.add(player)
                    lowest.damage = current
                } else if (lowest.damage == current) {
                    lowest.players.add(player)
                }

                if (highest.damage < current) {
                    highest.players.clear()
                    highest.players.add(player)
                    highest.damage = current
                } else if (highest.damage == current) {
                    highest.players.add(player)
                }
            }
        }
        for (player in lowest.players) {
            player.grantAdvancement(UHCAdvancements.MOSTLY_HARMLESS)
        }
        for (player in highest.players) {
            player.grantAdvancement(UHCAdvancements.HEAVY_HITTER)
        }
    }

    fun serialize(): JsonObject {
        val json = JsonObject()
        json.add("claimed", this.claimed.toJsonStringArray { it.name })
        return json
    }

    fun deserialize(json: JsonObject) {
        for (claimed in json.array("claimed")) {
            this.claimed.add(UHCRaceAdvancement.valueOf(claimed.asString))
        }
    }

    @Listener(flags = IS_PLAYING, priority = 2000, during = During(before = GAME_OVER_ID))
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val relogs = this.uhc.stats.getOrCreateStat(event.player, ArcadeStats.RELOGS).value
        // Wait for player to load in
        GlobalTickedScheduler.schedule(1.Seconds, PlayerTask(event.player) { player ->
            player.grantAdvancement(UHCAdvancements.COMBAT_LOGGER)
            if (relogs == 10) {
                player.grantAdvancement(UHCAdvancements.OK_WE_BELIEVE_YOU_NOW)
            }
        })

        val team = event.player.team
        if (team !== null && this.uhc.teams.isTeamEliminated(team)) {
            this.uhc.teams.removeEliminatedTeam(team)
            event.player.grantAdvancement(UHCAdvancements.TEAM_PLAYER)
        }
    }

    @Listener(
        phase = BuiltInEventPhases.POST,
        flags = IS_PLAYING,
        during = During(before = GAME_OVER_ID)
    )
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        val (player, source) = event

        if (this.claimed.add(UHCRaceAdvancement.Death)) {
            player.grantAdvancement(UHCAdvancements.EARLY_EXIT)
        }

        if (source.`is`(DamageTypes.OUTSIDE_BORDER)) {
            player.grantAdvancement(UHCAdvancements.SKILL_ISSUE)
        } else if (source.`is`(DamageTypes.FELL_OUT_OF_WORLD)) {
            player.grantAdvancement(UHCAdvancements.WELL_THAT_WAS_A_BIT_SILLY)
        }

        val killer = player.getKillCreditWith(source)
        if (killer is ServerPlayer && this.uhc.players.has(killer)) {
            if (this.claimed.add(UHCRaceAdvancement.Kill)) {
                killer.grantAdvancement(UHCAdvancements.FIRST_BLOOD)
            }
            if (this.uhc.stats.getOrCreateStat(killer, ArcadeStats.KILLS).value >= 10) {
                killer.grantAdvancement(UHCAdvancements.OKAY_EZTAK)
            }
        } else if (killer is Warden) {
            player.grantAdvancement(UHCAdvancements.BEAR_CARED)
        } else if (killer is IronGolem) {
            player.grantAdvancement(UHCAdvancements.COW_MOMENT)
        }

        if (player.level().dimension() == this.uhc.nether.dimension()) {
            event.player.grantAdvancement(UHCAdvancements.WHERE_WAS_MY_PORTAL)
        }
    }

    @Listener(
        flags = IS_PLAYING,
        during = During(before = GAME_OVER_ID),
        phase = BuiltInEventPhases.POST
    )
    private fun onPlayerBlockPlaced(event: PlayerBlockPlacedEvent) {
        val state = event.state
        val block = state.block
        val context = event.context
        val pos = context.clickedPos
        val world = context.level
        if (block is FallingBlock && FallingBlock.isFree(world.getBlockState(pos.below()))) {
            event.player.grantAdvancement(UHCAdvancements.FALLING_BLOCK)
        } else if (block === Blocks.REDSTONE_WIRE) {
            event.player.grantAdvancement(UHCAdvancements.NOT_DUSTLESS)
        } else if (block === Blocks.TNT) {
            event.player.grantAdvancement(UHCAdvancements.DEMOLITION_EXPERT)
        } else if (block === Blocks.POWERED_RAIL) {
            event.player.grantAdvancement(UHCAdvancements.UPDATE_DEPRESSION)
        } else if (block === Blocks.SLIME_BLOCK) {
            event.player.grantAdvancement(UHCAdvancements.SLIMESTONER)
        } else if (block === Blocks.HOPPER) {
            event.player.grantAdvancement(UHCAdvancements.STORAGE_TECH_IS_MY_PASSION)
        } else if (block === Blocks.CHEST && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
            event.player.grantAdvancement(UHCAdvancements.MAIN_STORAGE)
        }


        val blocksPlaced = this.uhc.stats.getOrCreateStat(event.player, CommonStats.BLOCKS_PLACED)
        blocksPlaced.increment()
        if (blocksPlaced.value >= 500) {
            event.player.grantAdvancement(UHCAdvancements.PRINTER_ISNT_ALLOWED)
        }
    }

    @Listener(flags = IS_PLAYING, during = During(before = GAME_OVER_ID))
    private fun onPlayerBlockMined(event: PlayerBlockMinedEvent) {
        val blocksMined = this.uhc.stats.getOrCreateStat(event.player, CommonStats.BLOCKS_MINED)
        blocksMined.increment()
        if (blocksMined.value >= 2500) {
            event.player.grantAdvancement(UHCAdvancements.HUMAN_QUARRY)
        }

        if (event.state.block == Blocks.BEE_NEST) {
            event.player.grantAdvancement(UHCAdvancements.BEE_NES)
        }
    }

    @Listener(flags = IS_PLAYING, during = During(before = GAME_OVER_ID))
    private fun onEntityDeath(event: EntityDeathEvent) {
        val (entity, source) = event
        val attacker = source.entity
        if (attacker is ServerPlayer) {
            if (entity is Villager) {
                attacker.grantAdvancement(UHCAdvancements.TELL_WITNESSES_THAT_I_WAS_MURDERED)
            } else if (entity is EnderDragon) {
                attacker.grantAdvancement(UHCAdvancements.DO_THE_IMPOSSIBLE)
            }
        }
    }

    @Listener(flags = IS_PLAYING, during = During(before = GAME_OVER_ID))
    private fun onPlayerCraft(event: PlayerCraftEvent) {
        if (event.stack.isOf(Items.CRAFTING_TABLE)) {
            if (this.claimed.add(UHCRaceAdvancement.Craft)) {
                event.player.grantAdvancement(UHCAdvancements.WORLD_RECORD_PACE)
            }
        } else if (event.stack.isOf(Items.PAPER)) {
            event.player.grantAdvancement(UHCAdvancements.DOES_THIS_WORK_ON_PAPER)
        }
    }

    @Listener(flags = IS_PLAYING, during = During(before = GAME_OVER_ID))
    private fun onPlayerLoot(event: PlayerLootEvent) {
        if (event.items.any { it.isOf(Items.ENCHANTED_GOLDEN_APPLE) }) {
            event.player.grantAdvancement(UHCAdvancements.DREAM_LUCK)
        }
    }

    @Listener(
        flags = IS_PLAYING,
        during = During(before = GAME_OVER_ID),
        phase = BuiltInEventPhases.POST
    )
    private fun onPlayerSlotClick(event: PlayerSlotClickEvent) {
        val (player, menu, index) = event
        if (menu is FurnaceMenu && index == 0) {
            val item = menu.getSlot(0).item.item
            if (item is BlockItem && item.block is ConcretePowderBlock) {
                player.grantAdvancement(UHCAdvancements.CONCRETE_SMELTER)
            }
        }
    }

    @Listener(flags = IS_PLAYING, during = During(before = GAME_OVER_ID))
    private fun onPlayerTick(event: PlayerTickEvent) {
        val player = event.player
        val stat = this.uhc.stats.getOrCreateStat(player, UHCStats.HALF_HEART_TIME)
        if (this.uhc.players.isPlaying(player)) {
            if (player.health <= 1.0F) {
                stat.increment()
                if (stat.value == 1200) {
                    player.grantAdvancement(UHCAdvancements.ON_THE_EDGE)
                }
            } else {
                stat.modify { 0 }
            }

            this.uhc.stats.getOrCreateStat(player, CommonStats.ALIVE_TIME).increment()
            if (player.isShiftKeyDown) {
                val crouchTime = this.uhc.stats.getOrCreateStat(player, CommonStats.CROUCH_TIME)
                crouchTime.increment()
                if (crouchTime.value >= 30.Minutes.ticks) {
                    player.grantAdvancement(UHCAdvancements.DOES_YOUR_PINKIE_HURT_YET)
                }
            }
        }

        if (player.isInStructure(BuiltinStructures.STRONGHOLD)) {
            if (this.uhc.phase <= UHCPhase.Grace) {
                player.grantAdvancement(UHCAdvancements.SPEEDRUN_ANY_PERCENT)
            }
            player.grantAdvancement(UHCAdvancements.THE_END_IS_NEAR)
        } else if (player.isInStructure(BuiltinStructures.IGLOO)) {
            player.grantAdvancement(UHCAdvancements.WHATS_THAT_ABOUT_BARRIERS)
        } else if (player.isInStructure(BuiltinStructures.TRIAL_CHAMBERS)) {
            player.grantAdvancement(UHCAdvancements.TRIAL_MEMBER)
        } else if (player.isInStructure(BuiltinStructures.WOODLAND_MANSION)) {
            player.grantAdvancement(UHCAdvancements.COOL_HOUSE_BRO)
        }
    }

    @Listener(flags = IS_PLAYING)
    private fun onPlayerDamage(event: PlayerDamageEvent) {
        if (this.uhc.uptime < 1200 && event.source.`is`(DamageTypes.FALL) && event.amount > 0.0F) {
            event.player.grantAdvancement(UHCAdvancements.BROKEN_ANKLES)
        } else if (event.source.`is`(DamageTypes.DROWN)) {
            event.player.grantAdvancement(UHCAdvancements.FORGOT_YOUR_DOOR)
        }
    }

    @Listener(during = During(before = GAME_OVER_ID))
    private fun onPlayerChat(event: PlayerChatEvent) {
        val message = event.message.signedContent().lowercase()
        if (this.uhc.chat.isMessageGlobal(event.player, message)) {
            if (message.contains("jndi") && message.contains("ldap")) {
                event.player.grantAdvancement(UHCAdvancements.LDAP)
            }
            if (message.contains("basically")) {
                event.player.grantAdvancement(UHCAdvancements.BASICALLY)
            }
        }
    }

    @Listener(flags = IS_PLAYING, during = During(before = GAME_OVER_ID))
    private fun onPlayerBlockCollision(event: PlayerBlockCollisionEvent) {
        if (event.state.`is`(Blocks.SWEET_BERRY_BUSH)) {
            event.player.grantAdvancement(UHCAdvancements.EMBARRASSING)
        }
    }

    @Listener(flags = ListenerFlags.HAS_PLAYER)
    private fun onPlayerAdvancement(event: PlayerAdvancementEvent) {
        val isUHCAdvancement = UHCAdvancements.contains(event.advancement)
        event.announce = event.announce && isUHCAdvancement &&
            (this.uhc.players.isPlaying(event.player) || this.uhc.phase >= UHCPhase.GameOver)
        if (isUHCAdvancement) {
            val advancementsAwarded = this.uhc.stats.getOrCreateStat(event.player, UHCStats.ADVANCEMENTS_AWARDED)
            advancementsAwarded.increment()
            if (advancementsAwarded.value >= 15) {
                event.player.grantAdvancement(UHCAdvancements.ADVANCEMENT_HUNTER)
            }
        }
    }

    @Listener(flags = IS_PLAYING)
    private fun onPlayerCheat(event: PlayerCheatEvent) {
        event.player.grantAdvancement(UHCAdvancements.BUSTED)
    }

    @Listener(flags = IS_PLAYING)
    private fun onPlayerTotem(event: PlayerTotemEvent) {
        event.player.grantAdvancement(UHCAdvancements.PERFECTLY_BALANCED)
    }

    @Listener(flags = IS_PLAYING)
    private fun onPlayerSleep(event: PlayerSleepEvent) {
        event.player.grantAdvancement(UHCAdvancements.NO_ONE_ASKED)
    }

    @Listener(flags = IS_PLAYING)
    private fun onPlayerDimensionChange(event: PlayerDimensionChangeEvent) {
        if (event.destination.dimension() == this.uhc.end.dimension()) {
            event.player.grantAdvancement(UHCAdvancements.BRAVE_CHOICE)
        }
    }

    private class DamageCounter(
        var damage: Float,
        val players: MutableList<ServerPlayer> = ArrayList(),
    )
}