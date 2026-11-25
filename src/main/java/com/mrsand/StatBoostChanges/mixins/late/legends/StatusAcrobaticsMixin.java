package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tihyo.legends.abilities.status.StatusAcrobatics;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Form of Double Jump (acrobatics) must not apply while Transvection hover is active.
 */
@Mixin(value = StatusAcrobatics.class, remap = false)
public abstract class StatusAcrobaticsMixin {

	@Inject(method = "run(Lnet/minecraft/entity/player/EntityPlayer;I)V", at = @At("HEAD"), cancellable = true)
	private void bvSkipAcrobaticsDuringTransvection(EntityPlayer player, int amplifier, CallbackInfo ci) {
		if (player == null) return;
		Object tv = LegendsProperties.TRANSVECTION.get(player);
		if (tv instanceof Boolean && ((Boolean) tv).booleanValue()) {
			ci.cancel();
		}
	}
}
