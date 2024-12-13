package net.casual.championships.common.mixin.event;

import net.casual.arcade.events.GlobalEventHandler;
import net.casual.championships.common.event.TippedArrowTradeOfferEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VillagerTrades.TippedArrowForItemsAndEmeralds.class)
public class TippedArrowForItemsAndEmeraldsMixin {
	@ModifyVariable(
		method = "getOffer",
		at = @At(value = "STORE")
	)
	private Holder<Potion> onGetRandomPotion(Holder<Potion> original, Entity trader) {
		TippedArrowTradeOfferEvent event = new TippedArrowTradeOfferEvent(trader, original);
		GlobalEventHandler.Server.broadcast(event);
		return event.getPotion();
	}
}
