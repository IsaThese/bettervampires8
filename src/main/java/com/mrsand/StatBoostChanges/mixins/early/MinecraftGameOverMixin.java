package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.packets.PacketClearDeathScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;

/**
 * After a vampire CANCEL_SAVED, block GuiGameOver reopen while suppress is active
 * (S06 health=0 can race ahead of the heal/clear packets).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftGameOverMixin {

	@Inject(method = "func_147108_a", at = @At("HEAD"), cancellable = true)
	private void bvSuppressGameOverAfterVampireSave(GuiScreen screen, CallbackInfo ci) {
		if (!(screen instanceof GuiGameOver)) return;
		boolean suppress = PacketClearDeathScreen.shouldSuppressGameOver();
		float hp = -1.0F;
		boolean dead = false;
		boolean aliveHp = false;
		try {
			Minecraft mc = (Minecraft) (Object) this;
			if (mc.thePlayer != null) {
				hp = mc.thePlayer.getHealth();
				dead = mc.thePlayer.isDead;
				aliveHp = hp > 0.0F;
			}
		} catch (Throwable ignored) {
		}
		// #region agent log
		dbgGameOver("open_attempt", suppress, hp, dead);
		// #endregion
		// Suppress window (pre-armed on hp<=0 or PacketClearDeathScreen), or already healed.
		if (suppress || aliveHp) {
			ci.cancel();
			// #region agent log
			dbgGameOver(aliveHp && !suppress ? "open_cancelled_alive" : "open_cancelled", true, hp, dead);
			// #endregion
		}
	}

	// #region agent log
	private static void dbgGameOver(String msg, boolean suppress, float hp, boolean dead) {
		try {
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(
					"c:/Users/isaac/IdeaProjects/bettervampires8/.cursor/debug-9ecb1a.log", true));
			pw.println("{\"sessionId\":\"9ecb1a\",\"runId\":\"death-gui\",\"hypothesisId\":\"H1\","
					+ "\"location\":\"MinecraftGameOverMixin\",\"message\":\"" + msg + "\","
					+ "\"timestamp\":" + System.currentTimeMillis()
					+ ",\"data\":{\"suppress\":" + suppress + ",\"hp\":" + hp
					+ ",\"dead\":" + dead + "}}");
			pw.close();
		} catch (Throwable ignored) {
		}
	}
	// #endregion
}
