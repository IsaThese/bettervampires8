package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.tihyo.legends.legacy.powers.LegacyPowerMorphVampireBeast;

import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = LegacyPowerMorphVampireBeast.class, remap = false)

public abstract class LegacyPowerMorphBeastMixin {

	@Shadow
	public abstract int getAssignedKey(EntityPlayer player);

//	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
//	private void beastDarkMagic(EntityPlayer player, CallbackInfo cir) {
//		if (LegacyHelper.useAbility(player, getAssignedKey(player)) && !LegacyHelper.getLegacyClass(player).getWeaknesses(player)) {
//			LegendsUtil.ifMorphedTurnHuman(player);
//			GeneralUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);
//		}
//	}

}
