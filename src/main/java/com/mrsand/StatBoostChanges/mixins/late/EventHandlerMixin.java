package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import morph.common.core.EventHandler;
import net.minecraftforge.client.event.RenderPlayerEvent;

@Mixin(value = EventHandler.class, remap = false)
public abstract class EventHandlerMixin {
	
	@Inject(method = "onRenderPlayer", at = @At("HEAD"), cancellable = true)
    private void onRenderPlayer(RenderPlayerEvent.Pre event) {
		
	}

}
