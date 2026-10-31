package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.legacy.powers.LegacyPowerWallClimbing;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = LegacyPowerWallClimbing.class, remap = false)
public abstract class LegacyPowerWallClimbingMixin {
	
	@Inject(method = "execute", at=@At("HEAD"), cancellable = true)
	public void execute(EntityPlayer player, CallbackInfo ci) {
		ci.cancel();
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		if(vPlayer.getBoolean(PlayerKeys.WALL_CLIMB)) {
		LegendsAbilities.wallClimbing(player, 2);
		}
	  }

}
