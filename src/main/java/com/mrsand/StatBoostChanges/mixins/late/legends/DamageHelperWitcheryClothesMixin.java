package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.VampireClothesRepair;
import com.tihyo.legends.management.DamageHelper;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Keep Legends suit soft-cap / repair paths off Witchery vampire clothes.
 * Wear/mend for those pieces is owned by {@link VampireClothesRepair}.
 */
@Mixin(value = DamageHelper.class, remap = false)
public abstract class DamageHelperWitcheryClothesMixin {

	@Inject(method = "applySafeDamage", at = @At("HEAD"), cancellable = true)
	private static void betterVampires$skipWitcheryClothes(ItemStack stack, int amount,
			EntityLivingBase entity, CallbackInfo ci) {
		if (VampireClothesRepair.isVampireClothes(stack)) {
			ci.cancel();
		}
	}

	/** Legends mend uses negative damageItem and can drive stacks below 0. */
	@Inject(method = "repairArmor(Lnet/minecraft/entity/player/EntityPlayer;I)V", at = @At("RETURN"))
	private static void betterVampires$clampAfterRepair(EntityPlayer player, int amount, CallbackInfo ci) {
		VampireClothesRepair.clampWornVampireClothes(player);
	}

	@Inject(method = "repairArmor(Lnet/minecraft/entity/player/EntityPlayer;IZ)V", at = @At("RETURN"))
	private static void betterVampires$clampAfterRepairFiltered(EntityPlayer player, int amount, boolean all,
			CallbackInfo ci) {
		VampireClothesRepair.clampWornVampireClothes(player);
	}
}
