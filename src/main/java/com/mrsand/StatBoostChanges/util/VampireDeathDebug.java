package com.mrsand.StatBoostChanges.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.emoniph.witchery.common.ExtendedPlayer;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;

/**
 * Opt-in probe for vampire death-save vs client death-screen desync.
 * Enable with JVM arg (not Minecraft "game" args):
 *   -Dstatboo.debugVampireDeath=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 *
 * Expect: server SAVE/CANCEL with hp restored while client may log TICK_DESYNC
 * (GuiGameOver up, hp still &gt; 0).
 */
public final class VampireDeathDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/VampireDeath");

	private static final boolean ENABLED = Boolean.parseBoolean(
			System.getProperty("statboo.debugVampireDeath", "false"));
	private static final int DESYNC_CHAT_INTERVAL = 20;
	private static boolean announced;

	private VampireDeathDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("VampireDeath debug enabled via -Dstatboo.debugVampireDeath=true");
		chat(player, "VampireDeath debug ON (desync/SAVE in latest.log)");
	}

	public static void log(EntityPlayer player, String note, DamageSource source) {
		if (!ENABLED || player == null) return;
		LOG.info(format(player, note, source));
	}

	public static void log(EntityPlayer player, String note) {
		log(player, note, null);
	}

	/** Sparse chat + log for important SAVE/desync events. */
	public static void logImportant(EntityPlayer player, String note, DamageSource source) {
		if (!ENABLED || player == null) return;
		String line = format(player, note, source);
		LOG.info(line);
		if (player.worldObj != null && player.worldObj.isRemote) {
			chat(player, "[" + note + "] hp=" + String.format("%.1f", player.getHealth())
					+ " dead=" + player.isDead + " deathT=" + player.deathTime);
		}
	}

	/**
	 * Client END tick: death GUI or isDead/deathTime while hp &gt; 0.
	 * Throttled to avoid multi-hit spam.
	 */
	public static void tickClient(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		announceIfNeeded(player);
		if (!GeneralUtil.isVampire(player)) return;

		boolean gameOver = isGameOverScreen();
		boolean hpAlive = player.getHealth() > 0.0F;
		boolean flagDead = player.isDead || player.deathTime > 0;
		boolean desync = (gameOver && hpAlive) || (flagDead && hpAlive);
		if (!desync) return;
		// Throttle multi-hit spam; chat only every DESYNC_CHAT_INTERVAL.
		if (player.ticksExisted % 10 != 0) return;
		String note = "TICK_DESYNC guiOver=" + gameOver;
		if (player.ticksExisted % DESYNC_CHAT_INTERVAL == 0) {
			logImportant(player, note, null);
		} else {
			log(player, note, null);
		}
	}

	private static boolean isGameOverScreen() {
		try {
			Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
			Object mc = mcClass.getMethod("func_71410_x").invoke(null);
			if (mc == null) {
				mc = mcClass.getMethod("getMinecraft").invoke(null);
			}
			Object screen = mcClass.getField("field_71462_r").get(mc);
			if (screen == null) {
				try {
					screen = mcClass.getField("currentScreen").get(mc);
				} catch (Throwable ignored) {
					return false;
				}
			}
			if (screen == null) return false;
			String name = screen.getClass().getName();
			return name.contains("GuiGameOver");
		} catch (Throwable t) {
			return false;
		}
	}

	private static String format(EntityPlayer player, String note, DamageSource source) {
		String side = (player.worldObj != null && player.worldObj.isRemote) ? "C" : "S";
		int blood = -1;
		int level = -1;
		if (Loader.isModLoaded("witchery")) {
			try {
				ExtendedPlayer ex = ExtendedPlayer.get(player);
				if (ex != null) {
					blood = ex.getBloodPower();
					level = ex.getVampireLevel();
				}
			} catch (Throwable ignored) {
			}
		}
		String src = source == null ? "-" : String.valueOf(source.getDamageType());
		return String.format(
				"[%s/%s] name=%s tick=%d hp=%.2f/%.2f isDead=%s deathT=%d hurtRes=%d blood=%d lvl=%d src=%s",
				side, note, player.getCommandSenderName(), player.ticksExisted,
				player.getHealth(), player.getMaxHealth(),
				Boolean.valueOf(player.isDead), player.deathTime, player.hurtResistantTime,
				blood, level, src);
	}

	private static void chat(EntityPlayer player, String msg) {
		player.addChatMessage(new ChatComponentText(msg)
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE)));
	}
}
