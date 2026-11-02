package com.mrsand.StatBoostChanges.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;

/**
 * Legends 5-style bat FP arm: {@code MorphBat.getTexture} ({@code bat_hand.png}) on bipedRightArm.
 * Prefer {@link MorphHandRender#renderRightArm} from Morph.renderRightHand.
 */
public class RenderMyHand {

	public static void renderVampireArm(AbstractClientPlayer player) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || player == null) return;
		MorphHandRender.renderRightArm(mc, player, MorphHandRender.BAT_HAND, null);
	}
}
