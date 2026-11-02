package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.util.VampireEyesDebug;
import com.mrsand.StatBoostChanges.util.VampireEyesVisual;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.PlayerRenderHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Gate vampire-eye overlay on BV modes; bind BV 1x2 sheet when {@code EYE_TYPE == 3};
 * scale/disable Legends fullbright glow via {@link StatBoostConfig#vampireEyesGlow}.
 */
@Mixin(value = PlayerRenderHelper.class, remap = false)
public abstract class PlayerRenderHelperVampireEyesMixin {

	private static final int STOCK_EYE_LIGHTING = 61680;

	private static final ThreadLocal<EntityPlayer> BV_EYE_PLAYER = new ThreadLocal<EntityPlayer>();

	/**
	 * Stock checks {@code getVampireEyes != 0}. Remap stored mode → draw/skip for this frame.
	 */
	@Redirect(
			method = "renderBipedPost",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/PlayerHelper;getVampireEyes(Lnet/minecraft/entity/player/EntityPlayer;)I"
			)
	)
	private static int bv$vampireEyesVisible(EntityPlayer player) {
		BV_EYE_PLAYER.set(player);
		VampireEyesDebug.logReplayRender(player);
		return VampireEyesVisual.shouldDrawOverlay(player) ? 1 : 0;
	}

	/**
	 * Eye type 3 falls through to the stock 1x2 bind (3rd bindTexture after getVampireEyes).
	 */
	@ModifyArg(
			method = "renderBipedPost",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;getVampireEyes(Lnet/minecraft/entity/player/EntityPlayer;)I"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/texture/TextureManager;func_110577_a(Lnet/minecraft/util/ResourceLocation;)V",
					ordinal = 2
			),
			index = 0
	)
	private static ResourceLocation bv$swapBvEyeTexture(ResourceLocation original) {
		EntityPlayer player = BV_EYE_PLAYER.get();
		if (player != null && PlayerHelper.getEyeType(player) == VampireEyesVisual.EYE_TYPE_BV_1X2) {
			return VampireEyesVisual.BV_1X2_EYES;
		}
		return original;
	}

	/**
	 * Scale stock {@code setLighting(61680)} by {@link StatBoostConfig#vampireEyesGlow}.
	 */
	@ModifyArg(
			method = "renderBipedPost",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;getVampireEyes(Lnet/minecraft/entity/player/EntityPlayer;)I"
					),
					to = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;getSithEyes(Lnet/minecraft/entity/player/EntityPlayer;)I"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/LegendsRenderHelper;setLighting(I)V"
			),
			index = 0
	)
	private static int bv$scaleEyeLighting(int lighting) {
		float glow = StatBoostConfig.vampireEyesGlow;
		if (glow <= 0.0F) {
			return 0;
		}
		if (glow >= 1.0F) {
			return lighting;
		}
		return (int) (STOCK_EYE_LIGHTING * glow);
	}

	/**
	 * Skip {@code glDisable(GL_LIGHTING)} when glow is off so eyes use ambient lighting.
	 */
	@Redirect(
			method = "renderBipedPost",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;getVampireEyes(Lnet/minecraft/entity/player/EntityPlayer;)I"
					),
					to = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;getSithEyes(Lnet/minecraft/entity/player/EntityPlayer;)I"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"
			)
	)
	private static void bv$maybeKeepLighting(int cap) {
		if (cap == GL11.GL_LIGHTING && StatBoostConfig.vampireEyesGlow <= 0.0F) {
			return;
		}
		GL11.glDisable(cap);
	}
}
