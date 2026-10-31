package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.MorphHandRender;
import com.tihyo.legends.morph.MorphWolf;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = MorphWolf.class, remap = false)
public class MorphWolfMixin {

	/**
	 * Legends already applied ItemRenderer FP transforms; only redraw the arm with wolf texture.
	 */
	@Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
	private void renderRightHand(Minecraft mc, EntityPlayer player, CallbackInfo ci) {
		com.mrsand.StatBoostChanges.util.HandRenderDebug.logPipeline("MorphWolf.renderRightHand -> wolf texture arm");
		MorphHandRender.renderRightArm(mc, player, MorphHandRender.WOLF_HAND);
		ci.cancel();
	}

}
