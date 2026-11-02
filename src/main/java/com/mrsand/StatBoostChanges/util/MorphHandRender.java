package com.mrsand.StatBoostChanges.util;

import org.lwjgl.opengl.GL11;

import com.tihyo.legends.management.LegendsRenderHelper;
import com.tihyo.legends.morph.Morph;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Draws morph FP arms. Legends 5/8 style: bat_hand / wolf texture on bipedRightArm.
 * Call from Morph.renderRightHand after ItemRenderer + RenderPlayerHand have positioned the hand.
 */
@SideOnly(Side.CLIENT)
public final class MorphHandRender {

	public static final ResourceLocation BAT_HAND = new ResourceLocation("legends", "textures/skins/bat_hand.png");
	public static final ResourceLocation WOLF_HAND = new ResourceLocation("bettervampires", "textures/wolf_hand.png");

	/** Dedicated FP arm — do not depend on RenderPlayerHand.modelBipedMain / parent. */
	private static final ModelBiped FP_ARM = new ModelBiped(0.0F);

	private static boolean loggedBatDraw;

	private MorphHandRender() {}

	/**
	 * @return texture for bat/wolf morph, or null if this player should keep vanilla FP arm
	 */
	public static ResourceLocation textureFor(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("legends")) return null;
		try {
			return textureForLegendsMorph(player);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static ResourceLocation textureForLegendsMorph(EntityPlayer player) {
		if (MorphHelper.isBat(player)) return BAT_HAND;
		if (MorphHelper.isWolf(player)) return WOLF_HAND;
		return null;
	}

	/**
	 * Called from {@code RenderPlayer.renderFirstPersonArm} while ItemRenderer matrix is active.
	 * @return true if vanilla arm draw should be canceled
	 */
	public static boolean tryReplaceFirstPersonArm(EntityPlayer player, RenderPlayer render) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null || player != mc.thePlayer) return false;
		if (!Loader.isModLoaded("legends")) return false;
		try {
			if (MorphHelper.isVampireBeast(player)) {
				Morph morph = MorphHelper.getMorphClass(player);
				if (morph == null) return false;
				morph.renderRightHand(mc, player);
				return true;
			}
		} catch (Throwable ignored) {
			return false;
		}
		ResourceLocation texture = textureFor(player);
		if (texture == null) return false;
		renderRightArm(mc, player, texture);
		return true;
	}

	/**
	 * Call from Morph.renderRightHand after Legends has positioned the FP hand.
	 */
	public static void renderRightArm(Minecraft mc, EntityPlayer player, ResourceLocation texture) {
		renderRightArm(mc, player, texture, null);
	}

	public static void renderRightArm(Minecraft mc, EntityPlayer player, ResourceLocation texture, RenderPlayer render) {
		if (mc == null || player == null || texture == null) return;

		// L8 shiny/glint passes call Morph.renderRightHand under additive blend + depthMask(false).
		try {
			if (LegendsRenderHelper.overrideColor) {
				return;
			}
		} catch (Throwable ignored) {
		}

		GL11.glPushMatrix();
		try {
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			mc.getTextureManager().bindTexture(texture);
			FP_ARM.onGround = 0.0F;
			FP_ARM.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
			FP_ARM.bipedRightArm.render(0.0625F);

			if (!loggedBatDraw && texture == BAT_HAND && HandRenderDebug.shouldProbe()) {
				loggedBatDraw = true;
				HandRenderDebug.logImportant("Morph FP arm DRAW bat_hand (ModelBiped)", true);
			}
		} finally {
			GL11.glPopMatrix();
		}
	}
}
