//package com.mrsand.StatBoostChanges.mixins.late;
//
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//import org.spongepowered.asm.mixin.injection.At;
//
//import com.emoniph.witchery.item.ItemGlassGoblet.BloodSource;
//import com.emoniph.witchery.ritual.Circle;
//
//@Mixin(BloodSource.class)
//public abstract class BloodSourceMixin {
//	
//	
//	@Inject(method = "isOneOf", at = @At("HEAD"), cancellable = true )
//    public static void isOneOfMyBlood(String key, CallbackInfoReturnable<Boolean> ci) {
//	      if(key.equals("__Human") || key.equals("__Animal")) {
//	      		ci.setReturnValue(true);
//	      		ci.cancel();
//	    	  
//	      }
//	    }
//
//}
