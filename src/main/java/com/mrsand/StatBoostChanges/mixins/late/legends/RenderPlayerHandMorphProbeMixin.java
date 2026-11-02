package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.HandRenderDebug;
import com.mrsand.StatBoostChanges.util.MorphHandRender;
import com.tihyo.legends.client.renders.player.RenderPlayerHand;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Confirms L8 ASM still routes empty-hand FP through RenderPlayerHand after we stop canceling RenderHandEvent.
 */
@Mixin(value = RenderPlayerHand.class, remap = false)
public abstract class RenderPlayerHandMorphProbeMixin {

	@Inject(method = "func_82441_a", at = @At("HEAD"))
	private void betterVampires$probeMorphFp(EntityPlayer player, CallbackInfo ci) {
		if (MorphHandRender.textureFor(player) == null) return;
		HandRenderDebug.logPipeline("RenderPlayerHand.func_82441_a entered (morph FP)");
	}
}
