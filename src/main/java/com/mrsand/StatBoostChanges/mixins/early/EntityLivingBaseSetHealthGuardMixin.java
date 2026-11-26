package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.util.LordColossalCombat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Blocks mods that call {@code setHealth} to roll back heals (OreSpawn Queen
 * {@code ev}/{@code evh}) without going through combat. Legitimate drops are
 * marked from {@code LivingHurtEvent} before {@code damageEntity} runs.
 */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseSetHealthGuardMixin {

	/** {@code setHealth} — cancel out-of-band HP rollbacks only. */
	@Inject(method = "func_70606_j", at = @At("HEAD"), cancellable = true)
	private void bvGuardSetHealth(float health, CallbackInfo ci) {
		EntityLivingBase self = (EntityLivingBase) (Object) this;
		if (!(self instanceof EntityPlayer)) return;
		if (self.worldObj != null && self.worldObj.isRemote) return;
		EntityPlayer player = (EntityPlayer) self;
		float cur = player.getHealth();
		if (!(health < cur)) return;
		// Real combat: LivingHurt armed an expected drop for this player.
		if (LordColossalCombat.consumeExpectedHealthDrop(player)) return;
		if (LordColossalCombat.isAllowDirectHealthDrop()) return;

		boolean invuln = LordColossalCombat.isDeathSaveInvulnerable(player);
		boolean lord = false;
		try {
			lord = DaywalkerControl.isVampireLord(player);
		} catch (Throwable ignored) {
		}
		if (!invuln && !lord) return;

		ci.cancel();
	}
}
