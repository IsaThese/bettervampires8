package com.mrsand.StatBoostChanges.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Opt-in bat morph probe. Enable with JVM arg:
 *   -Dbettervampires.batDebug=true
 * Also arms HandRenderDebug bat FP probe when that flag is set.
 */
public final class BatFormDebug {

	public static final Logger LOG = LogManager.getLogger("BetterVampires/BatForm");

	private static final boolean ENABLED = Boolean.parseBoolean(
			System.getProperty("bettervampires.batDebug", "false"));

	private BatFormDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void log(EntityPlayer player, String action, String detail) {
		if (!ENABLED || player == null) return;
		String side = (player.worldObj != null && player.worldObj.isRemote) ? "C" : "S";
		StringBuilder sb = new StringBuilder();
		sb.append("[Bat ").append(side).append("/").append(action).append("]");
		if (Loader.isModLoaded("legends")) {
			sb.append(" isBat=").append(MorphHelper.isBat(player));
			sb.append(" morphed=").append(MorphHelper.isMorphed(player));
		}
		sb.append(" dayBlocked=").append(VampireSunRules.batFormBlocked(player));
		sb.append(" daytime=").append(VampireSunRules.isDaytime(player));
		sb.append(" sunSafe=").append(VampireSunRules.isInSunSafeBiome(player));
		if (detail != null && !detail.isEmpty()) {
			sb.append(" ").append(detail);
		}
		sb.append(" via=").append(callerHint());
		LOG.info(sb.toString());
	}

	private static String callerHint() {
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		// 0=getStackTrace, 1=callerHint, 2=log, 3=direct caller, 4+=upstream
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (int i = 3; i < st.length && n < 6; i++) {
			String cn = st[i].getClassName();
			if (cn.startsWith("java.") || cn.startsWith("sun.")) continue;
			if (cn.contains("BatFormDebug")) continue;
			if (n > 0) sb.append(" <- ");
			String simple = cn.substring(cn.lastIndexOf('.') + 1);
			sb.append(simple).append(".").append(st[i].getMethodName())
					.append(":").append(st[i].getLineNumber());
			n++;
		}
		return sb.toString();
	}
}
