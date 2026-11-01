package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;

import net.minecraft.entity.player.EntityPlayer;

/**
 * When removeLegacyDaywalker is on: block learning Daywalker; treat Vampirism
 * lords as Daywalker for sun burn checks; force non-lords as not having it.
 */
@Mixin(value = LegacyHelper.class, remap = false)
public abstract class LegacyHelperMixin {

	@Inject(method = "hasLearnedPower", at = @At("HEAD"), cancellable = true)
	private static void bvDaywalkerLearned(EntityPlayer player, LegacyPower power,
			CallbackInfoReturnable<Boolean> cir) {
		if (!DaywalkerControl.isEnabled() || power != LegacyRegistry.daywalker) return;
		if (DaywalkerControl.isVampireLord(player)) {
			cir.setReturnValue(Boolean.TRUE);
			return;
		}
		cir.setReturnValue(Boolean.FALSE);
	}

	@Inject(method = "canBeLearned", at = @At("HEAD"), cancellable = true)
	private static void bvBlockDaywalkerLearn(EntityPlayer player, LegacyPower power,
			CallbackInfoReturnable<Boolean> cir) {
		if (!DaywalkerControl.isEnabled() || power != LegacyRegistry.daywalker) return;
		cir.setReturnValue(Boolean.FALSE);
	}
}
