package com.mrsand.StatBoostChanges.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only gate for Horror mist THICK_SMOKE (3/tick vanilla → sparse).
 */
public final class MistSmokeThrottle {

	private static int lastTick = -1;
	private static int spawnedThisTick;

	private MistSmokeThrottle() {}

	public static boolean allow() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null) return false;
		int tick = mc.thePlayer.ticksExisted;
		if (tick != lastTick) {
			lastTick = tick;
			spawnedThisTick = 0;
		}
		if (spawnedThisTick > 0) return false;
		boolean firstPerson = mc.gameSettings.thirdPersonView == 0;
		int interval = firstPerson ? 4 : 2;
		if (tick % interval != 0) return false;
		spawnedThisTick++;
		return true;
	}
}
