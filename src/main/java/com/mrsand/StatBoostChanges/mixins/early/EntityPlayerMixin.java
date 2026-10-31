package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.GeneralUtil;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin {

	//WakeUp Player
	@Inject(method = "func_70999_a", at = @At("HEAD"), cancellable = true)
	private void wakeUpPlayerNotIfVamprisim(boolean immediately, boolean updateWorld, boolean setSpawn, CallbackInfo ci) {
		//		if(!Loader.isModLoaded("vampirism")) return;
		//		else {
		//			
		//			EntityPlayer player = (EntityPlayer) (Object) this;
		//			boolean allowedToWake = de.teamlapen.vampirism.coremod.CoreHandler.shouldWakePlayer(player, immediately, updateWorld, setSpawn);
		//		    if (!allowedToWake) {
		//		    	ci.cancel();
		//		        return; // Aborts the method entirely! The player stays in bed.
		//		    }
		//		}
	}
	//canEat
	@Inject(method = "func_71043_e", at = @At("HEAD"), cancellable = true)
	private void overrideCanEatForVampires(boolean alwaysEdible, CallbackInfoReturnable<Boolean> cir) {
		EntityPlayer player = (EntityPlayer) (Object) this;
//		GeneralUtil.sendDebugMessage(player, "override canEat method");
		if (!player.capabilities.disableDamage && GeneralUtil.isVampire(player)) {
			cir.setReturnValue(true);
		}
	}
}




