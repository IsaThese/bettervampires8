package com.mrsand.StatBoostChanges.util;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * Opt-in FP hand render probe. Enable with a JVM argument (not Minecraft "game" args):
 *   -Dstatboo.debugHandRender=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 *
 * Symptom this targets: bat/wolf morph ends up showing a vanilla player hand.
 */
@SideOnly(Side.CLIENT)
public final class HandRenderDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/HandRender");

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("statboo.debugHandRender", "false"));
	private static long lastPipelineLogMs;
	private static long lastListenerDumpMs;
	private static long lastChatMs;
	private static boolean dumpedSubscribers;
	private static boolean announcedInChat;
	private static boolean announcedJvmFlag;

	private HandRenderDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	/** Call once from client when the local player is in a world (confirms JVM flag + mod loaded). */
	public static void announceJvmFlagIfNeeded() {
		if (!ENABLED || announcedJvmFlag) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null) return;
		announcedJvmFlag = true;
		LOG.info("Hand render debug enabled via -Dstatboo.debugHandRender=true");
		if (!Loader.isModLoaded("legends")) {
			logImportant("Hand debug ON but Legends not loaded — bat/wolf probe disabled", true);
			return;
		}
		logImportant("Hand debug ON. Morph Legends bat/wolf, stay 1st person; watch [Hand] in chat", true);
	}

	public static boolean shouldProbe() {
		if (!ENABLED || !Loader.isModLoaded("legends")) return false;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return false;
		EntityPlayer p = mc.thePlayer;
		return MorphHelper.isBat(p) || MorphHelper.isWolf(p);
	}

	public static String morphLabel() {
		EntityPlayer p = Minecraft.getMinecraft().thePlayer;
		if (p == null) return "?";
		if (MorphHelper.isBat(p)) return "bat";
		if (MorphHelper.isWolf(p)) return "wolf";
		return "none";
	}

	public static void log(String msg) {
		if (!ENABLED) return;
		LOG.info("[morph={}] {}", morphLabel(), msg);
	}

	/** File log + in-game chat (throttled unless {@code forceChat}). */
	public static void logImportant(String msg, boolean forceChat) {
		if (!ENABLED) return;
		LOG.info("[morph={}] {}", morphLabel(), msg);
		chat(msg, forceChat);
	}

	public static void logPipeline(String stage) {
		if (!shouldProbe()) return;
		long now = System.currentTimeMillis();
		if (now - lastPipelineLogMs < 500L) return;
		lastPipelineLogMs = now;
		log(stage);
	}

	public static void onRenderHandBegin(RenderHandEvent event) {
		if (!shouldProbe()) return;
		dumpSubscribersOnce(event);
		log("RenderHandEvent BEGIN canceled=" + event.isCanceled());
	}

	public static void onRenderHandEnd(RenderHandEvent event) {
		if (!shouldProbe()) return;
		boolean canceled = event.isCanceled();
		log("RenderHandEvent END canceled=" + canceled
				+ " (if true, ItemRenderer was skipped by a listener above)");
		if (canceled) {
			logImportant("Hand event CANCELED — ItemRenderer/morph path skipped", false);
		}
	}

	public static void onEventBusListener(IEventListener listener, RenderHandEvent event, boolean canceledBefore) {
		if (!shouldProbe()) return;
		boolean after = event.isCanceled();
		String owner = describeListener(listener);
		if (canceledBefore != after) {
			String msg = "CANCEL FLIP: " + owner;
			log("RenderHandEvent " + msg + " -> canceled=" + after);
			logImportant(msg, true);
		} else {
			long now = System.currentTimeMillis();
			if (now - lastListenerDumpMs >= 1000L) {
				log("RenderHandEvent listener " + owner + " canceled=" + after);
			}
		}
	}

	public static void markListenerPassDone() {
		lastListenerDumpMs = System.currentTimeMillis();
	}

	private static void dumpSubscribersOnce(RenderHandEvent event) {
		if (dumpedSubscribers) return;
		dumpedSubscribers = true;
		if (!announcedInChat) {
			announcedInChat = true;
			chat("Hand debug ON (bat/wolf FP). Watch CANCEL FLIP / FP_ARM in chat; stacks in latest.log", true);
		}
		log("First RenderHandEvent while morph=" + morphLabel()
				+ " — watch CANCEL FLIP / FP_ARM / morphHand in log");
	}

	private static void chat(String msg, boolean force) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null) return;
		long now = System.currentTimeMillis();
		if (!force && now - lastChatMs < 2500L) return;
		lastChatMs = now;
		String line = "[Hand] " + msg;
		if (line.length() > 120) {
			line = line.substring(0, 117) + "...";
		}
		GeneralUtil.sendDebugMessage(mc.thePlayer, new ChatComponentText(line));
	}

	public static String describeListener(IEventListener listener) {
		if (listener == null) return "null";
		Class<?> c = listener.getClass();
		String raw = String.valueOf(listener);
		String mod = modIdForClass(c);
		if ("unknown".equals(mod) && raw.contains("@")) {
			int asm = raw.indexOf("ASM:");
			if (asm >= 0) {
				String rest = raw.substring(asm + 4).trim();
				int sp = rest.indexOf(' ');
				if (sp > 0) {
					String cls = rest.substring(0, sp).replace('/', '.');
					try {
						mod = modIdForClass(Class.forName(cls));
						return mod + " :: " + rest;
					} catch (Throwable ignored) {
						return rest;
					}
				}
			}
		}
		return mod + " :: " + raw;
	}

	public static String modIdForClass(Class<?> clazz) {
		if (clazz == null) return "unknown";
		try {
			CodeSource src = clazz.getProtectionDomain() != null ? clazz.getProtectionDomain().getCodeSource() : null;
			URL loc = src != null ? src.getLocation() : null;
			if (loc != null) {
				File jar = urlToFile(loc);
				if (jar != null) {
					List<ModContainer> mods = Loader.instance().getActiveModList();
					for (ModContainer mod : mods) {
						File modSrc = mod.getSource();
						if (modSrc != null && filesEqual(modSrc, jar)) {
							return mod.getModId();
						}
					}
					return "jar:" + jar.getName();
				}
			}
		} catch (Throwable ignored) {}
		String n = clazz.getName();
		if (n.startsWith("net.minecraft") || n.startsWith("net.minecraftforge")) return "minecraft/forge";
		if (n.startsWith("com.tihyo.legends") || n.startsWith("com.tihyo.horror")) return "legends";
		if (n.startsWith("de.teamlapen.vampirism")) return "vampirism";
		if (n.startsWith("com.mrsand.StatBoostChanges")) return "StatBoostChanges";
		return "unknown";
	}

	private static File urlToFile(URL url) {
		try {
			if ("file".equals(url.getProtocol())) {
				return new File(url.toURI());
			}
		} catch (Exception e) {
			try {
				return new File(url.getPath());
			} catch (Exception ignored) {}
		}
		return null;
	}

	private static boolean filesEqual(File a, File b) {
		try {
			return a.getCanonicalFile().equals(b.getCanonicalFile());
		} catch (Exception e) {
			return a.equals(b);
		}
	}
}
