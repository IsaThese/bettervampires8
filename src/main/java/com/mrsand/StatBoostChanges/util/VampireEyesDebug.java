package com.mrsand.StatBoostChanges.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.legends.management.PlayerHelper;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Opt-in with {@code -Dstatboo.debugVampireEyes=true}.
 */
public final class VampireEyesDebug {

	private static final Logger LOG = LogManager.getLogger("StatBoo/VampireEyes");
	private static final Map<Integer, Long> LAST_REPLAY_LOG_MS = new ConcurrentHashMap<Integer, Long>();

	private VampireEyesDebug() {}

	public static boolean enabled() {
		return Boolean.getBoolean("statboo.debugVampireEyes");
	}

	public static void log(EntityPlayer player, String msg) {
		if (!enabled() || player == null) return;
		GeneralUtil.sendDebugMessage(player, "[BV Eyes] " + msg);
		logState(player, msg);
	}

	/** Log-only state snapshot (no chat spam). */
	public static void logState(EntityPlayer player, String tag) {
		if (!enabled() || player == null) return;
		String side = (player.worldObj != null && player.worldObj.isRemote) ? "C" : "S";
		VExtendedPlayer v = VExtendedPlayer.get(player);
		boolean hypno = v != null && v.getBoolean(PlayerKeys.HYPNOTIZING);
		boolean other = v != null && v.getBoolean(PlayerKeys.OTHERFORM);
		boolean aiming = PlayerHelper.isAimingRightArm(player);
		int aimType = PlayerHelper.getRightArmAimingType(player);
		boolean channeling = VampireRestoredAbilities.isHypnosisMasteryChanneling(player);
		boolean draw = VampireEyesVisual.shouldDrawOverlay(player);
		LOG.info("[{}] {} name={} id={} hypno={} other={} aiming={} aimType={} channel={} draw={} mode={} replay={}",
				side, tag, player.getCommandSenderName(), player.getEntityId(), hypno, other, aiming, aimType,
				channeling, draw, VampireEyesVisual.getMode(player), isReplayPlaying());
	}

	public static void logSyncClient(EntityPlayer player, boolean hypnoChanged, boolean hypnoNow) {
		if (!enabled() || player == null) return;
		LOG.info("[S] syncClient hypnoChanged={} hypnoNow={} name={}",
				hypnoChanged, hypnoNow, player.getCommandSenderName());
	}

	/**
	 * Once per second per player while a ReplayMod playback is active and debug is on.
	 * Called from the eye render redirect.
	 */
	public static void logReplayRender(EntityPlayer player) {
		if (!enabled() || player == null || !isReplayPlaying()) return;
		long now = System.currentTimeMillis();
		Integer id = Integer.valueOf(player.getEntityId());
		Long last = LAST_REPLAY_LOG_MS.get(id);
		if (last != null && now - last.longValue() < 1000L) return;
		LAST_REPLAY_LOG_MS.put(id, Long.valueOf(now));
		logState(player, "replay-render");
	}

	/** Safe when ReplayMod is absent. */
	public static boolean isReplayPlaying() {
		try {
			Class<?> clazz = Class.forName("com.replaymod.replay.ReplayModReplay");
			Object instance = clazz.getField("instance").get(null);
			if (instance == null) return false;
			Object handler = clazz.getMethod("getReplayHandler").invoke(instance);
			return handler != null;
		} catch (Throwable t) {
			return false;
		}
	}
}
