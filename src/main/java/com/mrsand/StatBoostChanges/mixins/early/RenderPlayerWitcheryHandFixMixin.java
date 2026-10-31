package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Witchery vampire clothes use custom {@code getArmorModel} with shared ModelBiped
 * showModel flags. Ensure the FP skin arm is still drawn.
 */
@Mixin(RenderPlayer.class)
public abstract class RenderPlayerWitcheryHandFixMixin {

	private static final String VAMPIRE_CLOTHES = "com.emoniph.witchery.item.ItemVampireClothes";

	@Inject(method = "func_82441_a", at = @At("HEAD"))
	private void betterVampires$ensureFpArmVisible(EntityPlayer player, CallbackInfo ci) {
		if (player == null || !wearsVampireClothes(player)) {
			return;
		}
		ModelBiped main = ((RenderPlayer) (Object) this).modelBipedMain;
		if (main != null && main.bipedRightArm != null) {
			main.bipedRightArm.showModel = true;
		}
	}

	private static boolean wearsVampireClothes(EntityPlayer player) {
		ItemStack[] armor = player.inventory.armorInventory;
		for (int i = 0; i < 4; i++) {
			ItemStack s = armor[i];
			if (s != null && s.getItem() != null
					&& VAMPIRE_CLOTHES.equals(s.getItem().getClass().getName())) {
				return true;
			}
		}
		return false;
	}
}
