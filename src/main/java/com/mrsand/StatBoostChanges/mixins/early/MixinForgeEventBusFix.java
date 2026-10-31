package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import net.minecraftforge.event.entity.EntityEvent;

@Mixin(value = EventBus.class, remap = false)
public class MixinForgeEventBusFix {

    @Inject(method = "post", at = @At("HEAD"), cancellable = true)
    public void onPostEvent(Event event, CallbackInfoReturnable<Boolean> cir) {
        // Target the exact event that is breaking across all mods
        if (event instanceof EntityEvent.EntityConstructing) {
            EntityEvent.EntityConstructing constructEvent = (EntityEvent.EntityConstructing) event;
            
            // If the entity doesn't have a valid world object yet because 
            // Serene Seasons + RTG are ticking too early, kill the event execution.
            if (constructEvent.entity == null || constructEvent.entity.worldObj == null) {
                // By canceling the post, mods never receive the broken entity,
                // preventing the infinite NullPointerException loop entirely.
                cir.setReturnValue(false); 
            }
        }
    }
}
