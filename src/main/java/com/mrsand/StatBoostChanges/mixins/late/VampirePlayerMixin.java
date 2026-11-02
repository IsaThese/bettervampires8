package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.util.LegendsUtil;

import de.teamlapen.vampirism.entity.player.VampirePlayer;
import net.minecraft.entity.player.EntityPlayer;

/**
 * When Vampirism levels a player to vampire, also assign the Legends vampire legacy.
 */
@Mixin(value = VampirePlayer.class, remap = false)
public abstract class VampirePlayerMixin {

	@Shadow
	private EntityPlayer player;

	@Shadow
	public abstract int getLevel();

	@Inject(method = "levelUp", at = @At("RETURN"))
	private void bvEnsureLegendsVampireOnLevelUp(CallbackInfo ci) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (getLevel() > 0) {
			LegendsUtil.ensureVampireLegacy(player);
		}
	}
}
