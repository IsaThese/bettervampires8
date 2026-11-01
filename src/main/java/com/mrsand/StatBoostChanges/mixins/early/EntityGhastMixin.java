package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.util.VampireHostility;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@Mixin(EntityGhast.class)
public abstract class EntityGhastMixin {

	@Redirect(
			method = "updateEntityActionState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/World;getClosestVulnerablePlayerToEntity(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/EntityPlayer;"
			)
	)
	private EntityPlayer stopGhastTargeting(World world, Entity ghast, double distance) {
		EntityPlayer player = world.getClosestVulnerablePlayerToEntity(ghast, distance);
		if (player != null && VampireHostility.shouldIgnoreAttacker(ghast, player)) {
			return null;
		}
		return player;
	}
}
