package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tihyo.legends.management.LegendsRenderHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Early mixin: LegendsRenderHelper loads before late mixins can apply.
 * Witchery vampire chest is not on Legends' FP-hand whitelist — allow it so
 * {@code RenderPlayerHand} does not early-return with no hand drawn.
 */
@Mixin(value = LegendsRenderHelper.class, remap = false)
public abstract class LegendsRenderHelperMixin {

	private static final String VAMPIRE_CLOTHES = "com.emoniph.witchery.item.ItemVampireClothes";

	@Inject(method = "shouldUseLegendsRender", at = @At("HEAD"), cancellable = true)
	private static void betterVampires$witcheryVampireClothes(Entity entity,
			CallbackInfoReturnable<Boolean> cir) {
		if (wearsWitcheryVampireChest(entity)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "shouldRenderHand", at = @At("HEAD"), cancellable = true)
	private static void betterVampires$witcheryShouldRenderHand(EntityPlayer player,
			CallbackInfoReturnable<Boolean> cir) {
		if (wearsWitcheryVampireChest(player)) {
			cir.setReturnValue(true);
		}
	}

	private static boolean wearsWitcheryVampireChest(Entity entity) {
		if (!(entity instanceof EntityPlayer)) {
			return false;
		}
		ItemStack chest = ((EntityPlayer) entity).getCurrentArmor(2);
		return chest != null && chest.getItem() != null
				&& VAMPIRE_CLOTHES.equals(chest.getItem().getClass().getName());
	}
}
