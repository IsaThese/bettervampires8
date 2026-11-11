package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.tihyo.legends.management.PlayerHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin {

	/**
	 * While nested in a Legends coffin, cancel vanilla wake eject (Space/damage/daytime).
	 * Exit is owned by {@link CoffinSleepUtil#exitCoffin}.
	 */
	@Inject(method = "func_70999_a", at = @At("HEAD"), cancellable = true)
	private void bvCancelWakeWhileInCoffin(boolean immediately, boolean updateWorld, boolean setSpawn, CallbackInfo ci) {
		EntityPlayer player = (EntityPlayer) (Object) this;
		if (!Loader.isModLoaded("legends")) return;
		if (CoffinSleepUtil.isExitAllowed()) return;
		try {
			if (PlayerHelper.isInCoffin(player)) {
				ci.cancel();
			}
		} catch (Throwable ignored) {
		}
	}

	@Inject(method = "func_71043_e", at = @At("HEAD"), cancellable = true)
	private void overrideCanEatForVampires(boolean alwaysEdible, CallbackInfoReturnable<Boolean> cir) {
		EntityPlayer player = (EntityPlayer) (Object) this;
		if (!player.capabilities.disableDamage && GeneralUtil.isVampire(player)) {
			cir.setReturnValue(true);
		}
	}
}
