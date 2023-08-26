package net.casual.minigame.uhc

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import me.senseiwells.replay.player.PlayerRecorders
import net.casual.arcade.Arcade
import net.casual.arcade.border.MultiLevelBorderListener
import net.casual.arcade.border.MultiLevelBorderTracker
import net.casual.arcade.border.TrackedBorder
import net.casual.arcade.events.block.BrewingStandBrewEvent
import net.casual.arcade.events.entity.EntityStartTrackingEvent
import net.casual.arcade.events.entity.MobCategorySpawnEvent
import net.casual.arcade.events.minigame.MinigamePauseEvent
import net.casual.arcade.events.minigame.MinigameUnpauseEvent
import net.casual.arcade.events.player.*
import net.casual.arcade.events.server.ServerRecipeReloadEvent
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.gui.display.ArcadeNameDisplay
import net.casual.arcade.gui.sidebar.ArcadeSidebar
import net.casual.arcade.gui.suppliers.ComponentSupplier
import net.casual.arcade.minigame.MinigamePhase
import net.casual.arcade.minigame.MinigameResources
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.scheduler.MinecraftTimeUnit
import net.casual.arcade.scheduler.Task
import net.casual.arcade.settings.DisplayableGameSettingBuilder
import net.casual.arcade.utils.ComponentUtils.bold
import net.casual.arcade.utils.ComponentUtils.gold
import net.casual.arcade.utils.ComponentUtils.green
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.red
import net.casual.arcade.utils.ItemUtils.literalNamed
import net.casual.arcade.utils.ItemUtils.potion
import net.casual.arcade.utils.JsonUtils.array
import net.casual.arcade.utils.JsonUtils.boolean
import net.casual.arcade.utils.JsonUtils.int
import net.casual.arcade.utils.PlayerUtils.clearPlayerInventory
import net.casual.arcade.utils.PlayerUtils.directionToNearestBorder
import net.casual.arcade.utils.PlayerUtils.directionVectorToNearestBorder
import net.casual.arcade.utils.PlayerUtils.grantAdvancement
import net.casual.arcade.utils.PlayerUtils.isSurvival
import net.casual.arcade.utils.PlayerUtils.message
import net.casual.arcade.utils.PlayerUtils.sendParticles
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.sendSubtitle
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.PlayerUtils.teamMessage
import net.casual.arcade.utils.SettingsUtils.defaultOptions
import net.casual.arcade.utils.TeamUtils.getServerPlayers
import net.casual.CasualMod
import net.casual.arcade.events.minigame.MinigameAddExistingPlayerEvent
import net.casual.arcade.events.minigame.MinigameAddNewPlayerEvent
import net.casual.arcade.gui.tab.ArcadeTabDisplay
import net.casual.arcade.utils.*
import net.casual.arcade.utils.ComponentUtils.aqua
import net.casual.minigame.uhc.advancement.RaceAdvancement
import net.casual.minigame.uhc.advancement.UHCAdvancements
import net.casual.events.border.BorderEntityPortalEntryPointEvent
import net.casual.events.border.BorderPortalWithinBoundsEvent
import net.casual.events.player.PlayerFlagEvent
import net.casual.extensions.PlayerFlag
import net.casual.extensions.PlayerFlagsExtension.Companion.flags
import net.casual.extensions.PlayerStat
import net.casual.extensions.PlayerStatsExtension.Companion.uhcStats
import net.casual.extensions.PlayerUHCExtension.Companion.uhc
import net.casual.extensions.TeamFlag
import net.casual.extensions.TeamFlagsExtension.Companion.flags
import net.casual.extensions.TeamUHCExtension.Companion.uhc
import net.casual.items.MinesweeperItem
import net.casual.managers.DataManager
import net.casual.managers.TeamManager
import net.casual.managers.TeamManager.hasAlivePlayers
import net.casual.minigame.CasualMinigame
import net.casual.minigame.uhc.UHCPhase.*
import net.casual.minigame.uhc.events.DefaultUHC
import net.casual.minigame.uhc.gui.BorderDistanceRow
import net.casual.minigame.uhc.gui.TeammateRow
import net.casual.recipes.GoldenHeadRecipe
import net.casual.screen.MinesweeperScreen
import net.casual.minigame.uhc.events.UHCEvent
import net.casual.minigame.uhc.task.*
import net.casual.util.*
import net.casual.util.DirectionUtils.opposite
import net.casual.util.Texts.monospaced
import net.casual.util.UHCPlayerUtils.belongsToTeam
import net.casual.util.UHCPlayerUtils.isAliveSolo
import net.casual.util.UHCPlayerUtils.isMessageGlobal
import net.casual.util.UHCPlayerUtils.sendResourcePack
import net.casual.util.UHCPlayerUtils.setForUHC
import net.casual.util.UHCPlayerUtils.updateGlowingTag
import net.casual.util.shapes.ArrowShape
import net.minecraft.ChatFormatting.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.effect.MobEffects.NIGHT_VISION
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.scores.Team
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.math.atan2

