package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.tihyo.horror.client.models.legacy.outfits.ModelVampireApparelV1;

import net.minecraft.util.ResourceLocation;

@Mixin(value = ModelVampireApparelV1.class, remap = false)
public abstract class ModelVampireApparelV1Mixin {

	private static final ResourceLocation RED_CAPE = new ResourceLocation(
			"horror:textures/models/armors/legacy/vampire_apparel_v1/cape_red.png");

	@Shadow
	private static ResourceLocation cape0;

	/** Undamaged cape: optional Legends basic red vs default cape_0. */
	@Redirect(
			method = "renderCape",
			at = @At(
					value = "FIELD",
					target = "Lcom/tihyo/horror/client/models/legacy/outfits/ModelVampireApparelV1;cape0:Lnet/minecraft/util/ResourceLocation;",
					opcode = Opcodes.GETSTATIC))
	private static ResourceLocation betterVampires$cape0Texture() {
		return StatBoostConfig.useRedVampireCape ? RED_CAPE : cape0;
	}
}
