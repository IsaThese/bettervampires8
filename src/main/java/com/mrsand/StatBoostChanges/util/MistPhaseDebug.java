package com.mrsand.StatBoostChanges.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

/**
 * Opt-in mist noClip / Legends-clear probe. Enable with JVM arg:
 *   -Dstatboo.debugMist=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 */
public final class MistPhaseDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/MistPhase");

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("statboo.debugMist", "false"));
	private static boolean announced;
	private static boolean lastNoClip;
	private static boolean hasLastNoClip;

	private MistPhaseDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("Mist phase debug enabled via -Dstatboo.debugMist=true");
		chat(player, "Mist debug ON. Enter mist; watch [Mist] chat + latest.log");
	}

	/** Sample while misted, every ~10 ticks, or whenever noClip flips. */
	public static void sample(EntityPlayer player, String tag) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		announceIfNeeded(player);
		boolean mist = Loader.isModLoaded("legends") && LegendsAbilityBridge.isMist(player);
		boolean flipped = hasLastNoClip && lastNoClip != player.noClip;
		if (!mist && !flipped) return;
		if (mist && !flipped && player.ticksExisted % 10 != 0) return;
		lastNoClip = player.noClip;
		hasLastNoClip = true;
		String line = format(player, tag);
		LOG.info(line);
		if (player.worldObj.isRemote) {
			chat(player, line);
		}
	}

	public static String format(EntityPlayer player, String tag) {
		String side = player.worldObj.isRemote ? "C" : "S";
		StringBuilder sb = new StringBuilder();
		sb.append("[Mist ").append(side).append("/").append(tag).append("]");
		if (!Loader.isModLoaded("legends")) {
			sb.append(" legends=NO");
			return sb.toString();
		}
		int mistTicks = LegendsAbilityBridge.getIntProp(player, LegendsProperties.MIST_FORM);
		sb.append(" mist=").append(mistTicks > 0).append("(").append(mistTicks).append(")");
		sb.append(" noClip=").append(player.noClip);
		try {
			sb.append(" isPhasing=").append(PlayerHelper.isPhasing(player));
			sb.append(" isIntangible=").append(PlayerHelper.isIntangible(player));
			sb.append(" canPhase=").append(PlayerHelper.canPhase(player));
		} catch (Throwable t) {
			sb.append(" legendsFlags=?");
		}
		sb.append(" insideOpaque=").append(player.isEntityInsideOpaqueBlock());
		sb.append(" collH=").append(player.isCollidedHorizontally);
		sb.append(" mot=")
				.append(round(player.motionX)).append(',')
				.append(round(player.motionY)).append(',')
				.append(round(player.motionZ));
		return sb.toString();
	}

	private static String round(double v) {
		return String.format("%.2f", Double.valueOf(v));
	}

	private static void chat(EntityPlayer player, String line) {
		if (player == null) return;
		player.addChatComponentMessage(new ChatComponentText(line)
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA)));
	}
}
