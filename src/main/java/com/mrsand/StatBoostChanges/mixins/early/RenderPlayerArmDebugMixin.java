package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.HandRenderDebug;

import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Smoking gun for "regular hand at the end": vanilla renderFirstPersonArm while bat/wolf.
 */
@Mixin(RenderPlayer.class)
public abstract class RenderPlayerArmDebugMixin {

	// renderFirstPersonArm
	@Inject(method = "func_82441_a", at = @At("HEAD"))
	private void statboo$logVanillaFpArm(EntityPlayer player, CallbackInfo ci) {
		if (!HandRenderDebug.shouldProbe()) return;
		Class<?> self = this.getClass();
		boolean legendsHand = self.getName().contains("RenderPlayerHand");
		// Stack identifies who called FP arm (second draw of skin arm = usual culprit)
		StringBuilder stack = new StringBuilder();
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		int logged = 0;
		for (int i = 2; i < st.length && logged < 10; i++) {
			String cn = st[i].getClassName();
			if (cn.startsWith("sun.") || cn.startsWith("java.")) continue;
			stack.append(" <- ").append(HandRenderDebug.modIdForClass(safeClass(cn)))
					.append(':').append(st[i].getClassName()).append('.').append(st[i].getMethodName());
			logged++;
		}
		String msg = (legendsHand ? "LegendsHand" : "FP_ARM")
				+ " renderFirstPersonArm self=" + self.getSimpleName()
				+ " mod=" + HandRenderDebug.modIdForClass(self)
				+ stack;
		HandRenderDebug.log(msg);
		if (!legendsHand) {
			HandRenderDebug.logImportant("Vanilla FP arm while morphed — see latest.log for stack", false);
		}
	}

	private static Class<?> safeClass(String name) {
		try {
			return Class.forName(name);
		} catch (Throwable t) {
			return null;
		}
	}
}
