package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.StatBoostChanges;
import com.mrsand.StatBoostChanges.common.packets.VisualPacketFromServer;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = PowersHandler.class, remap = false)
public abstract class PowersHandlerMixin {

	@Inject(method = "bloodVision", at = @At("HEAD"), cancellable = true)
	private static void bloodVisionChangeForm(EntityPlayer player, boolean flag, CallbackInfo ci) {
		ci.cancel();
		Boolean current = (Boolean) LegendsProperties.BLOOD_VISION.get(player);
		boolean on = current != null && current.booleanValue();
		if (flag != on) {
			LegendsProperties.BLOOD_VISION.set(player, Boolean.valueOf(flag));
		}
		StatBoostChanges.network.sendToAll(new VisualPacketFromServer(player.getEntityId(), flag));
	}
}
