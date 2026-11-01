package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.client.WitcheryApparelRender;
import com.tihyo.horror.client.models.characters.ModelDracula;
import com.tihyo.legends.client.models.ModelPlayer;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * When Witchery vampire clothes use {@link ModelDracula} for the coat, bind cape
 * textures from resource-pack {@code vampire_apparel_v1} instead of stock
 * {@code dracula/default/cape_*}. Does not affect real Dracula character morph.
 */
@Mixin(value = ModelDracula.class, remap = false)
public abstract class ModelDraculaMixin {

	private static final String STOCK_CAPE_PREFIX = "textures/models/armors/dracula/default/cape_";
	private static final String PACK_CAPE_PREFIX = "textures/models/armors/legacy/vampire_apparel_v1/cape_";
	private static final ResourceLocation RED_CAPE = new ResourceLocation(
			"horror", "textures/models/armors/legacy/vampire_apparel_v1/cape_red.png");

	@Redirect(
			method = "renderCape",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/texture/TextureManager;func_110577_a(Lnet/minecraft/util/ResourceLocation;)V",
					remap = true))
	private static void betterVampires$bindWitcheryCape(TextureManager textureManager, ResourceLocation location,
			ModelPlayer model, Entity entity, int capeLength) {
		if (entity instanceof EntityPlayer
				&& WitcheryApparelRender.wearsWitcheryVampireChest((EntityPlayer) entity)
				&& location != null
				&& "horror".equals(location.getResourceDomain())
				&& location.getResourcePath() != null
				&& location.getResourcePath().startsWith(STOCK_CAPE_PREFIX)) {
			textureManager.bindTexture(resolvePackCape(location.getResourcePath()));
			return;
		}
		textureManager.bindTexture(location);
	}

	private static ResourceLocation resolvePackCape(String stockPath) {
		// stockPath e.g. textures/models/armors/dracula/default/cape_0.png
		int stage = 0;
		int under = stockPath.lastIndexOf('_');
		int dot = stockPath.lastIndexOf('.');
		if (under >= 0 && dot > under) {
			try {
				stage = Integer.parseInt(stockPath.substring(under + 1, dot));
			} catch (NumberFormatException ignored) {
				stage = 0;
			}
		}
		if (stage == 0 && StatBoostConfig.useRedVampireCape) {
			return RED_CAPE;
		}
		if (stage < 0) stage = 0;
		if (stage > 2) stage = 2;
		return new ResourceLocation("horror", PACK_CAPE_PREFIX + stage + ".png");
	}
}
