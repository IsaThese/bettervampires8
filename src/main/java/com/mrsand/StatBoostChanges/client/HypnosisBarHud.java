package com.mrsand.StatBoostChanges.client;

import org.lwjgl.opengl.GL11;

import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.tihyo.legends.management.ClassColors;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.PlayerHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * Restores Legends 5.4 purple hypnosis tip on the left ability-bar column
 * ({@code AbilityBarEventDisplay.renderHypnosisBar}).
 */
@SideOnly(Side.CLIENT)
public final class HypnosisBarHud extends Gui {

	private static final ResourceLocation TEXTURE = new ResourceLocation("legends", "textures/gui/charge_bar.png");

	private HypnosisBarHud() {}

	public static void render(RenderGameOverlayEvent.Post event) {
		if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;

		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.thePlayer;
		if (player == null) return;
		if (!LegendsHelper.isVampire(player)) return;
		if (!VampireRestoredAbilities.hasHypnosisMastery(player)) return;
		if (!PlayerHelper.hasAbilityBar(player)) return;

		ScaledResolution resolution = event.resolution;
		int width = resolution.getScaledWidth();
		int height = resolution.getScaledHeight();
		int posX = width - 23;
		int posY = height - 23 + 23; // same +23 offset Legends applies for ability bar

		int amount = VampireRestoredAbilities.getHypnosisStrength(player);
		int barWidth = 3;
		int cap = VampireRestoredAbilities.DEFAULT_HYPNOSIS_STRENGTH;
		int ability = (int) ((float) amount / (float) cap * 8.0F);
		int length = (int) ((float) ability / 100.0F * 90.0F);

		GL11.glPushMatrix();
		mc.getTextureManager().bindTexture(TEXTURE);
		drawTexturedModalRectStatic(posX + 2, posY - 71, 0, 55, 9, 11);
		GL11.glPopMatrix();

		mc.getTextureManager().bindTexture(TEXTURE);
		if (length < 150) {
			drawTexturedModalRectStatic(posX + 5, posY - 61 - length, ClassColors.purple, 6, barWidth, length);
		}
	}

	/** Gui.drawTexturedModalRect is instance; expose for static HUD helper. */
	private static final HypnosisBarHud INSTANCE = new HypnosisBarHud();

	private static void drawTexturedModalRectStatic(int x, int y, int u, int v, int w, int h) {
		INSTANCE.drawTexturedModalRect(x, y, u, v, w, h);
	}
}
