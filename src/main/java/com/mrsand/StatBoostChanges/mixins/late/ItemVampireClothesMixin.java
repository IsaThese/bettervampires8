package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.item.ItemVampireClothes;
import com.mrsand.StatBoostChanges.client.WitcheryApparelRender;
import com.mrsand.StatBoostChanges.util.VampireClothesRepair;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

/**
 * Durability pool (200) for Witchery vampire clothes, plus Dracula-model
 * render remaps. Pieces remain fully breakable.
 */
@Mixin(value = ItemVampireClothes.class, remap = false)
public abstract class ItemVampireClothesMixin {

	private static final int LEGENDS_LIKE_DURABILITY = 200;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void betterVampires$legendsLikeDurability(int armorType, boolean female, boolean metal, CallbackInfo ci) {
		((Item) (Object) this).setMaxDamage(LEGENDS_LIKE_DURABILITY);
	}

	@Inject(method = "damageArmor", at = @At("HEAD"), cancellable = true)
	private void betterVampires$breakableDamage(EntityLivingBase entity, ItemStack stack, DamageSource source,
			int damage, int slot, CallbackInfo ci) {
		VampireClothesRepair.applyDamage(stack, damage, entity);
		ci.cancel();
	}

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

