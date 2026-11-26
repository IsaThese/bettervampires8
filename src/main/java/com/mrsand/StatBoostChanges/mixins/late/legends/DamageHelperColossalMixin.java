package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.LordColossalCombat;
import com.tihyo.legends.management.DamageHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * True Form ×1.5 on Legends ability damage + colossal %maxHP bonus after hits.
 */
@Mixin(value = DamageHelper.class, remap = false)
public abstract class DamageHelperColossalMixin {

	@ModifyVariable(
			method = "attack",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 0
	)
	private static float bvTrueFormAndColossalAbilityDamage(float damage, Entity victim, Entity attacker,
			DamageSource source) {
		float scaled = LordColossalCombat.scaleAbilityDamageForTrueForm(victim, attacker, source, damage);
		// Fold %maxHP into this hit so OreSpawn hurt_timer cannot eat a follow-up.
		return LordColossalCombat.addColossalBonusToAbilityDamage(victim, attacker, source, scaled);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private static void bvWarnUnresolvedColossal(Entity victim, Entity attacker, DamageSource source, float damage,
			CallbackInfo ci) {
		if (!(victim instanceof EntityLivingBase)) return;
		EntityLivingBase living = (EntityLivingBase) victim;
		if (!LordColossalCombat.isColossal(living)) return;
		// Resolution warning only — bonus already folded into damage above.
		LordColossalCombat.resolveLord(source, attacker, victim, true);
	}
}
