package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//import mca.core.forge.EventHooksForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
//
//@Mixin(value = EventHooksForge.class, remap = false)
//public class MCAEventHooksForgeMixin {
//
//    @Inject(method = "entityJoinedWorldEventHandler", at = @At("HEAD"), cancellable = true)
//    private void onWizardJoinWorld(EntityJoinWorldEvent event, CallbackInfo ci) {
//        if (event.entity == null) return;
//
//        // Fetch the full package + class path of the entity joining the world
//        String className = event.entity.getClass().getName();
//
//        // Blanket protection: Catch EntityWizard, EntityEvilWizard, and any other wizard entities
//        if (className.startsWith("electroblob.wizardry.entity.living.Entity")) {
//            
//            // Instantly exit MCA's code, preventing them from being deleted or swapped out
//            ci.cancel(); 
//        }
//    }
//}
