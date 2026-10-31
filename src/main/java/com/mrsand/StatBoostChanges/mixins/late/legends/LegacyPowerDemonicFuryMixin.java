package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tihyo.legends.legacy.powers.LegacyPowerDemonicFury;

@Mixin(value = LegacyPowerDemonicFury.class, remap = false)
public class LegacyPowerDemonicFuryMixin {
	@Inject(method = "getCost", at = @At("HEAD"), cancellable = true)
	  public void getCost(CallbackInfoReturnable <String>cir) {
	    //return "100";
		cir.setReturnValue("60");
	  }

}

