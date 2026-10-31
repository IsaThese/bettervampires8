package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.WitcheryHandRenderDebug;
import com.tihyo.legends.client.renders.player.RenderPlayerHand;
import com.tihyo.legends.management.LegendsRenderHelper;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Logs at Legends' real FP-hand entry (overrides RenderPlayer.func_82441_a).
 * Loaded only when {@code -Dstatboo.debugWitcheryHand=true} and Legends coremod is present.
 */
@Mixin(value = RenderPlayerHand.class, remap = false)
public abstract class RenderPlayerHandWitcheryDebugMixin {

	@Inject(method = "func_82441_a", at = @At("HEAD"))
	private void betterVampires$logWitcheryRenderPlayerHand(EntityPlayer player, CallbackInfo ci) {
		if (!WitcheryHandRenderDebug.enabled() || player == null) return;
		boolean shouldRender = LegendsRenderHelper.shouldRenderHand(player);
		boolean legendsRender = LegendsRenderHelper.shouldUseLegendsRender(player);
		WitcheryHandRenderDebug.onRenderPlayerHand(player, shouldRender, legendsRender);
	}
}
