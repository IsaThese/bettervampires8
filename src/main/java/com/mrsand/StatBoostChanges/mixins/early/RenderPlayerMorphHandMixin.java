package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.MorphHandRender;

import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Legends' RenderPlayerHand / Morph.renderRightHand path often never runs in this pack;
 * ItemRenderer still calls RenderPlayer.renderFirstPersonArm with the vanilla skin.
 * Replace that draw for bat/wolf morphs.
 */
@Mixin(RenderPlayer.class)
public abstract class RenderPlayerMorphHandMixin {

	// renderFirstPersonArm
	@Inject(method = "func_82441_a", at = @At("HEAD"), cancellable = true)
	private void statboo$morphFpArm(EntityPlayer player, CallbackInfo ci) {
		if (MorphHandRender.tryReplaceFirstPersonArm(player, (RenderPlayer) (Object) this)) {
			ci.cancel();
		}
	}
}
