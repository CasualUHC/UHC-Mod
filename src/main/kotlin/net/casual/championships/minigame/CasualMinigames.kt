package net.casual.championships.minigame

import com.mojang.serialization.JsonOps
import net.casual.arcade.commands.register
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.player.PlayerJoinEvent
import net.casual.arcade.events.player.PlayerRequestLoginEvent
import net.casual.arcade.events.player.PlayerTeamJoinEvent
import net.casual.arcade.events.server.ServerLoadedEvent
import net.casual.arcade.events.server.ServerRegisterCommandEvent
import net.casual.arcade.events.server.ServerSaveEvent
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.chat.ChatFormatter
import net.casual.arcade.minigame.events.*
import net.casual.arcade.minigame.ready.MinigamePlayerReadyHandler
import net.casual.arcade.minigame.ready.ReadyChecker
import net.casual.arcade.minigame.template.minigame.MinigamesTemplate
import net.casual.arcade.minigame.template.minigame.SequentialMinigames
import net.casual.arcade.minigame.utils.MinigameRegistries
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.minigame.utils.MinigameUtils.broadcastChangesToAdmin
import net.casual.arcade.resources.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.ComponentUtils.bold
import net.casual.arcade.utils.ComponentUtils.color
import net.casual.arcade.utils.ComponentUtils.green
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.red
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ComponentUtils.yellow
import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.PlayerUtils.broadcastToOps
import net.casual.arcade.utils.ServerUtils.setMessageOfTheDay
import net.casual.arcade.utils.codec.CodecProvider.Companion.register
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.utils.toSmallCaps
import net.casual.championships.CasualMod
import net.casual.championships.commands.CasualCommand
import net.casual.championships.commands.MinesweeperCommand
import net.casual.championships.commands.ViewCommand
import net.casual.championships.common.ui.CasualCountdown
import net.casual.championships.common.ui.CasualTeamReadyHandler
import net.casual.championships.common.util.CommonSounds
import net.casual.championships.common.util.CommonUI
import net.casual.championships.common.util.CommonUI.broadcastWithSound
import net.casual.championships.common.util.PerformanceUtils
import net.casual.championships.data.DataManager
import net.casual.championships.data.DatabaseDataManager
import net.casual.championships.data.JsonDataManager
import net.casual.championships.data.MultiDataManager
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.duel.DuelMinigameFactory
import net.casual.championships.events.CasualConfigReloaded
import net.casual.championships.minigame.lobby.CasualLobbyMinigameFactory
import net.casual.championships.resources.CasualResourcePackHost
import net.casual.championships.uhc.UHCMinigame
import net.casual.championships.uhc.UHCMinigameFactory
import net.casual.championships.util.CasualConfig
import net.casual.championships.util.CasualTeamUtils.getOrCreateAdminTeam
import net.casual.championships.util.CasualTeamUtils.getOrCreateSpectatorTeam
import net.casual.database.CasualDatabase
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.UserWhiteListEntry
import net.minecraft.world.scores.Team
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writer

@Suppress("UnstableApiUsage")
object CasualMinigames {
    private val path: Path = CasualConfig.resolve("event")
    internal val winners = HashSet<String>()

    private var minigames: SequentialMinigames? = null

    private var dataManager: DataManager? = null

    val minigame: Minigame
        get() = this.getMinigames().getCurrent()

    @JvmField
    var floodgates = false

    fun getMinigames(): SequentialMinigames {
        return this.minigames ?: throw IllegalArgumentException("Tried to access minigames too early!")
    }

    fun getDataManager(): DataManager {
        return this.dataManager ?: throw IllegalArgumentException("Tried to access data manager too early!")
    }

    fun isWinner(player: ServerPlayer): Boolean {
        return this.winners.contains(player.scoreboardName)
    }

    fun hasWinner(): Boolean {
        return this.winners.isNotEmpty()
    }

