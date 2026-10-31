package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.WitcheryHandRenderDebug;

import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(RenderPlayer.class)
public abstract class RenderPlayerWitcheryHandDebugMixin {

	@Inject(method = "func_82441_a", at = @At("HEAD"))
	private void betterVampires$logWitcheryFpArm(EntityPlayer player, CallbackInfo ci) {
		if (!WitcheryHandRenderDebug.enabled()) return;
		WitcheryHandRenderDebug.onFirstPersonArm(player, (RenderPlayer) (Object) this);
	}
}
