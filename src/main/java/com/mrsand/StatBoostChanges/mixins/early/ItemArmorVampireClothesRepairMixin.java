package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.VampireClothesRepair;

import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * Vampire clothes inherit {@link ItemArmor#getIsRepairable}; override here so
 * anvil repair accepts Woven Cruor only (not leather).
 */
@Mixin(ItemArmor.class)
public abstract class ItemArmorVampireClothesRepairMixin {

	private static final String VAMPIRE_CLOTHES = "com.emoniph.witchery.item.ItemVampireClothes";

	@Inject(method = "func_82789_a", at = @At("HEAD"), cancellable = true)
	private void betterVampires$wovenCruorRepair(ItemStack toRepair, ItemStack repair,
			CallbackInfoReturnable<Boolean> cir) {
		if (!VAMPIRE_CLOTHES.equals(((Object) this).getClass().getName())) {
			return;
		}
		cir.setReturnValue(VampireClothesRepair.isWovenCruor(repair));
	}
}
