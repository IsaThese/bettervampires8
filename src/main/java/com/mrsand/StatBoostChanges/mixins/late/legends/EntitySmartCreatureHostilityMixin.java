package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.VampireHostility;
import com.tihyo.legends.entities.mobs.EntitySmartCreature;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Drowners/Godzilla/Herobrine/etc. use DataWatcher targeting, not only EntityMob AI.
 * Refuse ignored vampire targets before the track ID is set.
 */
@Mixin(value = EntitySmartCreature.class, remap = false)
public abstract class EntitySmartCreatureHostilityMixin {

	@Shadow
	public abstract void setTargetID(int id);

	@Inject(method = "setTargetToTrack", at = @At("HEAD"), cancellable = true)
	private void bvRefuseIgnoredVampire(EntityLivingBase self, EntityLivingBase target, CallbackInfo ci) {
		if (!(target instanceof EntityPlayer)) return;
		Entity attacker = (Entity) (Object) this;
		if (VampireHostility.shouldIgnoreAttacker(attacker, (EntityPlayer) target)) {
			setTargetID(-1);
			ci.cancel();
		}
	}
}
