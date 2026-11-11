package com.mrsand.StatBoostChanges.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.brewing.potions.PotionResizing;

import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

/**
 * Opt-in probe for Legends coffin rest (enter / close / Space pass-day / exit).
 * Enable with JVM arg (not Minecraft "game" args):
 *   -Dstatboo.debugCoffin=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 */
public final class CoffinDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/Coffin");

	private static final boolean ENABLED = Boolean.parseBoolean(
			System.getProperty("statboo.debugCoffin", "false"));
	private static boolean announced;
	private static int clientTickThrottle;
	private static final Map<String, Double> LAST_POS_Y = new HashMap<String, Double>();

	private CoffinDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("Coffin debug enabled via -Dstatboo.debugCoffin=true");
		chat(player, "Coffin debug ON — Space/close/enter in latest.log");
	}

	public static void log(EntityPlayer player, String note) {
		if (!ENABLED || player == null) return;
		LOG.info(format(player, note));
	}

	public static void logImportant(EntityPlayer player, String note) {
		if (!ENABLED || player == null) return;
		String line = format(player, note);
		LOG.info(line);
		if (player.worldObj != null) {
			chat(player, note);
		}
	}

	/** Client: sparse status while nested so Space failures are obvious. */
	public static void tickClient(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		announceIfNeeded(player);
		if (!CoffinSleepUtil.isInCoffin(player)) return;
		if ((++clientTickThrottle) % 40 != 0) return;
		LOG.info(format(player, "NESTED " + CoffinSleepUtil.describeNestState(player)
				+ " " + describeResize(player)));
	}

	/** Server: same identity line, about every 40 ticks, only while nested. */
	public static void tickServer(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!CoffinSleepUtil.isInCoffin(player)) return;
		if (player.ticksExisted % 40 != 0) return;
		LOG.info(format(player, "NESTED " + CoffinSleepUtil.describeNestState(player)
				+ " " + describeResize(player)));
	}

	/** Both sides, every 20 ticks while nested. Not behind the debug flag. */
	public static void tickPose(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		if (!CoffinSleepUtil.isInCoffin(player)) return;
		if (player.ticksExisted % 20 != 0) return;
		logPose(player, "TICK");
	}

	/** One line after Legends has placed the player and the server has synced it. */
	public static void logEnter(EntityPlayer player) {
		logPose(player, "ENTER");
	}

	/** Unthrottled pose line. Used when the coffin camera changes. */
	public static void logPoseNow(EntityPlayer player, String tag) {
		logPose(player, tag);
	}

	private static void logPose(EntityPlayer player, String tag) {
		if (player == null || player.worldObj == null) return;
		String side = player.worldObj.isRemote ? "C" : "S";
		String key = side + player.getUniqueID();
		Double prev = LAST_POS_Y.get(key);
		String dY = prev == null ? "none" : String.format("%.3f", player.posY - prev.doubleValue());
		LAST_POS_Y.put(key, Double.valueOf(player.posY));
		LOG.info("[" + side + " " + tag + "] " + safeName(player)
				+ " posY=" + String.format("%.3f", player.posY)
				+ " dY=" + dY
				+ " yOff=" + String.format("%.3f", player.yOffset)
				+ " eyeH=" + String.format("%.3f", player.eyeHeight)
				+ " size=" + String.format("%.2f", player.width) + "x" + String.format("%.2f", player.height)
				+ " sleeping=" + player.sleeping
				+ " noClip=" + player.noClip
				+ " motionY=" + String.format("%.3f", player.motionY)
				+ " " + describeBedBlock(player)
				+ " " + describeResizePotion(player)
				+ describeCamera(player));
	}

	private static String describeCamera(EntityPlayer player) {
		if (player.worldObj == null || !player.worldObj.isRemote) return "";
		String camera = com.mrsand.StatBoostChanges.client.CoffinCamera.describe(player);
		if (camera == null || camera.length() == 0) return "";
		return " " + camera;
	}

	private static String describeBedBlock(EntityPlayer player) {
		ChunkCoordinates bed = CoffinSleepUtil.bedLocation(player);
		if (bed == null || player.worldObj == null) return "cls=none witcheryEq=false";
		Block block;
		try {
			block = player.worldObj.getBlock(bed.posX, bed.posY, bed.posZ);
		} catch (Throwable t) {
			return "cls=? witcheryEq=?";
		}
		if (block == null) return "cls=null witcheryEq=false";
		boolean witcheryEq = false;
		if (Loader.isModLoaded("witchery")) {
			try {
				witcheryEq = block == Witchery.Blocks.COFFIN;
			} catch (Throwable ignored) {
			}
		}
		return "cls=" + block.getClass().getName() + " witcheryEq=" + witcheryEq;
	}

	private static String describeResizePotion(EntityPlayer player) {
		if (!Loader.isModLoaded("witchery")) return "resizePotion=absent";
		try {
			if (Witchery.Potions == null || Witchery.Potions.RESIZING == null) return "resizePotion=?";
			return "resizePotion=" + player.isPotionActive(Witchery.Potions.RESIZING);
		} catch (Throwable t) {
			return "resizePotion=?";
		}
	}

	/**
	 * Stored-block identity plus Witchery resize. Does not change the player.
	 * {@code resizePotion=true} is the potion (GL scale). Size {@code 0.2x0.2} with
	 * the potion off is the coffin {@code setEntitySize} call.
	 */
	public static String describeResize(EntityPlayer player) {
		if (player == null) return "resize=?";
		StringBuilder sb = new StringBuilder();
		appendStoredIdentity(sb, player);
		sb.append(" size=").append(String.format("%.2f", player.width))
				.append("x").append(String.format("%.2f", player.height));
		sb.append(" yOff=").append(String.format("%.3f", player.yOffset));
		sb.append(" sleeping=").append(player.sleeping);
		ChunkCoordinates bed = player.playerLocation;
		if (bed == null) {
			sb.append(" bed=none");
		} else {
			sb.append(" bed=").append(bed.posX).append(",").append(bed.posY).append(",").append(bed.posZ);
		}
		appendWitcheryResize(sb, player);
		return sb.toString();
	}

	private static void appendStoredIdentity(StringBuilder sb, EntityPlayer player) {
		ChunkCoordinates stored = CoffinSleepUtil.bedLocation(player);
		if (stored == null || player.worldObj == null) {
			sb.append("cls=none reg=none witcheryEq=false");
			return;
		}
		Block block = null;
		try {
			block = player.worldObj.getBlock(stored.posX, stored.posY, stored.posZ);
		} catch (Throwable t) {
			sb.append("cls=? reg=? witcheryEq=?");
			return;
		}
		if (block == null) {
			sb.append("cls=null reg=null witcheryEq=false");
			return;
		}
		sb.append("cls=").append(block.getClass().getName());
		String reg = null;
		try {
			reg = Block.blockRegistry.getNameForObject(block);
		} catch (Throwable ignored) {
		}
		sb.append(" reg=").append(reg == null ? "?" : reg);
		boolean witcheryEq = false;
		if (Loader.isModLoaded("witchery")) {
			try {
				witcheryEq = block == Witchery.Blocks.COFFIN;
			} catch (Throwable ignored) {
			}
		}
		sb.append(" witcheryEq=").append(witcheryEq);
	}

	private static void appendWitcheryResize(StringBuilder sb, EntityPlayer player) {
		if (!Loader.isModLoaded("witchery")) {
			sb.append(" resizePotion=absent");
			return;
		}
		try {
			if (Witchery.Potions == null || Witchery.Potions.RESIZING == null) {
				sb.append(" resizePotion=?");
				return;
			}
			boolean active = player.isPotionActive(Witchery.Potions.RESIZING);
			sb.append(" resizePotion=").append(active);
			if (!active) return;
			PotionEffect effect = player.getActivePotionEffect(Witchery.Potions.RESIZING);
			if (effect == null) {
				sb.append(" amp=? dur=? scale=?");
				return;
			}
			int amp = effect.getAmplifier();
			sb.append(" amp=").append(amp);
			sb.append(" dur=").append(effect.getDuration());
			sb.append(" scale=").append(String.format("%.2f", PotionResizing.getScaleFactor(amp)));
		} catch (Throwable t) {
			sb.append(" resizePotion=?");
		}
	}

	private static String format(EntityPlayer player, String note) {
		String side = player.worldObj == null ? "?" : (player.worldObj.isRemote ? "C" : "S");
		ChunkCoordinates stored = CoffinSleepUtil.bedLocation(player);
		String storedStr = stored == null ? "none"
				: stored.posX + "," + stored.posY + "," + stored.posZ;
		int fx = CoffinSleepUtil.getFeetBlockX(player);
		int fy = CoffinSleepUtil.getFeetBlockY(player);
		int fz = CoffinSleepUtil.getFeetBlockZ(player);
		int bodyY = MathHelper.floor_double(player.posY);
		Block feetAt = player.worldObj != null ? player.worldObj.getBlock(fx, fy, fz) : null;
		Block feetBelow = player.worldObj != null ? player.worldObj.getBlock(fx, fy - 1, fz) : null;
		Block bodyAt = player.worldObj != null ? player.worldObj.getBlock(fx, bodyY, fz) : null;
		return String.format(
				"[%s] %s | inCoffin=%s closed=%s day=%s bloodOK=%s stored=%s feetY=%d feetAt=%s feetBelow=%s bodyAt=%s pos=%.1f,%.1f,%.1f time=%d | %s",
				side,
				safeName(player),
				Boolean.toString(CoffinSleepUtil.isInCoffin(player)),
				Boolean.toString(CoffinSleepUtil.isInClosedLegendsCoffin(player)),
				Boolean.toString(player.worldObj != null && player.worldObj.isDaytime()),
				Boolean.toString(CoffinSleepUtil.hasBloodToRest(player)),
				storedStr,
				Integer.valueOf(fy),
				blockName(feetAt),
				blockName(feetBelow),
				blockName(bodyAt),
				player.posX, player.posY, player.posZ,
				player.worldObj != null ? (player.worldObj.getWorldTime() % 24000L) : -1L,
				note);
	}

	private static String blockName(Block b) {
		if (b == null) return "null";
		try {
			String name = Block.blockRegistry.getNameForObject(b);
			boolean open = CoffinSleepUtil.isOpenCoffin(b);
			boolean closed = CoffinSleepUtil.isClosedCoffin(b);
			boolean legends = CoffinSleepUtil.isLegendsCoffinBlock(b);
			return name + "(legends=" + legends + ",open=" + open + ",closed=" + closed
					+ ",cls=" + b.getClass().getSimpleName() + ")";
		} catch (Throwable t) {
			return b.getClass().getSimpleName();
		}
	}

	private static String safeName(EntityPlayer player) {
		try {
			return player.getCommandSenderName();
		} catch (Throwable t) {
			return "?";
		}
	}

	private static void chat(EntityPlayer player, String text) {
		try {
			player.addChatComponentMessage(new ChatComponentText("[CoffinDebug] " + text)
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA)));
		} catch (Throwable ignored) {
		}
	}
}
