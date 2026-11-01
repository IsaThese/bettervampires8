package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.VampireHostility;
import com.tihyo.legends.entities.mobs.EntityHerobrine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Herobrine attacks normal vampires; only Vampire Lords are refused.
 */
@Mixin(value = EntityHerobrine.class, remap = false)
public abstract class EntityHerobrineHostilityMixin {

	@Inject(method = "isSuitableTarget", at = @At("RETURN"), cancellable = true)
	private void bvLordOnly(EntityLivingBase self, EntityLivingBase candidate, boolean flag,
			CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
		if (!(candidate instanceof EntityPlayer)) return;
		Entity attacker = (Entity) (Object) this;
		if (VampireHostility.shouldIgnoreAttacker(attacker, (EntityPlayer) candidate)) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
