package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import com.tihyo.battlebourn.handlers.BattlebournCommonEventHandler;

/**
 * Bat Horde AoE tick damage: L8 stock 5.0F → 7.0F.
 */
@Mixin(value = BattlebournCommonEventHandler.class, remap = false)
public abstract class BattlebournBatHordeMixin {

	private static final float BAT_HORDE_DAMAGE = 7.0F;

	@ModifyArg(
			method = "onAbilities",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;BAT_HORDE:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					),
					to = @At(
							value = "NEW",
							target = "com/tihyo/legends/entities/mobs/EntityVampireBat"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/DamageHelper;attack(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;F)V"
			),
			index = 3
	)
	private float bvBuffBatHordeDamage(float damage) {
		return BAT_HORDE_DAMAGE;
	}
}
