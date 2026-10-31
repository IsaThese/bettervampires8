package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tihyo.legends.abilities.Ability;
import com.tihyo.legends.abilities.AbilityHelper;
import com.tihyo.legends.management.LegendsGameRules;
import com.tihyo.legends.management.PlayerHelper;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = PlayerHelper.class, remap = false)
public abstract class PlayerHelperMixin {

@Inject(method = "isInvincible", at = @At("HEAD"), cancellable = true)
private static void isInvincibleChange(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
	cir.cancel();
    cir.setReturnValue( (AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.invincible) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.powerWithin) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.bold) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.mistForm) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.devonianBeast) 
            //|| AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.vampireBite) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.werewolfBite) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.batonBash) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.vadersMaelstrom) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.choke) 
            //|| AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.hypnosisControl) 
            || AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.lightningShield) 
            || player.worldObj.getGameRules().getGameRuleBooleanValue(LegendsGameRules.isInvincible))); // field_70170_p.func_82736_K().func_82766_b
}
}


