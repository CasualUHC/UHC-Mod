package net.casual.championships.mixin.feature;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.RateKickingConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RateKickingConnection.class)
public class RateKickingConnectionMixin {
	@Unique private long lastTick = -1L;

	@ModifyExpressionValue(
		method = "tickSecond",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/RateKickingConnection;getAverageReceivedPackets()F"
		)
	)
	private float onGetAverageReceivedPackets(float original) {
		long last = this.lastTick;
		long current = System.currentTimeMillis();
		this.lastTick = current;
		if (last != -1) {
			float delta = (current - last) / 1000.0F;
			if (delta >= 1.0F) {
				return original / delta;
			}
		}
		return original;
	}
}
