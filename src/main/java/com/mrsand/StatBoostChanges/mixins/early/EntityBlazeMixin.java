package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.VampireHostility;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(EntityBlaze.class)
public abstract class EntityBlazeMixin {

	@Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
	private void NewattackEntity(Entity entity, float distance, CallbackInfo ci) {
		if (!(entity instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) entity;
		EntityBlaze self = (EntityBlaze) (Object) this;
		if (VampireHostility.shouldIgnoreAttacker(self, player)) {
			ci.cancel();
		}
	}
}
