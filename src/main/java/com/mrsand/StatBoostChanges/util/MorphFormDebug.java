package com.mrsand.StatBoostChanges.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

/**
 * Opt-in morph / dig-gate probe. Enable with JVM arg (not Minecraft "game" args):
 *   -Dstatboo.debugMorph=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 *
 * Dumps client+server form flags when dig/use is blocked, and a periodic snapshot while morphed.
 */
public final class MorphFormDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/MorphForm");

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("statboo.debugMorph", "false"));
	private static long lastBlockLogMs;
	private static long lastPeriodicMs;
	private static boolean announced;

	private MorphFormDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("Morph form debug enabled via -Dstatboo.debugMorph=true");
		chat(player, "Morph debug ON. Dig while stuck; watch [Morph] chat + latest.log");
	}

	/** Call when dig or item-use is denied by form gates. */
	public static void onFormGate(EntityPlayer player, String reason) {
		if (!ENABLED || player == null) return;
		long now = System.currentTimeMillis();
		if (now - lastBlockLogMs < 750L) return;
		lastBlockLogMs = now;
		dump(player, "GATE:" + reason);
	}

	/** Periodic snapshot while any form-ish state is active (client tick). */
	public static void tickPeriodic(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		announceIfNeeded(player);
		if (!interesting(player)) return;
		long now = System.currentTimeMillis();
		if (now - lastPeriodicMs < 2000L) return;
		lastPeriodicMs = now;
		dump(player, "TICK");
	}

	private static boolean interesting(EntityPlayer player) {
		if (!Loader.isModLoaded("legends")) return false;
		return MorphHelper.isMorphed(player)
				|| LegendsAbilityBridge.isMist(player)
				|| (Loader.isModLoaded("vampirism") && VampirismUtil.isBatMode(player))
				|| LegendsUtil.blocksBlockBreak(player)
				|| LegendsUtil.blocksItemUse(player);
	}

	public static void dump(EntityPlayer player, String tag) {
		String line = format(player, tag);
		LOG.info(line);
		chat(player, line);
	}

	public static String format(EntityPlayer player, String tag) {
		String side = (player.worldObj != null && player.worldObj.isRemote) ? "C" : "S";
		StringBuilder sb = new StringBuilder();
		sb.append("[Morph ").append(side).append("/").append(tag).append("]");
		if (!Loader.isModLoaded("legends")) {
			sb.append(" legends=NO");
			return sb.toString();
		}
		sb.append(" morphed=").append(MorphHelper.isMorphed(player));
		sb.append(" bat=").append(MorphHelper.isBat(player));
		sb.append(" wolf=").append(MorphHelper.isWolf(player));
		sb.append(" beast=").append(MorphHelper.isVampireBeast(player));
		sb.append(" mist=").append(LegendsAbilityBridge.isMist(player));
		try {
			String morphTag = MorphHelper.getMorphTag(player);
			sb.append(" tag=").append(morphTag == null || morphTag.isEmpty() ? "\"\"" : morphTag);
		} catch (Throwable t) {
			sb.append(" tag=?");
		}
		sb.append(" blockDig=").append(LegendsUtil.blocksBlockBreak(player));
		sb.append(" blockUse=").append(LegendsUtil.blocksItemUse(player));
		if (Loader.isModLoaded("vampirism")) {
			sb.append(" vampBatSkill=").append(VampirismUtil.isBatMode(player));
		}
		sb.append(" dim=").append(player.dimension);
		return sb.toString();
	}

	private static void chat(EntityPlayer player, String line) {
		if (player == null) return;
		player.addChatComponentMessage(new ChatComponentText(line)
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
	}
}
