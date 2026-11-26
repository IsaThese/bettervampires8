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
 * Also clears a stuck GuiGameOver after CANCEL_SAVED when hp is already restored
 * (fallback alongside {@code PacketClearDeathScreen}).
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
		if (!enabled() || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("VampireDeath debug ON (-Dstatboo.debugVampireDeath)");
		chat(player, "VampireDeath debug ON (desync/SAVE in latest.log)");
	}

	public static void log(EntityPlayer player, String note, DamageSource source) {
		if (!enabled() || player == null) return;
		LOG.info(format(player, note, source));
	}

	public static void log(EntityPlayer player, String note) {
		log(player, note, null);
	}

	/** Sparse chat + log for important SAVE/desync events. */
	public static void logImportant(EntityPlayer player, String note, DamageSource source) {
		if (!enabled() || player == null) return;
		String line = format(player, note, source);
		LOG.info(line);
		if (player.worldObj != null && player.worldObj.isRemote) {
			chat(player, "[" + note + "] hp=" + String.format("%.1f", player.getHealth())
					+ " dead=" + player.isDead + " deathT=" + player.deathTime);
		}
	}

	public static void logData(EntityPlayer player, String note, DamageSource source, String hypothesisId, String extraJson) {
		if (!enabled() || player == null) return;
		LOG.info(format(player, note, source) + " [" + hypothesisId + "] extra=" + extraJson);
	}

	/**
	 * Client END tick: clear stuck GuiGameOver after vampire CANCEL_SAVED.
	 * Desync chat only when debug is enabled.
	 */
	public static void tickClient(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (!GeneralUtil.isVampire(player)) return;

		boolean gameOver = isGameOverScreen();
		boolean hpAlive = player.getHealth() > 0.0F;
		boolean flagDead = player.isDead || player.deathTime > 0;
		if (gameOver && hpAlive) {
			// #region agent log
			try {
				java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(
						"c:/Users/isaac/IdeaProjects/bettervampires8/.cursor/debug-9ecb1a.log", true));
				pw.println("{\"sessionId\":\"9ecb1a\",\"runId\":\"death-gui\",\"hypothesisId\":\"H3\","
						+ "\"location\":\"VampireDeathDebug.tickClient\",\"message\":\"tick_clear_desync_gui\","
						+ "\"timestamp\":" + System.currentTimeMillis()
						+ ",\"data\":{\"hp\":" + player.getHealth() + ",\"dead\":" + player.isDead
						+ ",\"deathT\":" + player.deathTime + "}}");
				pw.close();
			} catch (Throwable ignored) {
			}
			// #endregion
			clearGameOverScreen(player);
			if (enabled()) {
				logImportant(player, "CLEARED_DESYNC_GUI", null);
			}
			return;
		}
		if (!enabled()) return;
		announceIfNeeded(player);
		boolean desync = flagDead && hpAlive;
		if (!desync) return;
		if (player.ticksExisted % 10 != 0) return;
		String note = "TICK_DESYNC guiOver=" + gameOver;
		if (player.ticksExisted % DESYNC_CHAT_INTERVAL == 0) {
			logImportant(player, note, null);
		} else {
			log(player, note, null);
		}
	}

	private static void clearGameOverScreen(EntityPlayer player) {
		try {
			player.isDead = false;
			player.deathTime = 0;
			Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
			Object mc = null;
			try {
				mc = mcClass.getMethod("func_71410_x").invoke(null);
			} catch (Throwable ignored) {
			}
			if (mc == null) {
				mc = mcClass.getMethod("getMinecraft").invoke(null);
			}
			if (mc == null) return;
			Class<?> guiClass = Class.forName("net.minecraft.client.gui.GuiScreen");
			try {
				mcClass.getMethod("func_147108_a", guiClass).invoke(mc, new Object[] { null });
			} catch (Throwable t) {
				mcClass.getMethod("displayGuiScreen", guiClass).invoke(mc, new Object[] { null });
			}
		} catch (Throwable ignored) {
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
