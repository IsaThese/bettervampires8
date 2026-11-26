package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.tihyo.legends.legacy.powers.LegacyPowerVampiresWrath;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Merge lord Wrath into stock {@code vampiresWrath}: UI description reflects
 * latch targeting + once-per-latch Witchery 1/3 (behavior is in Horror latch mixin).
 */
@Mixin(value = LegacyPowerVampiresWrath.class, remap = false)
public abstract class LegacyPowerVampiresWrathMixin {

	@Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
	private void bvLordWrathDescription(EntityPlayer player, CallbackInfoReturnable<String> cir) {
		if (!DaywalkerControl.isVampireLord(player)) return;
		cir.setReturnValue(
				"Vampire's Wrath: latch from 3–5 blocks (snaps to ~2), drain life while channeling. "
						+ "Once per latch: Witchery blood = max × (1/3). Special cooldown.");
	}
}