class UHCMinigame(
    server: MinecraftServer,
    event: UHCEvent = DefaultUHC
): CasualMinigame(
    ResourceUtils.id("uhc_minigame"),
    server,
    Config.resolve("uhc_minigame.json")
), MultiLevelBorderListener {
    private val tracker = MultiLevelBorderTracker()

    private val claimed = HashSet<RaceAdvancement>()

    private val display = this.createTabDisplay()
    private var sidebar: ArcadeSidebar? = null
    private var health: ArcadeNameDisplay? = null

    private var glowing = false

    val settings = Settings()

    var event: UHCEvent = event
        private set

    var startTime = Long.MAX_VALUE
        private set
    var uptime = 0
        private set

    init {
        this.registerEvent<ServerTickEvent> { this.onServerTick() }
        this.registerEvent<ServerRecipeReloadEvent> { this.onRecipeReload(it) }

        this.registerMinigameEvent<MinigamePauseEvent> { this.onPause() }
        this.registerMinigameEvent<MinigameUnpauseEvent> { this.onUnpause() }

        this.registerMinigameEvent<MobCategorySpawnEvent> { this.onMobCategorySpawn(it) }
        this.registerMinigameEvent<EntityStartTrackingEvent> { this.onEntityStartTracking(it) }

        this.registerMinigameEvent<BrewingStandBrewEvent> { this.onBrewingStandBrew(it) }

        this.registerMinigameEvent<BorderEntityPortalEntryPointEvent> { this.onBorderEntityPortalEntryPointEvent(it) }
        this.registerMinigameEvent<BorderPortalWithinBoundsEvent> { this.onBorderWithinBoundsEvent(it) }

        this.registerMinigameEvent<PlayerAdvancementEvent> { this.onPlayerAdvancement(it) }
        this.registerMinigameEvent<PlayerAttackEvent> { this.onPlayerAttack(it) }
        this.registerMinigameEvent<PlayerChatEvent> { this.onPlayerChat(it) }
        this.registerMinigameEvent<PlayerClientboundPacketEvent> { this.onPlayerClientboundPacket(it) }
        this.registerMinigameEvent<PlayerDamageEvent> { this.onPlayerDamage(it) }
        this.registerMinigameEvent<PlayerDeathEvent> { this.onPlayerDeath(it) }
        this.registerMinigameEvent<PlayerGameModeChangeEvent> { this.onPlayerGameModeChange(it) }
        this.registerMinigameEvent<PlayerItemReleaseEvent> { this.onPlayerItemRelease(it) }
        this.registerMinigameEvent<PlayerLeaveEvent> { this.onPlayerLeave(it) }
        this.registerMinigameEvent<PlayerTickEvent> { this.onPlayerTick(it) }
        this.registerMinigameEvent<PlayerVoidDamageEvent> { this.onPlayerVoidDamage(it) }
        this.registerMinigameEvent<PlayerFlagEvent> { this.onPlayerFlag(it) }

        this.registerMinigameEvent<MinigameAddNewPlayerEvent> { this.onMinigameAddPlayer(it.player) }
        this.registerMinigameEvent<MinigameAddExistingPlayerEvent> { this.onMinigameAddPlayer(it.player) }

        this.registerUHCAdvancementEvents()
        this.initialiseBorderTracker()

        this.initialise()
        this.event.initialise(this)
    }

    fun hasUHCStarted(): Boolean {
        return this.phase >= Start
    }

    fun isLobbyPhase(): Boolean {
        return this.isPhase(Lobby)
    }

    override fun canReadyUp(): Boolean {
        return this.isPhase(Ready)
    }

    fun isActivePhase(): Boolean {
        return this.isPhase(Active)
    }

    fun setStartTime(newStartTime: Long) {
        this.startTime = newStartTime
    }

    fun startWorldBorders() {
        this.moveWorldBorders(this.settings.borderStage)
    }

    fun moveWorldBorders(stage: UHCBorderStage, size: UHCBorderSize = UHCBorderSize.END, instant: Boolean = false) {
        for ((border, level) in this.tracker.getAllTracking()) {
            this.moveWorldBorder(border, level, stage, size, instant)
        }
    }

    override fun onAllBordersComplete(borders: Map<TrackedBorder, ServerLevel>) {
        val stage = this.settings.borderStage
        CasualMod.logger.info("Finished world border stage: $stage")
        if (!this.isActivePhase()) {
            return
        }
        val next = stage.getNextStage()

        if (next == UHCBorderStage.END) {
            this.onWorldBorderComplete()
            return
        }
        this.schedulePhaseTask(10, MinecraftTimeUnit.Seconds, NextBorderTask(this))
    }

    fun pauseWorldBorders() {
        for ((border, _) in tracker.getAllTracking()) {
            this.moveWorldBorder(border, border.size)
        }
    }

    fun onGraceOver() {
        val message = Texts.UHC_GRACE_OVER.red().bold()
        PlayerUtils.forEveryPlayer { player ->
            player.sendSystemMessage(message)
            player.sendSound(SoundEvents.ENDER_DRAGON_AMBIENT)
        }
        this.server.isPvpAllowed = true
        this.settings.borderStage = UHCBorderStage.FIRST
    }

    fun onActiveEnd() {
        this.sidebar?.clearPlayers()
        this.sidebar = null
        this.health?.clearPlayers()
        this.health = null
        this.server.isPvpAllowed = false
    }

    fun onBorderFinish() {
        if (this.settings.endGameGlow) {
            this.glowing = true
            var count = 0
            PlayerUtils.forEveryPlayer { player ->
                if (player.isSurvival) {
                    player.setGlowingTag(true)
                    count++
                }
            }
            CasualMod.logger.info("$count player's are now glowing")
        }
        if (this.settings.generatePortals) {
            LevelUtils.overworld().portalForcer.createPortal(BlockPos.ZERO, Direction.Axis.X)
            LevelUtils.nether().portalForcer.createPortal(BlockPos.ZERO, Direction.Axis.X)
        }
    }

    fun isUnclaimed(achievement: RaceAdvancement): Boolean {
        return this.claimed.add(achievement)
    }

    fun resetTrackers() {
        this.uptime = 0
        this.glowing = false
        this.claimed.clear()
    }

    override fun getResources(): MinigameResources {
        return this.event.getResourcePackHandler()
    }

    override fun createTask(id: String, data: JsonObject): Task? {
        return when (id) {
            ActiveBossBarTask.ID -> ActiveBossBarTask(this)
            LobbyBossBarTask.ID -> LobbyBossBarTask(this)
            NextBorderTask.ID -> NextBorderTask(this)
            GracePeriodOverTask.ID -> GracePeriodOverTask(this)
            BorderFinishTask.ID -> BorderFinishTask(this)
            ActiveEndTask.ID -> ActiveEndTask(this)
            GlowingBossBarTask.ID -> GlowingBossBarTask(this, data.int("end"))
            GracePeriodBossBarTask.ID -> GracePeriodBossBarTask(this, data.int("end"))
            else -> null
        }
    }

    override fun getLevels(): Collection<ServerLevel> {
        return listOf(
            LevelUtils.overworld(),
            LevelUtils.nether(),
            LevelUtils.end()
        )
    }

    override fun getPhases(): Collection<MinigamePhase> {
        return UHCPhase.values().toList()
    }

    override fun readData(json: JsonObject) {
        this.glowing = json.boolean("glowing")
        this.server.isPvpAllowed = json.boolean("pvp")

        if (this.isActivePhase()) {
            this.uptime = json.int("uptime")
            for (claimed in json.array("claimed")) {
                this.claimed.add(RaceAdvancement.valueOf(claimed.asString))
            }
        }
    }

    override fun writeData(json: JsonObject) {
        json.addProperty("glowing", this.glowing)
        json.addProperty("pvp", this.server.isPvpAllowed)

        if (this.isActivePhase()) {
            json.addProperty("uptime", this.uptime)
            val claimed = this.claimed.stream().collect(::JsonArray, { a, r -> a.add(r.name) }, JsonArray::addAll)
            json.add("claimed", claimed)
        }
    }

    fun onSetup() {
        this.setPhase(Setup)
        TeamManager.createTeams()
    }

    fun onReady() {
        this.setPhase(Ready)
        for (team in TeamUtils.teams()) {
            team.flags.set(TeamFlag.Ready, false)
        }

        val bar = Component.literal("══════════════════").gold()
        val yes = Component.literal("[").append(Texts.LOBBY_YES).append("]").bold().green().withStyle {
            it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ready yes"))
        }
        val no = Component.literal("[").append(Texts.LOBBY_NO).append("]").bold().red().withStyle {
            it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ready no"))
        }
        val readyMessage = bar.copy()
            .append("\n      ")
            .append(Texts.LOBBY_READY_QUESTION)
            .append("\n\n\n       ")
            .append(yes)
            .append("        ")
            .append(no)
            .append("\n\n\n")
            .append(bar)

        PlayerUtils.forEveryPlayer { player ->
            val team = player.team
            if (team != null && !team.flags.has(TeamFlag.Ignored)) {
                player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 1.0F, 1.0F)
                player.sendSystemMessage(readyMessage)
            }
        }
    }

    fun onLobby() {
        this.setPhase(Lobby)

        RuleUtils.setLobbyGamerules(Arcade.server)

        val handler = this.event.getMinigameLobby()
        handler.getMap().place()

        PlayerUtils.forEveryPlayer { player ->
            if (!player.hasPermissions(4)) {
                player.setGameMode(GameType.ADVENTURE)
                handler.tryTeleport(player)
            }
        }
        TeamUtils.forEachTeam { team ->
            for (player in team.uhc.players) {
                Arcade.server.scoreboard.addPlayerToTeam(player, team)
            }
        }

        this.schedulePhaseEndTask(LobbyBossBarTask(this))

        TeamManager.setCollisions(false)
        for (team in Arcade.server.scoreboard.playerTeams) {
            team.nameTagVisibility = Team.Visibility.ALWAYS
        }

        this.moveWorldBorders(UHCBorderStage.FIRST, UHCBorderSize.START, true)
    }

    fun onStart() {
        this.setPhase(Start)
        this.setStartTime(Long.MAX_VALUE)

        // Countdown
        var starting = 10
        this.scheduleInLoopPhaseTask(0, 1, 10, MinecraftTimeUnit.Seconds) {
            val title = Component.literal(starting--.toString()).lime()
            PlayerUtils.forEveryPlayer { player ->
                player.sendTitle(title)
                player.sendSound(SoundEvents.NOTE_BLOCK_PLING.value(), pitch = 3.0F)
            }
        }
        this.schedulePhaseTask(10, MinecraftTimeUnit.Seconds) {
            PlayerUtils.forEveryPlayer { player ->
                player.sendResourcePack(this.event.getResourcePackHandler())
            }
            TeamUtils.forEachTeam { team ->
                team.uhc.players.clear()
            }

            val area = this.event.getMinigameLobby().getMap()
            area.removeEntities { it !is Player }
            area.remove()

            this.onActive()
            val players = PlayerUtils.players().filter { player ->
                val team = player.team
                team !== null && !team.flags.has(TeamFlag.Ignored)
            }
            val overworld = LevelUtils.overworld()
            PlayerUtils.spread(overworld, Vec2(0.0F, 0.0F), 500.0, 2900.0, true, players)

            PlayerUtils.forEveryPlayer { player ->
                player.setForUHC(this, true)
                if (player.isSpectator) {
                    if (player.level() != overworld) {
                        player.teleportTo(overworld, 0.0, 200.0, 0.0, 0.0F, 0.0F)
                    }
                    this.health?.addPlayer(player)
                }
            }
        }
    }

    fun onActive() {
        this.setPhase(Active)
        this.resetTrackers()
        RuleUtils.setActiveGamerules(Arcade.server)

        this.schedulePhaseEndTask(ActiveBossBarTask(this))

        PlayerUtils.forEveryPlayer { player ->
            player.sendSystemMessage(Texts.UHC_GRACE_FIRST.gold())
            player.sendSound(SoundEvents.NOTE_BLOCK_PLING.value())
        }
        val end = this.uptime + MinecraftTimeUnit.Minutes.toTicks(10)

        this.schedulePhaseTask(10, MinecraftTimeUnit.Minutes, GracePeriodOverTask(this))
        val barTask = GracePeriodBossBarTask(this, end)
        this.schedulePhaseTask(10, MinecraftTimeUnit.Minutes, barTask)
        this.schedulePhaseEndTask(barTask)

        this.schedulePhaseEndTask(ActiveEndTask(this))

        TeamManager.setCollisions(true)
        for (team in Arcade.server.scoreboard.playerTeams) {
            if (team.flags.has(TeamFlag.Ignored)) {
                team.nameTagVisibility = Team.Visibility.NEVER
            }
        }
    }

    fun onEnd() {
        this.setPhase(End)
        val team = TeamManager.getAnyAliveTeam()
        if (team == null) {
            CasualMod.logger.error("Last team was null!")
            return
        }
        val alive = team.getServerPlayers().filter { it.isSurvival }
        if (alive.size == 1) {
            alive[0].grantAdvancement(UHCAdvancements.LAST_MAN_STANDING)
        }

        for (player in alive) {
            player.abilities.mayfly = true
            player.abilities.invulnerable = true
            player.onUpdateAbilities()
        }

        PlayerUtils.forEveryPlayer { player ->
            if (player.belongsToTeam(team)) {
                player.flags.set(PlayerFlag.Won, true)
                player.grantAdvancement(UHCAdvancements.WINNER)
            }
            player.setGlowingTag(false)
            player.sendTitle(Texts.UHC_WON.generate(team.name).withStyle(team.color))

            DataManager.database.updateStats(player)
        }

        DataManager.database.incrementTeamWin(team)
        DataManager.database.combineStats()

        this.scheduleInLoopPhaseTask(0, 4, 100, MinecraftTimeUnit.Ticks) {
            PlayerUtils.forEveryPlayer { player ->
                player.sendSound(SoundEvents.FIREWORK_ROCKET_BLAST, volume = 0.5F)
                player.sendSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, volume = 0.5F)
                player.sendSound(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, volume = 0.5F)
            }
            this.schedulePhaseTask(6, MinecraftTimeUnit.Ticks) {
                PlayerUtils.forEveryPlayer { player ->
                    player.sendSound(SoundEvents.FIREWORK_ROCKET_SHOOT, volume = 0.5F)
                    player.sendSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, volume = 0.5F)
                    player.sendSound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, volume = 0.5F)
                }
            }
        }

        this.schedulePhaseTask(20, MinecraftTimeUnit.Seconds) {
            PlayerUtils.forEveryPlayer {
                it.clearPlayerInventory()
                PlayerRecorders.get(it)?.stop()
            }
            this.onLobby()
        }

        this.grantFinalAdvancements()
    }

    private fun createTabDisplay(): ArcadeTabDisplay {
        val display = ArcadeTabDisplay(
            ComponentSupplier.of(
                Component.literal("\n")
                    .append(Texts.ICON_UHC)
                    .append(Texts.space())
                    .append(Texts.CASUAL_UHC.gold().bold())
                    .append(Texts.space())
                    .append(Texts.ICON_UHC)
            )
        ) { _ ->
            val tps = TickUtils.calculateTPS()
            val formatting = if (tps >= 20) DARK_GREEN else if (tps > 15) YELLOW else if (tps > 10) RED else DARK_RED
            Component.literal("\n")
                .append("TPS: ")
                .append(Component.literal("%.1f".format(tps)).withStyle(formatting))
                .append("\n")
                .append(Texts.TAB_HOSTED.aqua().bold())
        }
        return display
    }

    private fun onWorldBorderComplete() {
        CasualMod.logger.info("World Border Completed")
        val end = this.uptime + MinecraftTimeUnit.Minutes.toTicks(5)

        this.schedulePhaseTask(5, MinecraftTimeUnit.Minutes, BorderFinishTask(this))
        val barTask = GlowingBossBarTask(this, end)
        this.schedulePhaseTask(5, MinecraftTimeUnit.Minutes, barTask)
        this.schedulePhaseEndTask(barTask)
    }

    private fun onServerTick() {
        this.uptime++
    }

    private fun onPause() {
        if (this.isActivePhase()) {
            this.pauseWorldBorders()
        }
    }

    private fun onUnpause() {
        if (this.isActivePhase()) {
            this.startWorldBorders()
        }
    }

    private fun onMobCategorySpawn(event: MobCategorySpawnEvent) {
        val (_, category, _, state) = event
        val i = category.maxInstancesPerChunk * state.spawnableChunkCount / 289
        if (state.mobCategoryCounts.getInt(category) >= i * PerformanceUtils.MOBCAP_MULTIPLIER) {
            event.cancel()
        }
    }

    private fun onEntityStartTracking(event: EntityStartTrackingEvent) {
        val entity = event.entity
        if (entity is Mob && PerformanceUtils.isEntityAIDisabled(entity)) {
            entity.isNoAi = true;
        }
    }

    private fun onBrewingStandBrew(event: BrewingStandBrewEvent) {
        if (!this.settings.opPotions) {
            val ingredient = event.entity.getItem(3)
            if (ingredient.`is`(Items.GLOWSTONE_DUST) || ingredient.`is`(Items.GLISTERING_MELON_SLICE)) {
                event.cancel()
            }
        }
    }

    private fun onBorderEntityPortalEntryPointEvent(event: BorderEntityPortalEntryPointEvent) {
        val (border, _, _, pos) = event

        // Blocks per millisecond
        val shrinkingSpeed = border.lerpSpeed
        if (shrinkingSpeed <= 0) {
            // Border is static or expanding
            return
        }
        val margin = shrinkingSpeed * (this.settings.portalEscapeTime * 1000)
        if (margin >= border.size * 0.5) {
            // Border would reach size 0 within 30 seconds
            event.cancel(BlockPos.containing(border.centerX, pos.y, border.centerZ))
            return
        }

        event.cancel(BlockPos.containing(
            Mth.clamp(pos.x, border.minX + margin, border.maxX - margin),
            pos.y,
            Mth.clamp(pos.z, border.minZ + margin, border.maxZ - margin)
        ))
    }

    private fun onBorderWithinBoundsEvent(event: BorderPortalWithinBoundsEvent) {
        val (border, _, pos) = event
        // Blocks per millisecond
        val shrinkingSpeed = border.lerpSpeed
        if (shrinkingSpeed <= 0) {
            // Border is static or expanding
            return
        }
        var margin = shrinkingSpeed * (this.settings.portalEscapeTime * 1000)
        margin = margin.coerceAtMost(border.size * 0.5 - 1)
        event.cancel(
            pos.x >= border.minX + margin
                && pos.x + 1 <= border.maxX - margin
                && pos.z >= border.minZ + margin
                && pos.z + 1 <= border.maxZ - margin
        )
    }

    private fun onPlayerTick(event: PlayerTickEvent) {
        val (player) = event

        if (this.isActivePhase()) {
            this.health?.setScore(player, (player.health / 2.0F).toInt())
            if (player.isSurvival) {
                this.updateWorldBorder(player)
            }
        }
    }

    private fun onPlayerDeath(event: PlayerDeathEvent) {
        event.invoke() // Post event
        val (player, source) = event

        player.flags.set(PlayerFlag.TeamGlow, false)

        // We can stop recording now...
        GlobalTickedScheduler.schedule(1, MinecraftTimeUnit.Seconds) {
            PlayerRecorders.get(player)?.stop()
        }

        player.setRespawnPosition(player.level().dimension(), player.blockPosition(), player.xRot, true, false)
        player.setGameMode(GameType.SPECTATOR)
        if (this.isActivePhase()) {
            this.onEliminated(player, source.entity)
        }
    }

    private fun onPlayerLeave(event: PlayerLeaveEvent) {
        val (player) = event
        DataManager.database.updateStats(player)

        if (!this.hasUHCStarted()) {
            PlayerRecorders.get(player)?.stop(false)
        }
    }

    private fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        val (player, _, current) = event
        if (this.isActivePhase()) {
            if (current == GameType.SPECTATOR) {
                this.health?.addPlayer(player)
            } else {
                this.health?.removePlayer(player)
            }
        }
    }

    private fun onRecipeReload(event: ServerRecipeReloadEvent) {
        event.add(GoldenHeadRecipe())
    }

    private fun onPlayerItemRelease(event: PlayerItemReleaseEvent) {
        val (player, stack) = event
        if (stack.`is`(Items.BOW)) {
            player.cooldowns.addCooldown(Items.BOW, (this.settings.bowCooldown * 20).toInt())
        }
    }

    private fun onPlayerAttack(event: PlayerAttackEvent) {
        if (this.isActivePhase() && event.target is ServerPlayer) {
            event.player.uhcStats.increment(PlayerStat.DamageDealt, event.damage.toDouble())
        }
    }

    private fun onPlayerDamage(event: PlayerDamageEvent) {
        val (player, amount) = event
        if (this.isActivePhase() && player.isSurvival) {
            player.uhcStats.increment(PlayerStat.DamageTaken, amount.toDouble())
        }
    }

    private fun onPlayerVoidDamage(event: PlayerVoidDamageEvent) {
        val (player) = event
        if (player.isSpectator) {
            event.cancel()
        }
    }

    private fun onPlayerFlag(event: PlayerFlagEvent) {
        val player = event.player
        when (event.flag) {
            PlayerFlag.TeamGlow -> {
                val team = player.team ?: return player.updateGlowingTag()
                for (member in team.getServerPlayers()) {
                    member.updateGlowingTag()
                }
            }
            PlayerFlag.FullBright -> {
                if (event.value) {
                    player.addEffect(MobEffectInstance(NIGHT_VISION, INFINITE_DURATION, 0, false, false))
                } else {
                    player.removeEffect(NIGHT_VISION)
                }
            }
            else -> { }
        }
    }

    private fun onMinigameAddPlayer(player: ServerPlayer) {
        player.updateGlowingTag()
        player.sendResourcePack(this.event.getResourcePackHandler())

        this.display.addPlayer(player)
        this.sidebar?.addPlayer(player)

        if (this.isActivePhase() && this.glowing && player.isSurvival) {
            player.setGlowingTag(true)
        }

        if (this.isActivePhase() && player.isSpectator) {
            this.health?.addPlayer(player)
        }

        val scoreboard = Arcade.server.scoreboard
        if (!this.hasUHCStarted()) {
            player.sendSystemMessage(Texts.LOBBY_WELCOME.append(" Casual UHC").gold())
            if (!player.hasPermissions(2)) {
                player.setGameMode(GameType.ADVENTURE)
                this.event.getMinigameLobby().tryTeleport(player)
            } else if (Config.dev) {
                player.sendSystemMessage(Component.literal("UHC is in dev mode!").red())
            }
        } else if (player.team == null || !player.flags.has(PlayerFlag.Participating)) {
            player.setGameMode(GameType.SPECTATOR)
        }

        if (player.team == null) {
            val spectator = scoreboard.getPlayerTeam("Spectator")
            if (spectator != null) {
                scoreboard.addPlayerToTeam(player.scoreboardName, spectator)
            }
        }

        // Needed for updating the player's health
        GlobalTickedScheduler.schedule(1, MinecraftTimeUnit.Seconds, player::resetSentInfo)
    }

    private fun onPlayerChat(event: PlayerChatEvent) {
        val (player, message) = event
        val content = message.signedContent()
        if (this.isActivePhase() && !player.isMessageGlobal(content)) {
            player.teamMessage(message)
        } else {
            val decorated = if (content.startsWith('!')) content.substring(1) else content
            player.message(Component.literal(decorated))
        }
        event.cancel()
    }

    private fun onPlayerAdvancement(event: PlayerAdvancementEvent) {
        event.player.uhcStats.add(event.advancement)
    }

    private fun onPlayerClientboundPacket(event: PlayerClientboundPacketEvent) {
        val (player, packet) = event

        if (packet is ClientboundBundlePacket || packet is ClientboundSetEntityDataPacket) {
            @Suppress("UNCHECKED_CAST")
            val updated = this.updatePacket(player, packet as Packet<ClientGamePacketListener>)
            if (updated !== packet) {
                event.cancel(updated)
            }
        }

        if (packet is ClientboundBundlePacket) {
            for (sub in packet.subPackets()) {
                this.onPacket(player, sub)
            }
        } else {
            this.onPacket(player, packet)
        }
    }

    private fun updatePacket(player: ServerPlayer, packet: Packet<ClientGamePacketListener>): Packet<ClientGamePacketListener> {
        if (packet is ClientboundSetEntityDataPacket) {
            val glowing = player.serverLevel().getEntity(packet.id())
            if (glowing is ServerPlayer) {
                return this.handleTrackerUpdatePacketForTeamGlowing(glowing, player, packet)
            }
        }
        if (packet is ClientboundBundlePacket) {
            val updated = java.util.ArrayList<Packet<ClientGamePacketListener>>()
            for (sub in packet.subPackets()) {
                updated.add(this.updatePacket(player, sub))
            }
            return ClientboundBundlePacket(updated)
        }
        return packet
    }

    private fun onPacket(player: ServerPlayer, packet: Packet<*>) {
        if (packet is ClientboundAddPlayerPacket) {
            val newPlayer = player.server.playerList.getPlayer(packet.playerId)
            newPlayer?.updateGlowingTag()
        }
    }

    private fun updateWorldBorder(player: ServerPlayer) {
        val level = player.level()
        val border = level.worldBorder

        if (border.isWithinBounds(player.boundingBox)) {
            if (player.flags.has(PlayerFlag.WasInBorder)) {
                player.flags.set(PlayerFlag.WasInBorder, false)
                player.connection.send(ClientboundInitializeBorderPacket(border))
            }
            return
        }

        val vector = player.directionVectorToNearestBorder()

        val start = player.eyePosition.add(0.0, 4.0, 0.0)
        val end = start.add(vector.normalize())

        val completed = java.util.ArrayList<BlockPos>(10)
        for (i in 1..2) {
            val top = start.lerp(end, 1.5 * i)
            val bottom = top.subtract(0.0, 10.0, 0.0)
            val hit = level.clip(ClipContext(top, bottom, ClipContext.Block.VISUAL, ClipContext.Fluid.SOURCE_ONLY, player))
            completed.add(hit.blockPos)

            if (hit.type != HitResult.Type.MISS) {
                val position = hit.blockPos
                val rotation = atan2(vector.x, vector.z)

                val arrow = ArrowShape.createCentred(position.x, hit.location.y + 0.1, position.z, 1.0, rotation)

                for (point in arrow) {
                    player.sendParticles(ParticleTypes.END_ROD, point)
                }
            }
        }

        val direction = player.directionToNearestBorder()
        val fakeDirection = direction.opposite()

        val fakeCenterX = border.centerX + fakeDirection.stepX * border.size
        val fakeCenterZ = border.centerZ + fakeDirection.stepZ * border.size

        val fakeBorder = player.uhc.border
        fakeBorder.setCenter(fakeCenterX, fakeCenterZ)
        fakeBorder.size = border.size + 0.5
        player.connection.send(ClientboundInitializeBorderPacket(fakeBorder))

        if (this.uptime % 200 == 0) {
            player.sendTitle(Component.empty())
            player.sendSubtitle(Texts.UHC_OUTSIDE_BORDER.generate(Texts.direction(direction).lime()))
        }

        player.flags.set(PlayerFlag.WasInBorder, true)
    }

    private fun handleTrackerUpdatePacketForTeamGlowing(
        glowingPlayer: ServerPlayer,
        observingPlayer: ServerPlayer,
        packet: ClientboundSetEntityDataPacket
    ): ClientboundSetEntityDataPacket {
        if (!this.settings.friendlyPlayerGlow || !this.isActivePhase()) {
            return packet
        }
        if (!glowingPlayer.isSurvival || !observingPlayer.flags.has(PlayerFlag.TeamGlow)) {
            return packet
        }
        if (glowingPlayer.team !== observingPlayer.team) {
            return packet
        }

        val tracked = packet.packedItems ?: return packet
        if (tracked.none { it.id == Entity.DATA_SHARED_FLAGS_ID.id }) {
            return packet
        }

        // Make a copy of the packet, because other players are sent the same instance of
        // The packet and may not be on the same team
        val buf = FriendlyByteBuf(Unpooled.buffer())
        packet.write(buf)
        val new = ClientboundSetEntityDataPacket(buf)

        val iterator = new.packedItems.listIterator()
        while (iterator.hasNext()) {
            val value = iterator.next()
            // Need to compare ids because they're not the same instance once re-serialized
            if (value.id() == Entity.DATA_SHARED_FLAGS_ID.id) {
                @Suppress("UNCHECKED_CAST")
                val byteValue = value as SynchedEntityData.DataValue<Byte>
                var flags = byteValue.value()
                flags = (flags.toInt() or (1 shl Entity.FLAG_GLOWING)).toByte()
                iterator.set(SynchedEntityData.DataValue.create(Entity.DATA_SHARED_FLAGS_ID, flags))
            }
        }
        return new
    }

    private fun onEliminated(player: ServerPlayer, killer: Entity?) {
        if (this.isUnclaimed(RaceAdvancement.Death)) {
            player.grantAdvancement(UHCAdvancements.EARLY_EXIT)
        }

        if (killer is ServerPlayer) {
            this.onKilled(killer, player)
        }

        if (this.settings.playerDropsGapple) {
            player.drop(Items.GOLDEN_APPLE.defaultInstance, true, false)
        }

        if (this.settings.playerDropsHead) {
            val head = HeadUtils.createConsumablePlayerHead(player)
            if (killer is ServerPlayer) {
                if (!killer.inventory.add(head)) {
                    player.drop(head, true, false)
                }
            }
        }

        player.uhcStats.increment(PlayerStat.Deaths, 1.0)

        val original = player.team
        Arcade.server.scoreboard.addPlayerToTeam(player.scoreboardName, TeamManager.getSpectatorTeam())

        if (original !== null && !original.flags.has(TeamFlag.Eliminated) && !original.hasAlivePlayers()) {
            original.flags.set(TeamFlag.Eliminated, true)
            PlayerUtils.forEveryPlayer {
                it.sendSound(SoundEvents.LIGHTNING_BOLT_THUNDER, volume = 0.5F)
                it.sendSystemMessage(Texts.UHC_ELIMINATED.generate(original.name).withStyle(original.color).bold())
            }
        }

        if (TeamManager.isOneTeamRemaining()) {
            this.onEnd()
        }
    }

    private fun onKilled(player: ServerPlayer, killed: ServerPlayer) {
        player.uhcStats.increment(PlayerStat.Kills, 1.0)
        if (this.isUnclaimed(RaceAdvancement.Kill)) {
            player.grantAdvancement(UHCAdvancements.FIRST_BLOOD)
        }

        val team = killed.team
        if (team != null && this.settings.soloBuff && team.hasAlivePlayers() && player.isAliveSolo()) {
            player.addEffect(MobEffectInstance(MobEffects.REGENERATION, 60, 2))
        }
    }

    fun createActiveSidebar() {
        this.health = ArcadeNameDisplay(Texts.ICON_HEART)
        this.health?.setTitle(Texts.ICON_HEART)

        val sidebar = ArcadeSidebar(ComponentSupplier.of(Texts.CASUAL_UHC.gold().bold()))

        val buffer = "   "

        sidebar.addRow(ComponentSupplier.of(Component.empty().append(buffer).append(Texts.SIDEBAR_TEAMMATES.monospaced())))
        for (i in 0 until this.event.getTeamSize()) {
            sidebar.addRow(TeammateRow(i, buffer))
        }
        sidebar.addRow(ComponentSupplier.of(Component.empty()))
        sidebar.addRow(BorderDistanceRow(buffer))
        sidebar.addRow { player ->
            Component.literal(buffer).append(Texts.UHC_WB_RADIUS.generate((player.level().worldBorder.size / 2.0).toInt())).monospaced()
        }
        sidebar.addRow(ComponentSupplier.of(Component.empty()))

        PlayerUtils.forEveryPlayer {
            sidebar.addPlayer(it)
        }
        this.sidebar = sidebar
    }

    private fun initialiseBorderTracker() {
        this.tracker.addListener(this)
        for (level in this.getLevels()) {
            this.tracker.addLevelBorder(level)
        }

        BorderUtils.isolateWorldBorders()
    }

    private fun moveWorldBorder(border: TrackedBorder, level: Level, stage: UHCBorderStage, size: UHCBorderSize, instant: Boolean = false) {
        if (level.dimension() == Level.END && stage >= UHCBorderStage.SIX) {
            return
        }
        val dest = if (size == UHCBorderSize.END) stage.getEndSizeFor(level) else stage.getStartSizeFor(level)
        val time = if (instant) -1.0 else stage.getRemainingTimeAsPercent(border.size, level)
        moveWorldBorder(border, dest, time)
    }

    private fun moveWorldBorder(border: TrackedBorder, newSize: Double, percent: Double = -1.0) {
        val seconds = (percent * this.settings.worldBorderTime).toLong()
        if (seconds > 0) {
            border.lerpSizeBetween(border.size, newSize, seconds * 1000L)
            return
        }
        border.size = newSize
    }

    private fun registerUHCAdvancementEvents() {
        this.registerMinigameEvent<PlayerJoinEvent>(2000) { event ->
            if (this.isActivePhase() && event.player.isSurvival) {
                val stats = event.player.uhcStats
                stats.increment(PlayerStat.Relogs, 1.0)

                // Wait for player to load in
                GlobalTickedScheduler.schedule(5, MinecraftTimeUnit.Seconds) {
                    event.player.grantAdvancement(UHCAdvancements.COMBAT_LOGGER)
                    if (stats[PlayerStat.Relogs] == 10.0) {
                        event.player.grantAdvancement(UHCAdvancements.OK_WE_BELIEVE_YOU_NOW)
                    }
                }

                val team = event.player.team
                if (team !== null && team.flags.has(TeamFlag.Eliminated)) {
                    team.flags.set(TeamFlag.Eliminated, false)
                    event.player.grantAdvancement(UHCAdvancements.TEAM_PLAYER)
                }
            }
        }
        this.registerMinigameEvent<PlayerDeathEvent> { event ->
            if (event.player.containerMenu is MinesweeperScreen) {
                event.player.grantAdvancement(UHCAdvancements.DISTRACTED)
            }
        }
        this.registerMinigameEvent<PlayerBlockPlacedEvent> { event ->
            val state = event.state
            val block = state.block
            val context = event.context
            val pos = context.clickedPos
            val world = context.level
            if (block is FallingBlock && FallingBlock.isFree(world.getBlockState(pos.below())) || pos.y < world.minBuildHeight) {
                event.player.grantAdvancement(UHCAdvancements.FALLING_BLOCK)
            } else if (block === Blocks.REDSTONE_WIRE) {
                event.player.grantAdvancement(UHCAdvancements.NOT_DUSTLESS)
            } else if (block === Blocks.TNT) {
                event.player.grantAdvancement(UHCAdvancements.DEMOLITION_EXPERT)
            }
        }
        this.registerMinigameEvent<PlayerCraftEvent> { event ->
            if (event.stack.`is`(Items.CRAFTING_TABLE) && this.isUnclaimed(RaceAdvancement.Craft)) {
                event.player.grantAdvancement(UHCAdvancements.WORLD_RECORD_PACE)
            }
        }
        this.registerMinigameEvent<PlayerBorderDamageEvent> { event ->
            if (event.invoke() && event.player.isDeadOrDying) {
                event.player.grantAdvancement(UHCAdvancements.SKILL_ISSUE)
            }
        }
        this.registerMinigameEvent<PlayerLootEvent> { event ->
            if (event.items.any { it.`is`(Items.ENCHANTED_GOLDEN_APPLE) }) {
                event.player.grantAdvancement(UHCAdvancements.DREAM_LUCK)
            }
        }
        this.registerMinigameEvent<PlayerTickEvent> { event ->
            val player = event.player
            val extension = event.player.uhc
            if (player.isSurvival && player.flags.has(PlayerFlag.Participating) && player.health <= 1.0) {
                if (++extension.halfHealthTicks == 1200) {
                    player.grantAdvancement(UHCAdvancements.ON_THE_EDGE)
                }
            } else {
                extension.halfHealthTicks = 0
            }
        }
        this.registerMinigameEvent<PlayerLandEvent> { event ->
            if (this.isActivePhase() && this.uptime < 1200 && event.damage > 0) {
                event.player.grantAdvancement(UHCAdvancements.BROKEN_ANKLES)
            }
        }
        this.registerMinigameEvent<PlayerFallEvent> { event ->
            if (this.isLobbyPhase() && !event.player.hasPermissions(4)) {
                if (this.event.getMinigameLobby().tryTeleport(event.player)) {
                    event.player.grantAdvancement(UHCAdvancements.UH_OH)
                }
            }
        }
        this.registerMinigameEvent<PlayerChatEvent> { event ->
            val message: String = event.message.signedContent().lowercase()
            if (this.isActivePhase() && event.player.isMessageGlobal(message)) {
                if (message.contains("jndi") && message.contains("ldap")) {
                    event.player.grantAdvancement(UHCAdvancements.LDAP)
                }
                if (message.contains("basically")) {
                    event.player.grantAdvancement(UHCAdvancements.BASICALLY)
                }
            }
        }
        this.registerMinigameEvent<PlayerBlockCollisionEvent> { event ->
            if (event.state.`is`(Blocks.SWEET_BERRY_BUSH)) {
                event.player.grantAdvancement(UHCAdvancements.EMBARRASSING)
            }
        }
        this.registerEvent<PlayerAdvancementEvent> { event ->
            event.announce = UHCAdvancements.isRegistered(event.advancement) && event.announce
        }
    }

    private fun grantFinalAdvancements() {
        var lowest: PlayerAttacker? = null
        var highest: PlayerAttacker? = null
        for (player in PlayerUtils.players()) {
            if (player.flags.has(PlayerFlag.Participating)) {
                val current = player.uhcStats[PlayerStat.DamageDealt]
                if (lowest === null) {
                    val first = PlayerAttacker(player, current)
                    lowest = first
                    highest = first
                }
                if (lowest.damage > current) {
                    lowest = PlayerAttacker(player, current)
                } else if (highest!!.damage < current) {
                    highest = PlayerAttacker(player, current)
                }
            }
        }
        if (lowest != null) {
            lowest.player.grantAdvancement(UHCAdvancements.MOSTLY_HARMLESS)
            highest!!.player.grantAdvancement(UHCAdvancements.HEAVY_HITTER)
        }
    }

    private class PlayerAttacker(
        val player: ServerPlayer,
        val damage: Double
    )

    inner class Settings {
        var worldBorderTime by registerSetting(
            DisplayableGameSettingBuilder.long()
                .name("border_completion_time")
                .display(Items.DIAMOND_BOOTS.literalNamed("Border Completion Time"))
                .option("ten_minutes", Items.CAKE.literalNamed("10 Minutes"), MINUTES.toSeconds(10))
                .option("two_hours", Items.GREEN_STAINED_GLASS_PANE.literalNamed("2 Hours"), MINUTES.toSeconds(120))
                .option("two_and_half_hours", Items.YELLOW_STAINED_GLASS_PANE.literalNamed("2.5 Hours"), MINUTES.toSeconds(150))
                .option("three_hours", Items.RED_STAINED_GLASS_PANE.literalNamed("3 Hours"), MINUTES.toSeconds(180))
                .value(MINUTES.toSeconds(150))
                .build()
        )
        var portalEscapeTime by registerSetting(
            DisplayableGameSettingBuilder.long()
                .name("portal_escape_time")
                .display(Items.OBSIDIAN.literalNamed("Portal Escape Time"))
                .option("none", Items.CLOCK.literalNamed("None"), 0)
                .option("ten_seconds", Items.CLOCK.literalNamed("10 Seconds"), 10)
                .option("twenty_seconds", Items.CLOCK.literalNamed("20 Second"), 20)
                .option("thirty_seconds", Items.CLOCK.literalNamed("30 Seconds"), 30)
                .option("sixty_seconds", Items.CLOCK.literalNamed("60 Seconds"), 60)
                .value(30)
                .build()
        )
        var bowCooldown by registerSetting(
            DisplayableGameSettingBuilder.double()
                .name("bow_cooldown")
                .display(Items.BOW.literalNamed("Bow Cooldown"))
                .option("none", Items.CLOCK.literalNamed("None"), 0.0)
                .option("half_second", Items.CLOCK.literalNamed("0.5 Seconds"), 0.5)
                .option("one_second", Items.CLOCK.literalNamed("1 Second"), 1.0)
                .option("two_seconds", Items.CLOCK.literalNamed("2 Seconds"), 2.0)
                .option("three_seconds", Items.CLOCK.literalNamed("3 Seconds"), 3.0)
                .option("five_seconds", Items.CLOCK.literalNamed("5 Seconds"), 5.0)
                .value(1.0)
                .build()
        )
        var health by registerSetting(
            DisplayableGameSettingBuilder.double()
                .name("health")
                .display(Items.POTION.literalNamed("Health").potion(Potions.HEALING))
                .option("triple", Items.GREEN_STAINED_GLASS_PANE.literalNamed("Triple"), 2.0)
                .option("double", Items.YELLOW_STAINED_GLASS_PANE.literalNamed("Double"), 1.0)
                .option("normal", Items.RED_STAINED_GLASS_PANE.literalNamed("Normal"), 0.0)
                .value(1.0)
                .build()
        )
        var endGameGlow by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("end_game_glow")
                .display(Items.SPECTRAL_ARROW.literalNamed("End Game Glow"))
                .defaultOptions()
                .value(true)
                .build()
        )
        var friendlyPlayerGlow by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("friendly_player_glow")
                .display(Items.GOLDEN_CARROT.literalNamed("Friendly Player Glow"))
                .defaultOptions()
                .value(true)
                .build()
        )
        var playerDropsGapple by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("player_drops_gapple")
                .display(Items.GOLDEN_APPLE.literalNamed("Player Drops Gapple"))
                .defaultOptions()
                .value(false)
                .build()
        )
        var playerDropsHead by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("player_drops_head")
                .display(Items.PLAYER_HEAD.literalNamed("Player Drops Head"))
                .defaultOptions()
                .value(true)
                .build()
        )
        var opPotions by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("op_potions")
                .display(Items.SPLASH_POTION.literalNamed("OP Potions").potion(Potions.STRONG_HARMING))
                .defaultOptions()
                .value(false)
                .build()
        )
        var generatePortals by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("generate_portals")
                .display(Items.CRYING_OBSIDIAN.literalNamed("Generate Portals"))
                .defaultOptions()
                .value(true)
                .build()
        )
        var announceMinesweeper by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("announce_minesweeper")
                .display(MinesweeperItem.STATES.createStack(MinesweeperItem.MINE).literalNamed("Announce Minesweeper"))
                .defaultOptions()
                .value(true)
                .build()
        )
        var soloBuff by registerSetting(
            DisplayableGameSettingBuilder.boolean()
                .name("solo_buff")
                .display(Items.LINGERING_POTION.literalNamed("Solo Buff").potion(Potions.REGENERATION))
                .defaultOptions()
                .value(true)
                .build()
        )
        var borderStage by registerSetting(
            DisplayableGameSettingBuilder.enum<UHCBorderStage>()
                .name("border_stage")
                .display(Items.BARRIER.literalNamed("World Border Stage"))
                .defaultOptions(UHCBorderStage::class.java)
                .value(UHCBorderStage.FIRST)
                .listener { _, value ->
                    if (isActivePhase()) {
                        moveWorldBorders(value, UHCBorderSize.START, true)
                        schedulePhaseTask(0, MinecraftTimeUnit.Ticks) {
                            startWorldBorders()
                        }
                    }
                }
                .build()
        )
    }
}