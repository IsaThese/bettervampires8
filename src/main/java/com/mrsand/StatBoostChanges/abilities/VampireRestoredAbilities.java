package com.mrsand.StatBoostChanges.abilities;

import java.util.List;

import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.properties.PropertyManager;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWeb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Auto-granted BetterVampires restorations for powers removed in Legends 8.
 */
public final class VampireRestoredAbilities {

	public static final int DEFAULT_HYPNOSIS_STRENGTH = 100;
	public static final int DEFAULT_FURY_SECONDS = 15;
	private static final String NBT_WEB_PHASE = "bvWebPhase";

	private VampireRestoredAbilities() {}

	public static boolean isVampire(EntityPlayer player) {
		return player != null && LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire;
	}

	/** Ensure unlock flag, learn restored powers, and hypnosis strength baseline. */
	public static void ensureAutoGranted(EntityPlayer player) {
		if (!isVampire(player) || player.worldObj.isRemote) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;

		boolean changed = false;
		if (!v.getBoolean(PlayerKeys.UNLOCKED_RESTORED_POWERS)) {
			v.setBoolean(PlayerKeys.UNLOCKED_RESTORED_POWERS, true);
			changed = true;
		}
		if (v.getInteger(PlayerKeys.HYPNOSIS_STRENGTH) <= 0) {
			v.setInteger(PlayerKeys.HYPNOSIS_STRENGTH, DEFAULT_HYPNOSIS_STRENGTH);
			changed = true;
		}

		// Mirror learnAllPowers: mutate learned list + PropertyManager sync.
		// Do NOT use LegacyHelper.learnPower — that sends PurchasePowerPacket (client purchase path).
		List<LegacyPower> learned = LegacyHelper.getLearnedPowers(player);
		boolean powersChanged = false;
		for (LegacyPower power : BvVampirePowers.all()) {
			if (!LegacyHelper.hasLearnedPower(player, power)) {
				learned.add(power);
				powersChanged = true;
				changed = true;
			}
		}
		if (powersChanged) {
			PropertyManager.setUnlockedPowers(player, learned);
		}

		if (changed) {
			v.syncClient();
		}
	}

	public static boolean hasRestoredPowers(EntityPlayer player) {
		if (!isVampire(player)) return false;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		return v != null && v.getBoolean(PlayerKeys.UNLOCKED_RESTORED_POWERS);
	}

	public static boolean hasDemonicForm(EntityPlayer player) {
		return hasRestoredPowers(player)
				|| LegacyHelper.hasLearnedPower(player, BvVampirePowers.demonicForm);
	}

	public static boolean hasHypnosisMastery(EntityPlayer player) {
		return hasRestoredPowers(player)
				|| LegacyHelper.hasLearnedPower(player, BvVampirePowers.hypnosisMastery);
	}

	public static boolean hasIntangibility(EntityPlayer player) {
		return hasRestoredPowers(player);
	}

	public static boolean hasDemonicFury(EntityPlayer player) {
		return hasRestoredPowers(player)
				|| LegacyHelper.hasLearnedPower(player, BvVampirePowers.demonicFury);
	}

	public static int getHypnosisStrength(EntityPlayer player) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		return v == null ? 0 : v.getInteger(PlayerKeys.HYPNOSIS_STRENGTH);
	}

	public static void setHypnosisStrength(EntityPlayer player, int amount) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null || player.worldObj.isRemote) return;
		v.setInteger(PlayerKeys.HYPNOSIS_STRENGTH, LegendsHelper.limit(amount, 0, DEFAULT_HYPNOSIS_STRENGTH));
		v.syncClient();
	}

	public static void addHypnosisStrength(EntityPlayer player, int delta) {
		setHypnosisStrength(player, getHypnosisStrength(player) + delta);
	}

	/** True while Hypnosis Mastery hold duration is active. */
	public static boolean isHypnosisMasteryChanneling(EntityPlayer player) {
		if (player == null || !hasHypnosisMastery(player)) return false;
		int key = BvVampirePowers.hypnosisMastery.getAssignedKey(player);
		return key > 0 && LegacyHelper.getDuration(player, key) > 0;
	}

	public static boolean isDemonicFuryActive(EntityPlayer player) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		return v != null && v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS) > 0;
	}

	public static void activateDemonicFury(EntityPlayer player, int seconds) {
		if (player == null || player.worldObj.isRemote || !hasDemonicFury(player)) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;
		int ticks = Math.max(1, seconds) * 20;
		v.setInteger(PlayerKeys.DEMONIC_FURY_TICKS, Math.max(v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS), ticks));
		PowersHandler.fury(player, seconds);
		v.syncClient();
	}

	public static void tick(EntityPlayer player) {
		if (player == null || player.worldObj.isRemote) return;
		ensureAutoGranted(player);
		if (!hasRestoredPowers(player)) return;

		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;

		int fury = v.getInteger(PlayerKeys.DEMONIC_FURY_TICKS);
		if (fury > 0) {
			v.setInteger(PlayerKeys.DEMONIC_FURY_TICKS, fury - 1);
			if (fury - 1 == 0) {
				v.syncClient();
			}
		}

		tickHypnosisStrengthRegen(player);
		tickWebPassThrough(player);
	}

	/** Legends 5.4: +1 hypnotic strength every 5 ticks while not controlling. */
	private static void tickHypnosisStrengthRegen(EntityPlayer player) {
		if (!hasHypnosisMastery(player)) return;
		if (isHypnosisMasteryChanneling(player)) return;
		if (getHypnosisStrength(player) >= DEFAULT_HYPNOSIS_STRENGTH) return;
		if (LegendsHelper.getTicks(player) % 5 != 0) return;
		addHypnosisStrength(player, 1);
	}

	/** Passive intangibility: no collision with cobwebs only. */
	private static void tickWebPassThrough(EntityPlayer player) {
		if (!hasIntangibility(player) || LegendsUtil.getWeakness(player)) {
			clearWebPhase(player);
			return;
		}
		// Mist uses its own noClip; don't fight it.
		if (LegendsAbilityBridge.isMist(player)) {
			clearWebPhase(player);
			return;
		}

		boolean inWeb = isIntersectingWeb(player);
		if (inWeb) {
			player.noClip = true;
			player.getEntityData().setBoolean(NBT_WEB_PHASE, true);
		} else {
			clearWebPhase(player);
		}
	}

	private static void clearWebPhase(EntityPlayer player) {
		if (!player.getEntityData().getBoolean(NBT_WEB_PHASE)) return;
		player.getEntityData().setBoolean(NBT_WEB_PHASE, false);
		if (!player.capabilities.isCreativeMode && !LegendsAbilityBridge.isMist(player)) {
			player.noClip = false;
		}
	}

	private static boolean isIntersectingWeb(EntityPlayer player) {
		World world = player.worldObj;
		int minX = MathHelper.floor_double(player.boundingBox.minX);
		int maxX = MathHelper.floor_double(player.boundingBox.maxX);
		int minY = MathHelper.floor_double(player.boundingBox.minY);
		int maxY = MathHelper.floor_double(player.boundingBox.maxY);
		int minZ = MathHelper.floor_double(player.boundingBox.minZ);
		int maxZ = MathHelper.floor_double(player.boundingBox.maxZ);

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlock(x, y, z);
					if (block instanceof BlockWeb) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
