package net.casual.championships.minigame.lobby

import com.google.common.collect.ImmutableList
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.casual.arcade.commands.*
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.events.server.player.PlayerFallEvent
import net.casual.arcade.events.server.player.PlayerTeamJoinEvent
import net.casual.arcade.events.server.player.PlayerTickEvent
import net.casual.arcade.events.server.player.PlayerTryAttackEvent
import net.casual.arcade.minigame.annotation.Listener
import net.casual.arcade.minigame.area.PlaceableArea
import net.casual.arcade.minigame.chat.ChatFormatter
import net.casual.arcade.minigame.events.*
import net.casual.arcade.minigame.lobby.LobbyMinigame
import net.casual.arcade.minigame.lobby.LobbyPhase
import net.casual.arcade.minigame.ready.ReadyChecker
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.minigame.settings.MinigameSettings
import net.casual.arcade.minigame.stats.Stat.Companion.increment
import net.casual.arcade.minigame.stats.StatType
import net.casual.arcade.minigame.template.location.LocationTemplate
import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.casual.arcade.minigame.utils.MinigameUtils.isMinigameAdminOrHasPermission
import net.casual.arcade.resources.utils.ResourcePackUtils.afterPacksLoad
import net.casual.arcade.scheduler.MinecraftScheduler
import net.casual.arcade.utils.ComponentUtils.bold
import net.casual.arcade.utils.ComponentUtils.command
import net.casual.arcade.utils.ComponentUtils.gold
import net.casual.arcade.utils.ComponentUtils.green
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.red
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.ComponentUtils.yellow
import net.casual.arcade.utils.PlayerUtils.grantAdvancement
import net.casual.arcade.utils.PlayerUtils.location
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.PlayerUtils.setTitleAnimation
import net.casual.arcade.utils.PlayerUtils.teleportTo
import net.casual.arcade.utils.TeamUtils.getOnlineCount
import net.casual.arcade.utils.TimeUtils.Minutes
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.impl.Location
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.casual.arcade.visuals.elements.ComponentElements
import net.casual.arcade.visuals.elements.PlayerSpecificElement
import net.casual.arcade.visuals.elements.UniversalElement
import net.casual.arcade.visuals.firework.VirtualFirework
import net.casual.arcade.visuals.sidebar.DynamicSidebar
import net.casual.arcade.visuals.sidebar.Sidebar
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.casual.arcade.visuals.sidebar.SidebarComponents
import net.casual.arcade.visuals.tab.PlayerListDisplay
import net.casual.championships.CasualMod
import net.casual.championships.commands.MinesweeperCommand
import net.casual.championships.common.event.MinesweeperWonEvent
import net.casual.championships.common.minigame.CasualSettings
import net.casual.championships.common.minigame.rules.RulesProvider
import net.casual.championships.common.ui.bossbar.LobbyBossbar
import net.casual.championships.common.ui.elements.TeammatesSidebarElements
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonSounds
import net.casual.championships.common.util.CommonUI
import net.casual.championships.common.util.CommonUI.broadcastGame
import net.casual.championships.common.util.CommonUI.broadcastWithSound
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.duel.DuelMinigameFactory
import net.casual.championships.duel.DuelRequester
import net.casual.championships.duel.DuelSettings
import net.casual.championships.duel.arena.DuelArenasTemplate
import net.casual.championships.duel.ui.DuelConfigurationGui
import net.casual.championships.minigame.CasualMinigames
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.component.FireworkExplosion.Shape
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Team
import java.text.DecimalFormat
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class CasualLobbyMinigame(
    server: MinecraftServer,
    uuid: UUID,
    area: PlaceableArea,
    spawn: Location,
    private val podiumTemplate: LocationTemplate,
    private val podiumViewTemplate: LocationTemplate,
    private val fireworksLocations: List<LocationTemplate>,
    private val fireworkColors: List<Int>,
    private val duelArenaTemplates: List<DuelArenasTemplate>,
    private val factory: CasualLobbyMinigameFactory
): LobbyMinigame(server, uuid, area, spawn) {
    override val settings: MinigameSettings = CasualSettings(this)

    private val duels = ArrayList<DuelMinigame>()
    private val hasSeenFireworks = HashSet<UUID>()
    private var shouldWelcomePlayers = true

    private var minesweeperRecord = 127 * 1_000

    override val id: ResourceLocation = ID

    fun isDueling(player: ServerPlayer): Boolean {
        for (duel in this.duels) {
            if (duel.players.has(player)) {
                return true
            }
        }
        return false
    }

    fun getAllTeams(): Collection<PlayerTeam> {
        val teams = HashSet<PlayerTeam>()
        for (duel in this.duels) {
            for (team in duel.teams.getAllNonSpectatorOrAdminTeams()) {
                if (!this.teams.isTeamIgnored(team)) {
                    teams.add(team)
                }
            }
        }
        teams.addAll(this.teams.getAllNonSpectatorOrAdminTeams())
        return teams
    }

    override fun factory(): MinigameFactory {
        return this.factory
    }

    @Listener(priority = 1100)
    private fun onInitialize(event: MinigameInitializeEvent) {
        this.registerCommands()

        val display = PlayerListDisplay(CasualLobbyPlayerListEntries(this))
        CommonUI.addCasualFooterAndHeader(this, display)
        this.ui.setPlayerListDisplay(display)

        this.advancements.addAll(LobbyAdvancements)

        this.setBossbar(LobbyBossbar())

        this.ui.setSidebar(this.createSidebar())

        this.settings.canAttackEntities.set(true)
    }

    @Listener
    private fun onMinigameAddNewPlayer(event: MinigameAddNewPlayerEvent) {
        this.teleportToSpawn(event.player)
    }

    @Listener
    private fun onMinigameAddPlayer(event: MinigameAddPlayerEvent) {
        val player = event.player
        if (this.shouldWelcomePlayers) {
            player.setTitleAnimation(stay = 5.Seconds)
            player.sendTitle(
                CommonComponents.Text.WELCOME_TO_CASUAL_CHAMPIONSHIPS.copy().shadowless()
            )
        }

        if (!this.players.isAdmin(player)) {
            player.setGameMode(GameType.ADVENTURE)
        }

        player.grantAdvancement(LobbyAdvancements.ROOT)

        val team = player.team
        event.spectating = team == null || this.teams.isTeamIgnored(team)
        if (team != null) {
            team.collisionRule = Team.CollisionRule.NEVER
        }

        if (!this.hasSeenFireworks.contains(player.uuid) && CasualMinigames.hasWinner()) {
            player.afterPacksLoad {
                this.hasSeenFireworks.add(player.uuid)
                player.sendSound(CommonSounds.GAME_WON)
                this.scheduler.schedule(10.Seconds) {
                    this.spawnFireworkDisplays(player, this.scheduler)
                }
            }
        }
    }

    @Listener
    private fun onMinigameAddAdmin(event: MinigameAddAdminEvent) {
        val player = event.player
        if (CasualMod.config.dev) {
            player.sendSystemMessage(Component.literal("Minigames are in dev mode!").red())
        } else {
            player.sendSystemMessage(Component.literal("Minigames are NOT in dev mode!").red())
        }
    }

    @Listener
    private fun onPlayerTeamJoin(event: PlayerTeamJoinEvent) {
        val (player, team) = event
        if (!this.teams.isTeamIgnored(team)) {
            this.players.setPlaying(player)
        } else {
            this.players.setSpectating(player)
        }
    }

    @Listener
    private fun onMoveToNextMinigame(event: LobbyMoveToNextMinigameEvent) {
        event.delay = 3.Seconds
        this.shouldWelcomePlayers = false
        this.hasSeenFireworks.clear()
    }

    @Listener
    private fun onServerTick(event: ServerTickEvent) {
        if (this.bossbar.getRemainingDuration() == 25.Seconds) {
            for (player in this.players) {
                player.sendSound(CommonSounds.WAITING, SoundSource.MASTER)
            }
        }

        if (this.players.onlinePlayerCount > 0) {
            if (this.uptime.Ticks > 60.Seconds && this.uptime % 30.Seconds.ticks == 0) {
                CasualMinigames.reloadTeams(event.server)
            }
        }
    }

    @Listener
    private fun onPlayerTick(event: PlayerTickEvent) {
        val player = event.player
        val stat = this.stats.getOrCreateStat(player, MINESWEEPER_RECORD_STAT)
        val ticks = this.stats.getOrCreateStat(player, MINESWEEPER_RECORD_TICKS_STAT)
        if (stat.value == this.minesweeperRecord) {
            ticks.increment()
            if (ticks.value.Ticks >= 10.Minutes) {
                player.grantAdvancement(LobbyAdvancements.GAMER)
            }
        } else {
            ticks.modify { 0 }
        }
    }

    @Listener
    private fun onPlayerAttack(event: PlayerTryAttackEvent) {
        val (player, target) = event
        if (target is ServerPlayer && this.players.isAdmin(target)) {
            val stat = this.stats.getOrCreateStat(player, ATTACK_ADMIN_STAT)
            stat.increment()
            if (stat.value >= 50) {
                player.grantAdvancement(LobbyAdvancements.ADMIN_ABUSE)
            }
        }
        event.cancel()
    }

    @Listener
    private fun onPhaseSet(event: MinigameSetPhaseEvent) {
        if (event.phase >= LobbyPhase.Readying) {
            for (duel in ImmutableList.copyOf(this.duels)) {
                duel.close()
            }
        }
    }

    @Listener
    private fun onPlayerFall(event: PlayerFallEvent) {
        if (!AABB.of(this.area.getBoundingBox()).intersects(event.player.boundingBox)) {
            event.player.grantAdvancement(LobbyAdvancements.UH_OH)
            val stat = this.stats.getOrCreateStat(event.player, LEFT_LOBBY_STAT)
            stat.increment()
            if (stat.value >= 20) {
                event.player.grantAdvancement(LobbyAdvancements.YOU_SHALL_NOT_LEAVE)
            }
        }
    }

    @Listener
    private fun onMinesweeperWon(event: MinesweeperWonEvent) {
        val (player, duration) = event
        if (duration < 40.seconds) {
            event.player.grantAdvancement(LobbyAdvancements.OFFICIALLY_BORED)
        }

        val millis = duration.inWholeMilliseconds.toInt()
        val stat = this.stats.getOrCreateStat(player, MINESWEEPER_RECORD_STAT)
        if (millis < stat.value) {
            stat.modify { millis }
        }

        val formatted = FORMAT.format(duration.toDouble(DurationUnit.SECONDS))
        player.sendSystemMessage(CommonComponents.MINESWEEPER_WON.generate(formatted))
        if (millis < this.minesweeperRecord) {
            this.minesweeperRecord = millis
            val message = CommonComponents.MINESWEEPER_RECORD.generate(player.scoreboardName, formatted)
            this.chat.broadcast(message)
        }
    }

    override fun startNextMinigame() {
        val minigame = this.next
        if (minigame !is RulesProvider) {
            super.startNextMinigame()
            return
        }

        this.settings.isChatMuted.set(true)
        val rules = minigame.getRules()
        var delay = MinecraftTimeDuration.ZERO
        for (rule in rules) {
            for (entry in rule.entries) {
                val formatter = ChatFormatter.createAnnouncement(rule.title)
                this.scheduler.schedulePhased(delay) {
                    val message = entry.lines.fold(Component.empty()) { a, b -> a.append("\n\n").append(b) }
                    this.chat.broadcastWithSound(message, formatter = formatter)
                }
                delay += entry.duration
            }
        }
        this.scheduler.schedulePhased(delay) {
            super.startNextMinigame()
        }
        this.scheduler.schedulePhasedCancellable(delay) {
            this.settings.isChatMuted.set(false)
        }.runIfCancelled()
    }

    override fun teleportToSpawn(player: ServerPlayer) {
        val location = if (CasualMinigames.isWinner(player)) {
            this.podiumTemplate.get(this.area.level)
        } else if (CasualMinigames.hasWinner()) {
            this.podiumViewTemplate.get(this.area.level)
        } else {
            this.spawn
        }
        player.teleportTo(location)
    }

    private fun registerCommands() {
        this.commands.register(CommandTree.buildLiteral("minesweeper") {
            executes(::minesweeper)
        })
        this.commands.register(CommandTree.buildLiteral("duel") {
            executes(::startDuel)
            literal("start") {
                executes(::startDuel)
            }
            literal("view") {
                argument("player", EntityArgument.player()) {
                    suggests { _, builder ->
                        val players = duels.flatMap { it.players.playing }.map(ServerPlayer::getScoreboardName)
                        SharedSuggestionProvider.suggest(players, builder)
                    }
                    executes(::viewDueler)
                }
            }
        })
    }

    private fun minesweeper(context: CommandContext<CommandSourceStack>): Int {
        if (this.phase < LobbyPhase.Readying) {
            return MinesweeperCommand.execute(context)
        }
        context.source.playerOrException.grantAdvancement(LobbyAdvancements.NOT_NOW)
        return context.source.fail(Component.translatable("casual.duel.cannotMinesweeperNow"))
    }

    private fun startDuel(context: CommandContext<CommandSourceStack>): Int {
        val player = context.source.playerOrException
        if (this.phase >= LobbyPhase.Readying) {
            player.grantAdvancement(LobbyAdvancements.NOT_NOW)
            return context.source.fail(Component.translatable("casual.duel.cannotDuelNow"))
        }
        val settings = DuelSettings(this.duelArenaTemplates)
        val gui = DuelConfigurationGui(player, settings, this.players::all, this::requestDuelWith)
        gui.open()
        return Command.SINGLE_SUCCESS
    }

    private fun viewDueler(context: CommandContext<CommandSourceStack>): Int {
        val player = context.source.playerOrException

        val dueler = EntityArgument.getPlayer(context, "player")
        val minigame = dueler.getMinigame()
        if (minigame !is DuelMinigame) {
            return context.source.fail(Component.translatable("casual.duel.playerNotDueling"))
        }

        if (!this.duels.contains(minigame)) {
            return context.source.fail("This shouldn't happen, please tell sensei!")
        }

        minigame.players.add(player, true, this.players.isAdmin(player))
        player.teleportTo(dueler.location)
        return context.source.success(Component.translatable("casual.duel.teleportingToDuel"))
    }

    private fun requestDuelWith(
        initiator: ServerPlayer,
        players: Collection<ServerPlayer>,
        settings: DuelSettings
    ) {
        var started = false

        val duelers = HashSet(players)
        duelers.removeIf { !this.players.has(it) }
        duelers.add(initiator)

        val requesting = duelers.filter { it !== initiator }

        val requester = DuelRequester(initiator, duelers)
        if (requesting.isEmpty() && !initiator.isMinigameAdminOrHasPermission(4)) {
            requester.broadcastTo(Component.translatable("casual.duel.notEnoughPlayers").mini().red(), initiator)
            return
        }

        val checker = ReadyChecker(requester)
        checker.arePlayersReady(requesting).then {
            started = startDuelWith(started, initiator, duelers, setOf(), requester, settings, false)
        }
        val startAnyways = Component.translatable("casual.duel.clickToStart").mini().green().function { context ->
            val unready = checker.getUnreadyPlayers(context.server)
            started = startDuelWith(started, initiator, duelers, unready, requester, settings, true)
        }
        requester.broadcastTo(startAnyways, initiator)
    }

    private fun startDuelWith(
        started: Boolean,
        initiator: ServerPlayer,
        duelers: HashSet<ServerPlayer>,
        unready: Collection<ServerPlayer>,
        requester: DuelRequester,
        settings: DuelSettings,
        forced: Boolean
    ): Boolean {
        if (started) {
            if (forced) {
                requester.broadcastTo(Component.translatable("casual.duel.alreadyStarted").mini().red(), initiator)
            }
            return true
        }
        if (!this.players.has(initiator) || this.phase >= LobbyPhase.Readying) {
            requester.broadcastTo(Component.translatable("casual.duel.cannotDuelNow").mini().red(), initiator)
            initiator.grantAdvancement(LobbyAdvancements.NOT_NOW)
            return false
        }

        val ready = HashSet(duelers)
        if (!this.players.isAdmin(initiator)) {
            ready.removeAll(unready.toSet())
        }
        ready.removeIf { !this.players.has(it) }

        if (ready.size <= 1 && !initiator.isMinigameAdminOrHasPermission(4)) {
            requester.broadcastTo(Component.translatable("casual.duel.notEnoughPlayers").mini().red(), initiator)
            return false
        }

        val duel = DuelMinigameFactory(settings).create(MinigameCreationContext(initiator.server))
        this.duels.add(duel)
        duel.events.register<MinigameCloseEvent> { this.duels.remove(duel) }

        duel.commands.register(CommandTree.buildLiteral("duel") {
            literal("leave") {
                executes { context ->
                    val player = context.source.playerOrException
                    duel.players.transferTo(this@CasualLobbyMinigame, player, keepSpectating = false)
                    context.source.success("Returning to Lobby...")
                }
            }
        })

        this.players.transferTo(duel, ready, keepSpectating = false)
        duel.chat.broadcastGame(Component.translatable("casual.duel.starting").mini().green())
        duel.start()

        val players = if (ready.size > 4) {
            ready.take(4).joinToString(" & ") { it.scoreboardName }
        } else {
            ready.joinToString(" & ") { it.scoreboardName }
        }
        val aboutToDuel = Component.translatable("casual.duel.aboutToDuel", players).mini()
        for (player in this.players) {
            if (ready.contains(player)) {
                continue
            }
            requester.broadcastTo(aboutToDuel, player)

            val clickToSpectate = Component.empty().append("[")
                .append(Component.translatable("casual.duel.clickToSpectate"))
                .append("]").command("/duel view ${ready.first().scoreboardName}").lime().mini()
            requester.broadcastTo(clickToSpectate, player)
        }

        return true
    }

    private fun spawnFireworkDisplays(player: ServerPlayer, scheduler: MinecraftScheduler) {
        scheduler.scheduleInLoop(MinecraftTimeDuration.ZERO, 10.Ticks, 10.Seconds) {
            if (player.level() === this.area.level) {
                this.spawnFireworkDisplay(player)
            }
        }
    }

    private fun spawnFireworkDisplay(player: ServerPlayer) {
        for (template in this.fireworksLocations) {
            val firingLocation = template.get(this.area.level)
            val firework = VirtualFirework.build {
                location = firingLocation
                duration = Random.nextInt(20, 30).Ticks

                SHAPES.asSequence().shuffled().take(Random.nextInt(1, 4)).forEach { shape ->
                    val index = Random.nextInt(fireworkColors.size)
                    explosion {
                        shape(shape)
                        addPrimaryColours(fireworkColors[index])
                        addFadeColours(fireworkColors[index])
                        trail()
                        twinkle()
                    }
                }
            }
            firework.sendTo(player)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun createSidebar(): Sidebar {
        val name = CasualMinigames.getMinigames().event.name.replace('_', ' ')
        val title = Component.literal("Casual Championships").mini().bold()
        val sidebar = DynamicSidebar(ComponentElements.of(title))
        val event = SidebarComponent.withCustomScore(
            Component.literal(" Event:").mini().red().bold(),
            Component.literal("$name ").mini().gold()
        )

        val teammates = TeammatesSidebarElements(Component.literal(" "), Component.literal(" "), false)
        val players = UniversalElement.cached { server ->
            val playing = server.scoreboard.playerTeams.filter {
                !this.teams.isTeamIgnored(it)
            }
            val online = playing.sumOf { it.getOnlineCount() }
            val expected = playing.sumOf { it.players.size }
            SidebarComponent.withCustomScore(
                Component.literal(" Players: ").mini().lime().bold(),
                Component.literal("$online/$expected ").mini().yellow()
            )
        }
        sidebar.setRows(PlayerSpecificElement.composed(players) { player ->
            val components = SidebarComponents.of(SidebarComponent.EMPTY)
            components.addRow(event)
            components.addRow(SidebarComponent.EMPTY)

            val team = player.team
            if (team != null && !this.teams.isTeamIgnored(team)) {
                teammates.addTeammates(player, components)
                components.addRow(SidebarComponent.EMPTY)
            }

            components.addRow(players.get(player))
                .addRow(SidebarComponent.EMPTY)
        })
        return sidebar
    }

    companion object {
        private val SHAPES = listOf(Shape.LARGE_BALL, Shape.STAR, Shape.BURST)

        private val MINESWEEPER_RECORD_STAT = StatType.int32(CasualMod.id("minesweeper_record"), Int.MAX_VALUE)
        private val MINESWEEPER_RECORD_TICKS_STAT = StatType.int32(CasualMod.id("minesweeper_record_ticks"))
        private val LEFT_LOBBY_STAT = StatType.int32(CasualMod.id("left_lobby"))
        private val ATTACK_ADMIN_STAT = StatType.int32(CasualMod.id("attack_admin"))

        private val FORMAT = DecimalFormat("#.00")

        val ID = CasualMod.id("lobby")
    }
}