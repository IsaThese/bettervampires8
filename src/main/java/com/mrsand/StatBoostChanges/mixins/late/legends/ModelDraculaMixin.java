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
 * Witchery {@code ItemVampireClothes} coat uses {@link ModelDracula}; remap cape
 * binds to pack {@code cape_red*} / {@code cape_0..2} when configured.
 * Does not change real Dracula morph or Legends default apparel.
 */
@Mixin(value = ModelDracula.class, remap = false)
public abstract class ModelDraculaMixin {

	private static final String STOCK_CAPE_PREFIX = "textures/models/armors/dracula/default/cape_";
	private static final String PACK_DIR = "textures/models/armors/legacy/vampire_apparel_v1/";
	private static final ResourceLocation CAPE_RED = new ResourceLocation(
			"horror", PACK_DIR + "cape_red.png");
	private static final ResourceLocation CAPE_RED_2 = new ResourceLocation(
			"horror", PACK_DIR + "cape_red_2.png");
	private static final ResourceLocation CAPE_0 = new ResourceLocation(
			"horror", PACK_DIR + "cape_0.png");
	private static final ResourceLocation CAPE_1 = new ResourceLocation(
			"horror", PACK_DIR + "cape_1.png");
	private static final ResourceLocation CAPE_2 = new ResourceLocation(
			"horror", PACK_DIR + "cape_2.png");

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
			ResourceLocation remapped = resolveWitcheryCape(location.getResourcePath());
			if (remapped != null) {
				textureManager.bindTexture(remapped);
				return;
			}
		}
		textureManager.bindTexture(location);
	}

	/**
	 * @return pack cape, or {@code null} to keep stock (style 2 / unknown).
	 */
	private static ResourceLocation resolveWitcheryCape(String stockPath) {
		int style = StatBoostConfig.witcheryVampireCapeStyle;
		if (style < 0 || style > 2) {
			style = 0;
		}
		if (style == 2) {
			return null;
		}

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
		if (stage < 0) stage = 0;
		if (stage > 2) stage = 2;

		if (style == 0) {
			return stage >= 2 ? CAPE_RED_2 : CAPE_RED;
		}
		if (stage <= 0) return CAPE_0;
		if (stage == 1) return CAPE_1;
		return CAPE_2;
	}
}
