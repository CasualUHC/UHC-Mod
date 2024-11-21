package net.casual.championships.uhc

import net.casual.arcade.resources.font.FontResources

object UHCComponents {
    object Bitmap: FontResources(UHCMod.id("bitmap_font")) {
        val TITLE = bitmap(at("uhc_title.png"), 8, 9)

        val PLAYER_BACKGROUND = bitmap(at("player_background.png"), 9, 10)
    }
}