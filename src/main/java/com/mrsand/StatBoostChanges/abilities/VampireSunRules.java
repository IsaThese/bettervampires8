package com.mrsand.StatBoostChanges.abilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyPower;

import cpw.mods.fml.common.Loader;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Ability use vs sun/daytime. Separate from sun <em>burn</em> ({@link BvSunDamage} /
 * {@link DaywalkerControl#isVampireLord}).
 * <p>
 * Uses {@link GeneralUtil#isDay} / {@link GeneralUtil#isInLight} (same as old BV /
 * {@code VampirePlayer#isInSunlight}), <b>not</b> {@code World#isDaytime()} — vanilla
 * {@code isDaytime()} stays true until ~midnight, so {@code /time set night} still
 * looked like day and blocked the pie.
 */
public final class VampireSunRules {

	private static final Logger LOG = LogManager.getLogger("BetterVampires.SunRules");
	/** Set true temporarily to spam pie/canUse diagnostics into latest.log */
	public static boolean DEBUG = true;
	private static int debugThrottle;

	private VampireSunRules() {}

	public static boolean isEnabled() {
		return StatBoostConfig.removeLegacyDaywalker;
	}

	/** Open to the sky (not under a roof). Null-safe: assume covered if unknown. */
	public static boolean canSeeSky(EntityPlayer player) {
		if (player == null || player.worldObj == null) return false;
		World world = player.worldObj;
		int x = MathHelper.floor_double(player.posX);
		int y = MathHelper.floor_double(player.posY + player.getEyeHeight());
		int z = MathHelper.floor_double(player.posZ);
		return world.canBlockSeeTheSky(x, y, z);
	}

	/**
	 * Calendar day for ability bans. Sun-safe biomes (vampire forest / ominous)
	 * always count as night via {@link GeneralUtil#isDay} /
	 * {@link VampirismUtil#isInSunSafeBiome}. Null world → not day (do not block).
	 */
	public static boolean isDaytime(EntityPlayer player) {
		if (player == null || player.worldObj == null) return false;
		try {
			return GeneralUtil.isDay(player);
		} catch (Throwable t) {
			LOG.warn("[SunRules] isDaytime failed for {}: {}", safeName(player), t.toString());
			return false;
		}
	}

	public static boolean isInSunSafeBiome(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("vampirism")) return false;
		try {
			return VampirismUtil.isInSunSafeBiome(player);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * True sunlight exposure (brightness + sky). Matches pie's historic
	 * {@code VampirePlayer#isInSunlight} / {@link GeneralUtil#isInLight}.
	 */
	public static boolean isInSunlight(EntityPlayer player) {
		if (player == null || player.worldObj == null) return false;
		try {
			return GeneralUtil.isInLight(player);
		} catch (Throwable t) {
			LOG.warn("[SunRules] isInSunlight failed for {}: {}", safeName(player), t.toString());
			return false;
		}
	}

	/**
	 * General Legends/Vampirism powers may not start (and morphs/fury should strip).
	 * Lords: any calendar daytime. Non-lords: true sunlight.
	 */
	public static boolean generalPowersBlocked(EntityPlayer player) {
		if (!isEnabled() || player == null) return false;
		try {
			boolean lord = DaywalkerControl.isVampireLord(player);
			boolean blocked = lord ? isDaytime(player) : isInSunlight(player);
			debugMaybe(player, lord, blocked);
			return blocked;
		} catch (Throwable t) {
			LOG.warn("[SunRules] generalPowersBlocked NPE/error for {}: {}", safeName(player), t.toString());
			return false;
		}
	}

	/** Start mist: non-lord needs shade; lord needs night or under roof. */
	public static boolean canStartMist(EntityPlayer player) {
		if (!isEnabled() || player == null) return true;
		try {
			if (DaywalkerControl.isVampireLord(player)) {
				return !isDaytime(player) || !canSeeSky(player);
			}
			return !isInSunlight(player);
		} catch (Throwable t) {
			LOG.warn("[SunRules] canStartMist failed: {}", t.toString());
			return true;
		}
	}

	/** Change weather (rain): lords only; night or under roof by day. */
	public static boolean canStartWeather(EntityPlayer player) {
		if (!isEnabled() || player == null) return true;
		try {
			if (!DaywalkerControl.isVampireLord(player)) {
				return !isInSunlight(player);
			}
			return !isDaytime(player) || !canSeeSky(player);
		} catch (Throwable t) {
			LOG.warn("[SunRules] canStartWeather failed: {}", t.toString());
			return true;
		}
	}

	/** Auto-clear mist (non-lords in sun only). Lords keep mist until they exit. */
	public static boolean shouldForceClearMist(EntityPlayer player) {
		if (!isEnabled() || player == null) return false;
		if (DaywalkerControl.isVampireLord(player)) return false;
		return isInSunlight(player);
	}

	/**
	 * Auto-clear bat/beast/wolf/fury/vision/bloodlust.
	 * Does <em>not</em> apply to fangs or vampire bite (always usable, like original BV).
	 */
	public static boolean shouldForceClearForms(EntityPlayer player) {
		return generalPowersBlocked(player);
	}

	/**
	 * LegendsAbilitiesPacket blocks every key when {@code getWeaknesses} is true.
	 * Return true to let that key through anyway (fangs / bite / mist under roof).
	 * <p>
	 * Packet {@code keyTag} is <em>not</em> always the ability slot:
	 * 0→1, 1→2, 2→3, 3→6, 4→4, 5→5, 6→7 (see LegendsAbilitiesPacket.receive).
	 */
	public static boolean allowsLegendsKeyDuringWeakness(EntityPlayer player, int keyTag) {
		if (player == null) return false;
		try {
			// Fixed specialization slot used by LegacyPowerFangs
			if (keyTag == 13) return true;

			int slot = legendsAbilitySlotForKeyTag(keyTag);
			LegacyPower power = LegacyHelper.getAbility(player, slot);
			if (power == null && slot != keyTag) {
				power = LegacyHelper.getAbility(player, keyTag);
			}
			if (power == null) return false;
			String name = power.getName();
			if (name == null) return false;
			if ("fangs".equals(name) || "fangs2".equals(name) || "vampireBite".equals(name)) {
				if (DEBUG) {
					LOG.info("[SunRules] allow keyTag={} slot={} power={}", keyTag, slot, name);
				}
				return true;
			}
			if ("morpMist".equals(name)) {
				return canStartMist(player);
			}
			return false;
		} catch (Throwable t) {
			LOG.warn("[SunRules] allowsLegendsKeyDuringWeakness failed: {}", t.toString());
			return false;
		}
	}

	/** Map LegendsAbilitiesPacket keyTag → LegacyHelper.getAbility slot. */
	public static int legendsAbilitySlotForKeyTag(int keyTag) {
		switch (keyTag) {
			case 0: return 1;
			case 1: return 2;
			case 2: return 3;
			case 3: return 6;
			case 4: return 4;
			case 5: return 5;
			case 6: return 7;
			case 7: return 8;
			default: return keyTag;
		}
	}

	/** One-shot dump when pie builds the skill list (client). */
	public static void debugPieSnapshot(EntityPlayer player) {
		if (!DEBUG || player == null) return;
		boolean lord = false;
		try {
			lord = DaywalkerControl.isVampireLord(player);
		} catch (Throwable ignored) {}
		long time = player.worldObj != null ? (player.worldObj.getWorldTime() % 24000L) : -1L;
		boolean vanillaDay = player.worldObj != null && player.worldObj.isDaytime();
		LOG.info("[SunRules/pie] player={} side={} lord={} worldTime={} utilIsDay={} vanillaIsDaytime={} sunSafeBiome={} inLight={} seeSky={} generalBlocked={} canMist={} canWeather={}",
				safeName(player),
				player.worldObj != null && player.worldObj.isRemote ? "client" : "server",
				lord,
				time,
				isDaytime(player),
				vanillaDay,
				isInSunSafeBiome(player),
				isInSunlight(player),
				canSeeSky(player),
				generalPowersBlocked(player),
				canStartMist(player),
				canStartWeather(player));
	}

	private static void debugMaybe(EntityPlayer player, boolean lord, boolean blocked) {
		if (!DEBUG || player == null || player.worldObj == null) return;
		// Throttle: every ~5s per side while evaluating
		if ((++debugThrottle) % 100 != 0) return;
		long time = player.worldObj.getWorldTime() % 24000L;
		LOG.info("[SunRules] {} lord={} blocked={} time={} utilDay={} vanillaDay={} sunSafeBiome={} light={} sky={}",
				safeName(player), lord, blocked, time, isDaytime(player),
				player.worldObj.isDaytime(), isInSunSafeBiome(player), isInSunlight(player), canSeeSky(player));
	}

	private static String safeName(EntityPlayer player) {
		try {
			return player.getCommandSenderName();
		} catch (Throwable t) {
			return "?";
		}
	}
}
