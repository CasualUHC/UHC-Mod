package net.casual.championships.common.ui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import eu.pb4.sgui.api.elements.GuiElementInterface
import eu.pb4.sgui.api.gui.SimpleGui
import it.unimi.dsi.fastutil.ints.IntArraySet
import it.unimi.dsi.fastutil.ints.IntSet
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ItemUtils.named
import net.casual.championships.common.event.MinesweeperWonEvent
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.items.MinesweeperItems
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.text.DecimalFormat
import java.util.*
import kotlin.math.floor
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import net.minecraft.world.inventory.ClickType as ClickAction

class MinesweeperGui(
    player: ServerPlayer
): SimpleGui(MenuType.GENERIC_9x6, player, true) {
    private val guessed = IntArraySet()
    private val flags = IntArraySet()
    private val grid = Grid(9, 9)
    private val flagItem = MinesweeperItems.FLAG_COUNTER.named(CommonComponents.MINESWEEPER_FLAGS)
    private val clockItem = Items.CLOCK.named(CommonComponents.MINESWEEPER_TIMER)
    private var complete = false

    init {
        this.flagItem.count = Grid.MINE_COUNT
        for (i in 0..80) {
            this.setSlot(i, UNKNOWN_TILE)
        }

        this.setSlot(81, GuiElement(EXIT_TILE) { _, _, _, gui -> gui.close() })
        this.setSlot(82, DESC_TILE_1)
        this.setSlot(83, DESC_TILE_2)
        this.setSlot(84, DESC_TILE_3)
        this.setSlot(85, DESC_TILE_4)
        this.setSlot(87, this.flagItem)
        this.setSlot(88, this.clockItem)
        this.setSlot(89, GuiElement(PLAY_AGAIN_TILE) { _, _, _, gui -> MinesweeperGui(gui.player).open() })

        this.title = Component.empty().append(SpacingFontResources.spaced(-8))
            .append(CommonComponents.Gui.MINESWEEPER_MENU.copy().white())
    }

    override fun onTick() {
        if (this.grid.startTime != 0L && !this.complete) {
            val seconds = floor((System.nanoTime() - this.grid.startTime) / 1000000000.0).toInt()
            this.clockItem.count = Mth.clamp(seconds, 1, 127)
        }
    }

    override fun onClick(
        slotId: Int,
        type: ClickType,
        action: ClickAction,
        element: GuiElementInterface?
    ): Boolean {
        if (slotId >= 0 && slotId < this.grid.capacity) {
            if (action != ClickAction.PICKUP || this.complete) {
                return false
            }
            if (type.isLeft && !this.flags.contains(slotId)) {
                leftClickTile(slotId, player)
            }
            if (type.isRight) {
                rightClickTile(slotId)
            }
        }
        return false
    }

    private fun leftClickTile(index: Int, player: ServerPlayer) {
        this.grid.checkGenerated(index)
        this.recursiveClickTile(index, player, null)
    }

    private fun recursiveClickTile(index: Int, player: ServerPlayer, checked: IntSet?) {
        val tile = this.grid.getTile(index)
        if (tile == -1) {
            onLose(player)
            return
        }
        if (tile == Int.MIN_VALUE) {
            return
        }
        this.guessed.add(index)
        val stack = getTileStack(tile)
        this.setSlot(index, stack)
        this.checkWin(player)
        if (tile == 0) {
            val set = checked ?: IntArraySet()
            set.add(index)
            for (surrounding in this.grid.getSurroundingIndices(index).iterator()) {
                if (this.flags.contains(surrounding)) {
                    this.rightClickTile(surrounding)
                }
                if (set.add(surrounding)) {
                    this.recursiveClickTile(surrounding, player, set)
                }
            }
        }
    }

    private fun rightClickTile(index: Int) {
        if (this.guessed.contains(index) && !this.flags.contains(index)) {
            return
        }
        val stack = if (this.flags.contains(index)) {
            this.flags.remove(index)
            UNKNOWN_TILE
        } else {
            this.flags.add(index)
            MinesweeperItems.FLAG.named(CommonComponents.MINESWEEPER_FLAG)
        }
        this.flagItem.count = (12 - flags.size).coerceAtLeast(1)
        this.setSlot(index, stack)
    }

    private fun checkWin(player: ServerPlayer) {
        val possibleGuesses: Int = grid.capacity - Grid.MINE_COUNT
        if (this.guessed.size == possibleGuesses) {
            this.onWin(player)
        }
    }

    private fun onWin(player: ServerPlayer) {
        this.complete = true
        val time = (System.nanoTime() - grid.startTime).nanoseconds
        GlobalEventHandler.broadcast(MinesweeperWonEvent(player, time))

        val formatted = FORMAT.format(time.toDouble(DurationUnit.SECONDS))
        player.sendSystemMessage(CommonComponents.MINESWEEPER_WON.generate(formatted))
        if (time < record) {
            record = time
            val message = CommonComponents.MINESWEEPER_RECORD.generate(player.scoreboardName, formatted)
            player.server.playerList.broadcastSystemMessage(message, false)
        }
    }

    private fun onLose(player: ServerPlayer) {
        this.complete = true
        for (i in 0 until grid.capacity) {
            this.setSlot(i, this.getTileStack(this.grid.getTile(i)))
        }
        player.sendSystemMessage(CommonComponents.MINESWEEPER_LOST)
    }

    private fun getTileStack(tile: Int): ItemStack {
        check(tile <= 8 && tile >= -1) { "Invalid tile: $tile" }
        return when (tile) {
            -1 -> MinesweeperItems.MINE.named(CommonComponents.MINESWEEPER_MINE)
            0 -> ItemStack.EMPTY
            else -> MinesweeperItems.of(tile).named(tile.toString())
        }
    }

    private class Grid(private val width: Int, height: Int) {
        val tiles: IntArray
        val capacity: Int
        var startTime: Long = 0

        init {
            this.capacity = this.width * height
            this.tiles = IntArray(this.capacity)
        }

        fun checkGenerated(first: Int) {
            if (this.startTime == 0L) {
                generate(first)
                this.startTime = System.nanoTime()
            }
        }

        fun getTile(index: Int): Int {
            return if (index < 0 || index >= this.capacity) Int.MIN_VALUE else this.tiles[index]
        }

        private fun generate(first: Int) {
            var count = MINE_COUNT
            while (count > 0) {
                val index = RANDOM.nextInt(this.capacity)
                if (index != first && !isMine(index)) {
                    setMine(index)
                    // Super jank way of checking but it works :P
                    if (countMines(first) > 0) {
                        this.tiles[index] = 0
                        continue
                    }
                    count--
                }
            }
            for (i in 0 until this.capacity) {
                this.tiles[i] = countMines(i)
            }
        }

        private fun countMines(index: Int): Int {
            // If it's a mine we ignore; we don't count the mines.
            if (isMine(index)) {
                return -1
            }
            var mines = 0
            for (surrounding in this.getSurroundingIndices(index).iterator()) {
                if (isMine(surrounding)) {
                    mines++
                }
            }
            return mines
        }

        fun getSurroundingIndices(index: Int): IntSet {
            // Eliminates the indexes we need to check.
            val isLeft = index % this.width == 0
            val isRight = (index + 1) % this.width == 0
            val isTop = index - this.width < 0
            val isBottom = index + this.width > this.capacity
            val surrounding: IntSet = IntArraySet()
            // Could probably improve this, but eh.
            if (!isLeft) {
                surrounding.add(index - 1)
                if (!isTop) {
                    surrounding.add(index - this.width - 1)
                }
                if (!isBottom) {
                    surrounding.add(index + this.width - 1)
                }
            }
            if (!isRight) {
                surrounding.add(index + 1)
                if (!isTop) {
                    surrounding.add(index - this.width + 1)
                }
                if (!isBottom) {
                    surrounding.add(index + this.width + 1)
                }
            }
            if (!isTop) {
                surrounding.add(index - this.width)
            }
            if (!isBottom) {
                surrounding.add(index + this.width)
            }
            return surrounding
        }

        private fun isMine(index: Int): Boolean {
            return index in 0 until this.capacity && this.tiles[index] == -1
        }

        private fun setMine(index: Int) {
            tiles[index] = -1
        }

        companion object {
            private val RANDOM = Random()
            const val MINE_COUNT = 12
        }
    }

    companion object {
        private val UNKNOWN_TILE = MinesweeperItems.UNKNOWN.named("?")
        private val EXIT_TILE = DisplayItems.CROSS.named(CommonComponents.EXIT)
        private val DESC_TILE_1 = Items.OAK_SIGN.named(CommonComponents.MINESWEEPER_DESC_1)
        private val DESC_TILE_2 = Items.OAK_SIGN.named(CommonComponents.MINESWEEPER_DESC_2)
        private val DESC_TILE_3 = Items.OAK_SIGN.named(CommonComponents.MINESWEEPER_DESC_3)
        private val DESC_TILE_4 = Items.OAK_SIGN.named(CommonComponents.MINESWEEPER_DESC_4)
        private val PLAY_AGAIN_TILE = DisplayItems.GREEN_LONG_RIGHT.named(CommonComponents.MINESWEEPER_PLAY_AGAIN)

        private val FORMAT = DecimalFormat("#.00")

        private var record = 127.0.seconds
    }
}
