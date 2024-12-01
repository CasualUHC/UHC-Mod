package net.casual.championships.minigame.lobby

import net.casual.arcade.utils.AdvancementUtils.setTitleAndDesc
import net.casual.arcade.utils.ItemUtils
import net.casual.arcade.utils.advancement.AdvancementCollection
import net.casual.championships.CasualMod
import net.casual.championships.common.items.MinesweeperItems
import net.minecraft.advancements.AdvancementType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Items

object LobbyAdvancements: AdvancementCollection() {
    val ROOT = register {
        id = CasualMod.id("root")
        display(Items.GOLDEN_APPLE)
        setTitleAndDesc("lobby.advancements.root")
        background = ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/adventure.png")
    }

    val UH_OH = register {
        parent(ROOT)
        id = CasualMod.id("uh_oh")
        display(Items.BARRIER)
        setTitleAndDesc("lobby.advancements.uhOh")
        toast()
        announce()
    }

    val PARKOUR_MASTER = register {
        parent(UH_OH)
        id = CasualMod.id("parkour_master")
        display(Items.NETHERITE_BOOTS)
        setTitleAndDesc("lobby.advancements.parkourMaster")
        type = AdvancementType.CHALLENGE
        toast()
        announce()
    }

    val NOT_NOW = register {
        parent(ROOT)
        id = CasualMod.id("not_now")
        display(Items.NETHERITE_SWORD)
        setTitleAndDesc("lobby.advancements.notNow")
        announce()
    }

    val OFFICIALLY_BORED = register {
        parent(ROOT)
        id = CasualMod.id("officially_bored")
        display(Items.COMMAND_BLOCK)
        setTitleAndDesc("lobby.advancements.officiallyBored")
        toast()
        announce()
    }

    val ADMIN_ABUSE = register {
        parent(OFFICIALLY_BORED)
        id = CasualMod.id("admin_abuse")
        display(ItemUtils.createPlayerHead("senseiwells"))
        setTitleAndDesc("lobby.advancements.adminAbuse")
        toast()
        announce()
    }

    val GAMER = register {
        parent(ROOT)
        id = CasualMod.id("gamer")
        display(MinesweeperItems.FLAG_COUNTER)
        setTitleAndDesc("lobby.advancements.gamer")
        toast()
        announce()
    }

    val YOU_SHALL_NOT_LEAVE = register {
        parent(ROOT)
        id = CasualMod.id("you_shall_not_leave")
        display(Items.STRUCTURE_VOID)
        setTitleAndDesc("lobby.advancements.youShallNotLeave")
        toast()
        announce()
    }
}