    fun reloadResourcePacks(server: MinecraftServer): CompletableFuture<Void> {
        return CasualResourcePackHost.reload().thenAcceptAsync({
            for (player in this.minigame.players) {
                this.getMinigames().sendResourcesTo(player)
            }
        }, server)
    }

    internal fun registerEvents() {
        GlobalEventHandler.register<MinigameInitializeEvent> { event -> this.modifyMinigame(event.minigame) }

        // TODO: Move these?
        UHCMinigameFactory.register(MinigameRegistries.MINIGAME_FACTORY)
        DuelMinigameFactory.register(MinigameRegistries.MINIGAME_FACTORY)
        CasualLobbyMinigameFactory.register(MinigameRegistries.MINIGAME_FACTORY)

        GlobalEventHandler.register<ServerRegisterCommandEvent> { event ->
            event.register(MinesweeperCommand, CasualCommand, ViewCommand)
        }

        GlobalEventHandler.register<PlayerRequestLoginEvent> { event ->
            if (event.isAccepted && !floodgates && !event.server.playerList.isOp(event.profile)) {
                event.deny(Component.literal("CasualChampionships isn't quite ready yet..."))
            }
            if (!event.isAccepted) {
                event.server.playerList.players.broadcastToOps(
                    Component.literal("${event.profile.name} tried to join, but was denied because: ").append(event.reason!!)
                )
            }
        }

        // This must happen before minigames are loaded
        GlobalEventHandler.register<ServerLoadedEvent>(0) {
            val minigames = SequentialMinigames(this.readMinigameEvent(), it.server)
            this.minigames = minigames

            it.server.setMessageOfTheDay(this.getMOTD())

            this.dataManager = createDataManager(CasualMod.config)

        }
        GlobalEventHandler.register<ServerLoadedEvent>(Int.MAX_VALUE) {
            val data = this.loadMinigameEventData()
            if (data != null) {
                this.getMinigames().setData(data)
            }

            it.server.playerList.setUsingWhiteList(true)
            this.createTeams(it.server)
        }

        GlobalEventHandler.register<PlayerJoinEvent>(phase = PlayerJoinEvent.PHASE_INITIALIZED) {
            it.delayJoinMessage = true
        }
        GlobalEventHandler.register<PlayerJoinEvent> {
            val player = it.player
            this.getMinigames().addPlayer(player)
        }

        GlobalEventHandler.register<ServerSaveEvent> {
            this.writeMinigameEventData(this.getMinigames().getData())
        }

        GlobalEventHandler.register<CasualConfigReloaded> { (config) ->
            val minigames = this.getMinigames()
            minigames.event = this.readMinigameEvent()
            minigames.reloadLobby()

            this.getDataManager().close()
            this.dataManager = this.createDataManager(config)
        }

        GlobalEventHandler.register<ServerStoppingEvent> {
            this.getDataManager().close()
        }
    }

    private fun modifyMinigame(minigame: Minigame) {
        when (minigame) {
            is UHCMinigame -> this.modifyUHCMinigame(minigame)
            is DuelMinigame -> this.modifyDuelMinigame(minigame)
        }
    }

    private fun modifyUHCMinigame(minigame: UHCMinigame) {
        PerformanceUtils.reduceMinigameMobcap(minigame)
        PerformanceUtils.disableEntityAI(minigame)
        setCasualUI(minigame)

        minigame.resources.add(
            MinigameResources.of(CasualResourcePackHost.uhc.toPackInfo(!CasualMod.config.dev))
        )

        minigame.events.register<MinigameCloseEvent> {
            getMinigames().returnToLobby()
        }
        minigame.events.register<MinigameCompleteEvent> {
            this.winners.clear()
            this.winners.addAll(minigame.winners)
            getDataManager().syncUHCData(minigame)
        }

        minigame.settings.replay = !CasualMod.config.dev
    }

