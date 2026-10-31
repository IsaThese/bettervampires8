package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.item.ItemVampireClothes;
import com.mrsand.StatBoostChanges.client.WitcheryApparelRender;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

@Mixin(value = ItemVampireClothes.class, remap = false)
public abstract class ItemVampireClothesMixin {

	@SideOnly(Side.CLIENT)
	@Inject(method = "getArmorModel", at = @At("HEAD"), cancellable = true)
	private void betterVampires$legendsArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, int slot,
			CallbackInfoReturnable<ModelBiped> cir) {
		cir.setReturnValue(WitcheryApparelRender.getArmorModel(entityLiving, itemStack, slot));
	}

	@SideOnly(Side.CLIENT)
	@Inject(method = "getArmorTexture", at = @At("HEAD"), cancellable = true)
	private void betterVampires$legendsArmorTexture(ItemStack stack, Entity entity, int slot, String type,
			CallbackInfoReturnable<String> cir) {
		String texture = WitcheryApparelRender.getArmorTexture(stack, slot);
		if (texture != null) {
			cir.setReturnValue(texture);
		}
	}
}

