package com.mrsand.StatBoostChanges.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

/**
 * Opt-in hypnosis / Mesmerize gaze probe. Enable with JVM arg (not Minecraft "game" args):
 *   -Dstatboo.debugHypnosis=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 *
 * Chat only for important events (TOGGLE / DIM / announce). Full detail stays in latest.log.
 */
public final class HypnosisDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/Hypnosis");

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("statboo.debugHypnosis", "false"));
	private static boolean announced;

	private HypnosisDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("Hypnosis debug enabled via -Dstatboo.debugHypnosis=true");
		chat(player, "Hypnosis debug ON (chat=TOGGLE/DIM only; full log in latest.log)");
	}

	/** Log-only (no chat) — use for high-frequency SYNC noise. */
	public static void log(EntityPlayer player, String tag, String detail) {
		log(player, tag, detail, false);
	}

	public static void log(EntityPlayer player, String tag, String detail, boolean toChat) {
		if (!ENABLED) return;
		String line = format(player, tag, detail);
		LOG.info(line);
		if (toChat) {
			chat(player, line);
		}
	}

	public static void logToggle(EntityPlayer player, String action, boolean before, boolean after) {
		log(player, "TOGGLE", action + " " + before + "->" + after, true);
	}

	public static void logSync(EntityPlayer player, Side side, boolean hypnoAfter) {
		log(player, "SYNC-" + side, "HYPNOTIZING=" + hypnoAfter, false);
	}

	public static void logChannel(EntityPlayer player, String op, boolean was, boolean now) {
		if (was == now) return; // skip no-op end false->false spam
		log(player, "CHANNEL", op + " " + was + "->" + now, was != now);
	}

	public static void logDimension(EntityPlayer player, String op, int fromDim, int toDim) {
		log(player, "DIM", op + " " + fromDim + "->" + toDim, true);
	}

	public static String format(EntityPlayer player, String tag, String detail) {
		String side = "?";
		int dim = -999;
		boolean hypno = false;
		String tagType = "noprop";
		int ticks = -1;
		String power = "?";
		if (player != null) {
			if (player.worldObj != null) {
				side = player.worldObj.isRemote ? "C" : "S";
			}
			dim = player.dimension;
			ticks = player.ticksExisted;
			VExtendedPlayer props = VExtendedPlayer.get(player);
			if (props != null) {
				hypno = props.getBoolean(PlayerKeys.HYPNOTIZING);
				NBTBase nbtTag = props.getData() != null
						? props.getData().getTag(VExtendedPlayer.NBT_PREFIX + PlayerKeys.HYPNOTIZING.name())
						: null;
				tagType = nbtTag == null ? "absent" : nbtTag.getClass().getSimpleName();
			}
			try {
				com.emoniph.witchery.common.ExtendedPlayer ex = com.emoniph.witchery.common.ExtendedPlayer.get(player);
				if (ex != null) {
					power = String.valueOf(ex.getSelectedVampirePower());
				}
			} catch (Throwable ignored) {}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[Hypno ").append(side).append("/").append(tag).append("]");
		sb.append(" ").append(detail);
		sb.append(" | hypno=").append(hypno);
		sb.append(" tag=").append(tagType);
		sb.append(" dim=").append(dim);
		sb.append(" tick=").append(ticks);
		sb.append(" power=").append(power);
		return sb.toString();
	}

	private static void chat(EntityPlayer player, String line) {
		if (player == null) return;
		player.addChatComponentMessage(new ChatComponentText(line)
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA)));
	}
}
