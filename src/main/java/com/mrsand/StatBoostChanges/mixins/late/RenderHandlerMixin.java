package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;

import de.teamlapen.vampirism.client.render.RenderHandler;

@Mixin(value = RenderHandler.class, remap = false)
public abstract class RenderHandlerMixin {
	
//	@Inject(method = "onRenderPlayer", at = @At("HEAD"), cancellable = true)
//	public void onRenderPlayerCancel(RenderPlayerEvent.Pre event, CallbackInfo ci){
//		ci.cancel();
//	}
//	
//	@Inject(method = "onRenderHand", at = @At("HEAD"), cancellable = true)
//	public void onRenderHandCancel(RenderHandEvent event, CallbackInfo ci){
//		ci.cancel();
//	}
//	
//	@Inject(method = "renderTick", at = @At("HEAD"), cancellable = true)
//	public void renderTickCancel(TickEvent.RenderTickEvent event, CallbackInfo ci) {
//		ci.cancel();
//	}

}
