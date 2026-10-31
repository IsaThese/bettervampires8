package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.WitcheryHandRenderDebug;

import net.minecraft.client.renderer.ItemRenderer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererWitcheryHandDebugMixin {

	@Inject(method = "func_78440_a", at = @At("HEAD"))
	private void betterVampires$logWitcheryItemRenderer(float partialTicks, CallbackInfo ci) {
		if (!WitcheryHandRenderDebug.enabled()) return;
		WitcheryHandRenderDebug.onItemRenderer();
	}
}
