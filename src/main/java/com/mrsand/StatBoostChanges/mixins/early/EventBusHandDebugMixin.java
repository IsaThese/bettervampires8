package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.util.HandRenderDebug;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * When -Dstatboo.debugHandRender=true and bat/wolf morphed, log each RenderHandEvent
 * listener and which one flips canceled (skips ItemRenderer / Legends hand path).
 */
@Mixin(value = EventBus.class, remap = false)
public abstract class EventBusHandDebugMixin {

	@Redirect(
			method = "post",
			at = @At(
					value = "INVOKE",
					target = "Lcpw/mods/fml/common/eventhandler/IEventListener;invoke(Lcpw/mods/fml/common/eventhandler/Event;)V"
			)
	)
	private void statboo$debugRenderHandListeners(IEventListener listener, Event event) {
		if (HandRenderDebug.enabled() && event instanceof RenderHandEvent && HandRenderDebug.shouldProbe()) {
			RenderHandEvent rhe = (RenderHandEvent) event;
			boolean before = rhe.isCanceled();
			listener.invoke(event);
			HandRenderDebug.onEventBusListener(listener, rhe, before);
			HandRenderDebug.markListenerPassDone();
			return;
		}
		listener.invoke(event);
	}
}
