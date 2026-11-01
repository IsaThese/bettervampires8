package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.tihyo.legends.legacy.LegendsLegacy;
import com.tihyo.legends.packets.LegendsAbilitiesPacket;

import net.minecraft.entity.player.EntityPlayer;

/**
 * LegendsAbilitiesPacket skips <em>all</em> keyPressed when getWeaknesses is true.
 * Fangs / vampire bite (and mist under roof) must still fire in day/sun like original BV.
 */
@Mixin(value = LegendsAbilitiesPacket.class, remap = false)
public abstract class LegendsAbilitiesPacketMixin {

	@Shadow private int keyTag;

	@Redirect(
			method = "receive",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/legacy/LegendsLegacy;getWeaknesses(Lnet/minecraft/entity/player/EntityPlayer;)Z"
			)
	)
	private boolean bvSunSafeAbilityKeys(LegendsLegacy legacy, EntityPlayer player) {
		boolean weak = legacy != null && legacy.getWeaknesses(player);
		if (!weak) return false;
		if (VampireSunRules.allowsLegendsKeyDuringWeakness(player, this.keyTag)) {
			return false;
		}
		return true;
	}
}
