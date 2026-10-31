package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.Shapeshift;
import com.emoniph.witchery.common.Shapeshift.StatBoost;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyRegistry;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = Shapeshift.class, remap = false)
public abstract class ShapeshiftMixin {
	
		//Sets Damage Multiplier to 0 if Legends Vampire
	  @Inject(method = "getStatBoost", at = @At("HEAD"), cancellable = true)
	  private void legendsVampireStat(EntityPlayer player, ExtendedPlayer playerEx, CallbackInfoReturnable<StatBoost> cir) {
		    if(LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire) {
		    	cir.setReturnValue(new StatBoost(0.0F));
		    }
	  }
}