    private fun modifyDuelMinigame(minigame: DuelMinigame) {
        minigame.events.register<MinigameCloseEvent> {
            minigame.players.transferTo(this.minigame)
        }
        minigame.events.register<MinigameCompleteEvent> {
            this.getDataManager().syncDuelData(minigame)
        }
        this.setCasualUI(minigame)
        minigame.ui.setPlayerListDisplay(CommonUI.createSimpleTabDisplay(minigame))
    }

    internal fun setCasualUI(minigame: Minigame) {
        minigame.settings.broadcastChangesToAdmin()
        minigame.ui.setPlayerListDisplay(CommonUI.createTeamMinigameTabDisplay(minigame))
        minigame.ui.readier = ReadyChecker(
            MinigamePlayerReadyHandler(minigame),
            CasualTeamReadyHandler(minigame)
        )
        minigame.ui.countdown = CasualCountdown

        minigame.ui.addNameTag(CommonUI.createPlayingNameTag())
        minigame.events.register<MinigameAddPlayerEvent> {
            it.player.team?.nameTagVisibility = Team.Visibility.NEVER
        }
        minigame.events.register<PlayerTeamJoinEvent> {
            it.team.nameTagVisibility = Team.Visibility.NEVER
        }
        minigame.chat.systemChatFormatter = ChatFormatter {
            ChatFormatter.SYSTEM.format(Component.empty().append(it).mini())
        }

        this.setPauseNotification(minigame)
    }

    private fun setPauseNotification(minigame: Minigame) {
        minigame.events.register<MinigamePauseEvent> {
            minigame.chat.broadcastWithSound(
                Component.literal("Minigame is now paused"),
                Sound(CommonSounds.GAME_PAUSED)
            )
        }
    }

    private fun readMinigameEvent(): MinigamesTemplate {
        val path = this.path.resolve("event.json")
        if (path.exists()) {
            val json = path.reader().use {
                JsonUtils.decodeToJsonElement(it)
            }
            val result = MinigamesTemplate.CODEC.parse(JsonOps.INSTANCE, json)
            val event = result.resultOrPartial {
                CasualMod.logger.error(it)
            }
            if (event.isPresent) {
                return event.get()
            } else {
                val error = result.error()
                if (error.isPresent) {
                    CasualMod.logger.error(error.get().message())
                }
            }
        }
        this.writeMinigameEvent(MinigamesTemplate.DEFAULT)
        return MinigamesTemplate.DEFAULT
    }

    private fun writeMinigameEvent(config: MinigamesTemplate) {
        val path = this.path.resolve("event.json")
        path.parent.createDirectories()
        val json = MinigamesTemplate.CODEC.encodeStart(JsonOps.INSTANCE, config).result()
        if (json.isPresent) {
            path.writer().use {
                JsonUtils.encode(json.get(), it)
            }
        }
    }

    private fun loadMinigameEventData(): SequentialMinigames.Data? {
        val eventData = this.path.resolve("event_data.json")
        if (eventData.exists()) {
            val json = eventData.reader().use {
                JsonUtils.decodeToJsonElement(it)
            }
            val result = SequentialMinigames.Data.CODEC.parse(JsonOps.INSTANCE, json).result()
            if (result.isPresent) {
                return result.get()
            }
        }
        return null
    }

    private fun writeMinigameEventData(data: SequentialMinigames.Data) {
        val eventData = this.path.resolve("event_data.json")
        eventData.parent.createDirectories()
        val json = SequentialMinigames.Data.CODEC.encodeStart(JsonOps.INSTANCE, data).result()
        if (json.isPresent) {
            eventData.writer().use {
                JsonUtils.encode(json.get(), it)
            }
        }
    }

