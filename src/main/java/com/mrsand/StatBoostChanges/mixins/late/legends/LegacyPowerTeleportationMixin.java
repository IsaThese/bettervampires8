package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.legacy.powers.LegacyPowerTeleportation;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = LegacyPowerTeleportation.class, remap = false)
public abstract class LegacyPowerTeleportationMixin {

	@Shadow
	public abstract int getAssignedKey(EntityPlayer player);

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true )
	public void sunTeleportation(EntityPlayer player, CallbackInfo cir) {
		if (LegacyHelper.useAbility(player, getAssignedKey(player)) && !LegacyHelper.getLegacyClass(player).getWeaknesses(player) 
				&& LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire) {
			if(Loader.isModLoaded("vampirism")) {
				if(VampirismUtil.isInVampireBiome(player) || !player.worldObj.isDaytime()) {
					PowersHandler.localTeleport(player);
				}

			}
			else if(!player.worldObj.isDaytime()) {
				PowersHandler.localTeleport(player);
			}
		}
		//GeneralUtil.checkForSuspicionWitnesses(player, 6000, 4.0D); //This runs continously for some reason causing an error
		cir.cancel();
	}


}
