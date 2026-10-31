package com.mrsand.StatBoostChanges.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@Mixin(EntityGhast.class)
public abstract class EntityGhastMixin {

	// Update Entity Action State (func_70626_be) -> getClosestVulnerablePlayerToEntity (func_72856_b)
	@Redirect(method = "func_70626_be", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;func_72856_b(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/EntityPlayer;"))
	private EntityPlayer stopGhastTargeting(World world, Entity ghast, double distance) {
		EntityPlayer player = world.getClosestVulnerablePlayerToEntity(ghast, distance);
		if (player != null) {
			if (GeneralUtil.isVampire(player)) {
				VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
				if (vPlayer.getBoolean(PlayerKeys.OTHERFORM) || LegendsUtil.mobAttack(player)
						|| GeneralUtil.hasBatBlindStealth(player)) {
					return null;
				}
			}
		}
		return player;
	}

}
