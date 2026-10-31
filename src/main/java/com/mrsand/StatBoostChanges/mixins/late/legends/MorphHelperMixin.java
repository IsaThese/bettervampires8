package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.morph.MorphHelper;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = MorphHelper.class, remap = false)
public abstract class MorphHelperMixin {

	// Enables Legends ModMorphs to use any morph without needing Legends Armor
	//I'm pretty sure this was disabled because it caused armor glitches unless hiding armor

	@Inject(method = "hasIllegalArmor", at = @At("HEAD"), cancellable = true)
	private static void alwaysAllowArmor(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if(LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire) {
	    cir.setReturnValue(false);
	    cir.cancel();
		}
	}

}
