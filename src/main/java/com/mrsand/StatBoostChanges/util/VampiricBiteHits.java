package com.mrsand.StatBoostChanges.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Compatibility facade — bite latch lives in {@link VampireLatchHits}.
 */
public final class VampiricBiteHits {

	public static final String NBT_BITE_LATCHED = VampireLatchHits.Channel.BITE.latchedKey;
	public static final String NBT_BITE_TARGET_ID = VampireLatchHits.Channel.BITE.targetIdKey;
	public static final double ANIM_RANGE = VampireLatchHits.ANIM_RANGE;

	private VampiricBiteHits() {}

	public static EntityLivingBase getBiteTarget(EntityPlayer player) {
		return VampireLatchHits.getBiteTarget(player);
	}

	public static void clearLatchIfBiteEnded(EntityPlayer player) {
		VampireLatchHits.clearEndedChannels(player);
	}

	public static int suppressBiteMeterPulse(EntityPlayer player, int amount) {
		return VampireLatchHits.suppressBiteMeterPulse(player, amount);
	}

	public static void grantLatchWitcheryBlood(EntityPlayer player, float fraction) {
		VampireLatchHits.grantLatchWitcheryBlood(player, fraction);
	}
}
