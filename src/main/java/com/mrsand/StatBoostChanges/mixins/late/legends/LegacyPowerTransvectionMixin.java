package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.powers.LegacyPowerTransvection;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Humanoid Transvection: ability key toggles hover off (stock only turns on;
 * stock used right-click to clear).
 */
@Mixin(value = LegacyPowerTransvection.class, remap = false)
public abstract class LegacyPowerTransvectionMixin {

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bvToggleOffHumanoidHover(EntityPlayer player, CallbackInfo ci) {
		if (!LegendsUtil.isHumanoidKeyedTransvectionHover(player)) return;
		PowersHandler.transvection(player, false);
		ci.cancel();
	}

	@Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
	private void bvKeybindToggleDescription(EntityPlayer player, CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(
				"Hover in the air, granting yourself the ability to levitate. Press again to toggle off.");
	}
}
