package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(EntityBlaze.class)
public abstract class EntityBlazeMixin {

	//func_70785_a attackEntity
	@Inject(method = "func_70785_a", at = @At("HEAD"), cancellable = true)
	private void NewattackEntity(Entity entity, float distance, CallbackInfo ci){
		if(entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			if(player != null) {
				if(GeneralUtil.isVampire(player)) {
					VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
					if (GeneralUtil.hasBatBlindStealth(player)
							|| (WitcheryUtil.getVampireLevel(player) > 5 && (vPlayer.getBoolean(PlayerKeys.OTHERFORM) || LegendsUtil.mobAttack(player)))) {
						player = null;
						ci.cancel();
					}
				}
			}
		}
	}
}
