package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import com.mrsand.StatBoostChanges.util.LordColossalCombat;
import com.mrsand.StatBoostChanges.util.VampiricDrainHits;
import com.tihyo.battlebourn.handlers.BattlebournCommonEventHandler;
import com.tihyo.legends.management.DamageHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * Vampiric Drain: config range/wideness targeting + clear OreSpawn/vanilla i-frames
 * on blood-pulse ticks so large bosses take full DamageHelper hits (incl. colossal %).
 */
@Mixin(value = BattlebournCommonEventHandler.class, remap = false)
public abstract class BattlebournBloodDrainMixin {

	@Redirect(
			method = "onAbilities",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;BLOOD_DRAIN:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					),
					to = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;BAT_HORDE:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/LegendsHelper;getTarget(Lnet/minecraft/entity/player/EntityPlayer;D)Lnet/minecraft/entity/EntityLivingBase;"
			)
	)
	private EntityLivingBase bvDrainWiderTarget(EntityPlayer player, double stockRange) {
		return VampiricDrainHits.getDrainTarget(player);
	}

	@Redirect(
			method = "onAbilities",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;BLOOD_DRAIN:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					),
					to = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;BAT_HORDE:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/DamageHelper;attack(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;F)V"
			)
	)
	private void bvDrainPulseAttack(Entity victim, Entity attacker, DamageSource source, float damage) {
		if (!(attacker instanceof EntityPlayer)) {
			DamageHelper.attack(victim, attacker, source, damage);
			return;
		}
		EntityPlayer player = (EntityPlayer) attacker;
		if (!VampiricDrainHits.isDrainDamagePulse(player)) {
			return;
		}
		if (victim instanceof EntityLivingBase) {
			LordColossalCombat.clearBossIframes((EntityLivingBase) victim);
		}
		DamageHelper.attack(victim, attacker, source, damage);
	}
}
