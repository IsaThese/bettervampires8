package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.StatBoostChanges;
import com.mrsand.StatBoostChanges.common.packets.VisualPacketFromServer;
import com.tihyo.legends.abilities.Ability;
import com.tihyo.legends.abilities.AbilityHelper;
import com.tihyo.legends.handlers.PowersHandler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = PowersHandler.class, remap = false)
public abstract class PowersHandlerMixin {


	//If vampire from witchery disables auto night vision. OH THERE ARE TWO. This is fixed in LegacyVampire
//	@Inject(method = "nightVision(Lnet/minecraft/entity/player/EntityPlayer;Z)V", at = @At("HEAD"), cancellable = true)
//	private static void noNightVision(EntityPlayer player, boolean flag, CallbackInfo ci) {
//		if (!CreatureUtil.isVampire(player)) {
//			ci.cancel();
//		}
//	}
//
//	@Inject(method = "nightVision(Lnet/minecraft/entity/player/EntityPlayer;II)V", at = @At("HEAD"), cancellable = true)
//	private static void noNightVision2(EntityPlayer player, int duration, int amplitude, CallbackInfo ci) {
//		if (!CreatureUtil.isVampire(player)) {
//			ci.cancel();
//		}
//	}


	@Inject(method = "bloodVision", at = @At("HEAD"), cancellable = true)
	private static void bloodVisionChangeForm(EntityPlayer player, boolean flag, CallbackInfo ci) {
		ci.cancel();
		if (flag) {
			AbilityHelper.addEffectAndSync((EntityLivingBase)player, Ability.bloodVision, 1000000, 0);
			StatBoostChanges.network.sendToAll(new VisualPacketFromServer(player.getEntityId(), flag));
				
		} else if (!flag) {
			AbilityHelper.removeEffectAndSync((EntityLivingBase)player, Ability.bloodVision);
			StatBoostChanges.network.sendToAll(new VisualPacketFromServer(player.getEntityId(), flag));
		} 
	}
}




//Trying to make Hypnosismaster apply ENSLAVED potion effect for mobs.

//    @Inject(method = "mindTrick", at = @At("RETURN"), cancellable = true)
//    private static void onMindTrick(EntityPlayer player, int duration, CallbackInfo ci) {
//        // Get the targeted entity by the player within 25 blocks
//    	
//        EntityLivingBase target = LegendsHelper.getTarget(player, 25.0D);
//
//        if (target != null) {
//            // This doesn't work for some reason
//            target.addPotionEffect(new PotionEffect(Witchery.Potions.ENSLAVED.id, TimeUtil.secsToTicks(120))); 
//            
//        }
//    }
//}



//AbilityHelper.addEffect(entity, Witchery.Potions.PARALYSED.field_76415_H, duration, amplifier);
