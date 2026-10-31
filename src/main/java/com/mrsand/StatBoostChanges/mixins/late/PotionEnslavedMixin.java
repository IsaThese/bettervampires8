//package com.mrsand.StatBoostChanges.mixins.late;
//
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//import org.spongepowered.asm.mixin.injection.At;
//
//import com.emoniph.witchery.brewing.potions.PotionEnslaved;
//
//import net.minecraft.entity.EntityLiving;
//import net.minecraft.entity.EntityLivingBase;
//
//@Mixin(PotionEnslaved.class)
//public abstract class PotionEnslavedMixin {
//	
//	//trying to add Mesmerize to all monsters, but not understanding why not working.
//	
//	@Inject(method = "canCreatureBeEnslaved", at = @At("HEAD"), cancellable = true) 
//	private static void allowMobs(EntityLivingBase entityLiving, CallbackInfoReturnable<Boolean> cis){
//	    if (entityLiving instanceof net.minecraft.entity.monster.IMob) {
//	    	cis.setReturnValue(true);
//	    	cis.cancel();
//	    }
//			    
//	}
//
//}
//
