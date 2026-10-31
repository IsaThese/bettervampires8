package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.MorphHandRender;
import com.tihyo.legends.morph.MorphBat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = MorphBat.class, remap = false)
public class MorphBatMixin {

	/**
	 * Legends already applied ItemRenderer FP transforms; only redraw the arm with bat texture.
	 */
	@Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
	private void renderRightHand(Minecraft mc, EntityPlayer player, CallbackInfo ci) {
		com.mrsand.StatBoostChanges.util.HandRenderDebug.logPipeline("MorphBat.renderRightHand -> bat texture arm");
		MorphHandRender.renderRightArm(mc, player, MorphHandRender.BAT_HAND);
		ci.cancel();
	}

}
