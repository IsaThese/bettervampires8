package com.mrsand.StatBoostChanges.util;

import org.lwjgl.opengl.GL11;

import com.tihyo.legends.morph.MorphBat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.ResourceLocation;

public class RenderMyHand {


	private static final ResourceLocation VAMPIRE_TEXTURE = new ResourceLocation("bettervampires", "textures/entity/vampire_arm.png");

	/**
	 * This is the method you call to draw the arm.
	 */
	public static void renderVampireArm(AbstractClientPlayer player) {
		Minecraft mc = Minecraft.getMinecraft();

		RenderPlayer renderPlayer = (RenderPlayer) RenderManager.instance.getEntityRenderObject(player);

		MorphBat batHand = new MorphBat();
		mc.getTextureManager().bindTexture(batHand.getTexture(player));

		GL11.glPushMatrix();

		renderPlayer.modelBipedMain.onGround = 0.0F;
		renderPlayer.modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
		renderPlayer.modelBipedMain.bipedRightArm.render(0.0625F);

		GL11.glPopMatrix();
	}

}
