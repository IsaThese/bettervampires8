package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.HandRenderDebug;

import net.minecraft.client.renderer.ItemRenderer;

/**
 * Confirms ItemRenderer.renderItemInFirstPerson still runs (RenderHandEvent not canceled).
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererHandDebugMixin {

	// renderItemInFirstPerson
	@Inject(method = "func_78440_a", at = @At("HEAD"))
	private void statboo$logFpItemRenderer(float partialTicks, CallbackInfo ci) {
		HandRenderDebug.logPipeline("ItemRenderer.renderItemInFirstPerson HEAD (Legends/vanilla FP path entered)");
	}
}
