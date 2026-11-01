package com.mrsand.StatBoostChanges.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.tihyo.legends.legacy.LegacyHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWeb;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

/**
 * Opt-in cobweb intangibility probe. Enable with JVM arg:
 *   -Dstatboo.debugWeb=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 */
public final class WebPassDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/WebPass");

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("statboo.debugWeb", "false"));
	private static boolean announced;
	private static Field isInWebField;
	private static boolean isInWebResolved;

	private WebPassDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("Web pass debug enabled via -Dstatboo.debugWeb=true");
		chat(player, "Web debug ON. Stand in cobwebs; watch [Web] chat + latest.log");
	}

	/** Sample every ~10 ticks while in/near webs, or when gates flip. */
	public static void sample(EntityPlayer player, String tag) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		announceIfNeeded(player);
		boolean inWebBlock = isIntersectingWeb(player);
		boolean inWebFlag = readIsInWeb(player);
		if (!inWebBlock && !inWebFlag && player.ticksExisted % 40 != 0) return;
		if ((inWebBlock || inWebFlag) && player.ticksExisted % 10 != 0) return;
		String line = format(player, tag, inWebBlock, inWebFlag);
		LOG.info(line);
		if (player.worldObj.isRemote) {
			chat(player, line);
		}
	}

	public static String format(EntityPlayer player, String tag, boolean inWebBlock, boolean inWebFlag) {
		String side = player.worldObj.isRemote ? "C" : "S";
		StringBuilder sb = new StringBuilder();
		sb.append("[Web ").append(side).append("/").append(tag).append("]");
		sb.append(" lord=").append(DaywalkerControl.isVampireLord(player));
		sb.append(" hasInt=").append(VampireRestoredAbilities.hasIntangibility(player));
		sb.append(" weak=").append(LegendsUtil.getWeakness(player));
		sb.append(" pass=").append(VampireRestoredAbilities.shouldPassThroughWebs(player));
		sb.append(" webBlk=").append(inWebBlock);
		sb.append(" isInWeb=").append(inWebFlag);
		if (Loader.isModLoaded("legends")) {
			try {
				sb.append(" learned=")
						.append(LegacyHelper.hasLearnedPower(player, BvVampirePowers.intangibility));
			} catch (Throwable t) {
				sb.append(" learned=?");
			}
		}
		sb.append(" mot=")
				.append(round(player.motionX)).append(',')
				.append(round(player.motionY)).append(',')
				.append(round(player.motionZ));
		return sb.toString();
	}

	public static boolean isIntersectingWeb(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.boundingBox == null) return false;
		int minX = MathHelper.floor_double(player.boundingBox.minX);
		int maxX = MathHelper.floor_double(player.boundingBox.maxX);
		int minY = MathHelper.floor_double(player.boundingBox.minY);
		int maxY = MathHelper.floor_double(player.boundingBox.maxY);
		int minZ = MathHelper.floor_double(player.boundingBox.minZ);
		int maxZ = MathHelper.floor_double(player.boundingBox.maxZ);
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = player.worldObj.getBlock(x, y, z);
					if (block instanceof BlockWeb) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean readIsInWeb(Entity entity) {
		Field f = resolveIsInWeb();
		if (f == null || entity == null) return false;
		try {
			return f.getBoolean(entity);
		} catch (Throwable t) {
			return false;
		}
	}

	public static void clearIsInWeb(Entity entity) {
		Field f = resolveIsInWeb();
		if (f == null || entity == null) return;
		try {
			f.setBoolean(entity, false);
		} catch (Throwable ignored) {
		}
	}

	private static Field resolveIsInWeb() {
		if (isInWebResolved) return isInWebField;
		isInWebResolved = true;
		try {
			isInWebField = Entity.class.getDeclaredField("isInWeb");
			isInWebField.setAccessible(true);
			return isInWebField;
		} catch (Throwable ignored) {
		}
		// SRG (Forge runtime)
		try {
			isInWebField = Entity.class.getDeclaredField("field_70134_J");
			isInWebField.setAccessible(true);
			return isInWebField;
		} catch (Throwable ignored) {
		}
		LOG.warn("Could not resolve Entity.isInWeb field for web pass / debug");
		return null;
	}

	private static String round(double v) {
		return String.format("%.2f", Double.valueOf(v));
	}

	private static void chat(EntityPlayer player, String line) {
		if (player == null) return;
		player.addChatComponentMessage(new ChatComponentText(line)
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE)));
	}
}