    private fun getMOTD(): Component {
        return Component.empty().apply {
            append(Component.literal("╔").color(0x009BFF))
            append(Component.literal("═").color(0x19A5FF))
            append(Component.literal("═").color(0x33AFFF))
            append(Component.literal("═").color(0x4DB9FF))
            append(Component.literal("═").color(0x66C3FF))
            append(Component.literal("═").color(0x80CDFF))
            append(Component.literal("\uD83D\uDDE1").yellow())
            append(" ")
            append(Component.literal("C${"asual".toSmallCaps()} C${"hampionships".toSmallCaps()}").bold().color(0xFFAC1C))
            append(" ")
            append(Component.literal("\uD83C\uDFF9").yellow())
            append(Component.literal("═").color(0x80CDFF))
            append(Component.literal("═").color(0x66C3FF))
            append(Component.literal("═").color(0x4DB9FF))
            append(Component.literal("═").color(0x33AFFF))
            append(Component.literal("═").color(0x19A5FF))
            append(Component.literal("╗").color(0x009BFF))
            append("\n")

            append(Component.literal("╚").color(0x009BFF))
            append(Component.literal("═").color(0x19A5FF))
            append(Component.literal("═").color(0x33AFFF))

            append("   ")
            append(Component.literal("be prepared".toSmallCaps()).lime())
            append(" ")
            append(Component.literal("◆").white())
            append(" ")
            append(Component.literal("let the chaos ensue".toSmallCaps()).lime())
            append("    ")

            append(Component.literal("═").color(0x33AFFF))
            append(Component.literal("═").color(0x19A5FF))
            append(Component.literal("╝").color(0x009BFF))
        }
    }

    fun createTeams(server: MinecraftServer) {
        this.getDataManager().createTeams(server).thenApplyAsync({ teams ->
            val scoreboard = server.scoreboard
            this.minigame.teams.setAdminTeam(scoreboard.getOrCreateAdminTeam())
            this.minigame.teams.setSpectatorTeam(scoreboard.getOrCreateSpectatorTeam())
            val minigames = this.getMinigames()
            for (player in server.playerList.players) {
                if (minigames.event.isAdmin(player)) {
                    this.minigame.players.addAdmin(player)
                    val team = player.team
                    if (team == null || team == this.minigame.teams.getAdminTeam()) {
                        this.minigame.players.setSpectating(player)
                    }
                }
            }
            for (player in this.minigame.players) {
                if (player.team == null) {
                    this.minigame.players.setSpectating(player)
                }
            }

            if (CasualResourcePackHost.loadTeamColors(teams)) {
                this.reloadResourcePacks(server)
            }
        }, server)

        this.reloadWhitelist(server)
    }

    fun reloadTeams(server: MinecraftServer) {
        this.getDataManager().reloadTeams(server)
        return this.reloadWhitelist(server)
    }

    private fun reloadWhitelist(server: MinecraftServer) {
        this.getDataManager().getParticipants().thenApplyAsync({ participants ->
            val whitelist = server.playerList.whiteList
            val previous = whitelist.userList.toSet()
            for (entry in whitelist.entries.toList()) {
                server.playerList.whiteList.remove(entry)
            }
            val added = HashSet<String>()
            val removed = previous - participants.mapTo(HashSet()) { it.name }
            for (participant in participants) {
                whitelist.add(UserWhiteListEntry(participant))
                if (!previous.contains(participant.name)) {
                    added.add(participant.name)
                }
            }
            if (added.isEmpty() && removed.isEmpty()) {
                return@thenApplyAsync
            }
            val component = Component.literal("Reloaded whitelist.")
            if (added.isNotEmpty()) {
                component.append(Component.literal("\nAdded: ").green()).append(added.joinToString { it })
            }
            if (removed.isNotEmpty()) {
                component.append(Component.literal("\nRemoved: ").red()).append(removed.joinToString { it })
            }
            server.playerList.players.broadcastToOps(component.mini())
        }, server)
    }

    private fun createDataManager(config: CasualConfig): DataManager {
        val login = config.database
        if (login.url.isEmpty()) {
            return JsonDataManager()
        }
        val location = if (config.dev) "casual_championships_debug" else "casual_championships"
        val database = CasualDatabase(login.url + "/$location", login.username, login.password)
        database.initialize()
        return MultiDataManager.of(
            DatabaseDataManager(this.getMinigames().event.name, database),
            JsonDataManager()
        )
    }
}