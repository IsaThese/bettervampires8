package com.mrsand.StatBoostChanges.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;

/**
 * Opt-in dense probe for coffin enter/exit position, hitbox, collision, and F5 render sink.
 * Enable with JVM arg (not Minecraft "game" args):
 *   -Dstatboo.debugCoffinPose=true
 * CurseForge: instance → Settings (gear) → Java Settings → Additional Arguments.
 *
 * While nested (and 3s after leave) logs <b>every tick</b> to {@code StatBoo/CoffinPose}.
 * Expect a large latest.log — filter that logger name.
 */
public final class CoffinPoseDebug {

	public static final Logger LOG = LogManager.getLogger("StatBoo/CoffinPose");

	private static final boolean ENABLED = Boolean.parseBoolean(
			System.getProperty("statboo.debugCoffinPose", "false"));

	/** Keep dumping after leave so exit yank / stuck eyeHeight is visible. */
	private static final long POST_LEAVE_MS = 3000L;

	private static boolean announced;
	private static long lastRenderLogMs;
	private static long lastExitMarkMs;
	private static Field sleepingField;
	/** Dedupe: same ticksExisted+tag was dumping twice per side. */
	private static int lastDenseTickS = -1;
	private static int lastDenseTickC = -1;
	private static String lastDenseTagS = "";
	private static String lastDenseTagC = "";
	private static int lastDesyncTickS = -1;
	private static int lastDesyncTickC = -1;
	private static int lastLeaveDesyncTickS = -1;
	private static int lastLeaveDesyncTickC = -1;
	private static int lastCsGapTickS = -1;
	private static int lastYLiftTickC = -1;
	private static int lastMoveYLiftTickC = -1;
	private static Boolean lastNoClipS;
	private static Boolean lastNoClipC;
	private static int lastSetPosTickS = -1;
	private static int lastSetPosTickC = -1;

	private CoffinPoseDebug() {}

	public static boolean enabled() {
		return ENABLED;
	}

	public static void announceIfNeeded(EntityPlayer player) {
		if (!ENABLED || announced || player == null || player.worldObj == null) return;
		if (!player.worldObj.isRemote) return;
		announced = true;
		LOG.info("CoffinPose debug enabled via -Dstatboo.debugCoffinPose=true (every-tick while nested)");
		chat(player, "CoffinPose debug ON — every-tick nest dumps; watch latest.log");
	}

	/** Mark exit time so dense TICK / RENDER keep dumping after leave. */
	public static void markExit() {
		lastExitMarkMs = System.currentTimeMillis();
	}

	public static void dump(EntityPlayer player, String tag) {
		if (!ENABLED || player == null) return;
		String line = format(player, tag);
		LOG.info(line);
		if ("ENTER".equals(tag) || (tag != null && tag.startsWith("EXIT"))) {
			chat(player, tag + " logged");
		}
	}

	/** Every nest/post-leave tick dump (default tag TICK). */
	public static void tickDense(EntityPlayer player) {
		tickDense(player, "TICK");
	}

	/**
	 * Nest/post-leave dump with phase tag ({@code TICK_START}/{@code TICK_END}).
	 * At most one dump per ticksExisted+tag per side.
	 */
	public static void tickDense(EntityPlayer player, String tag) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		boolean nested = CoffinSleepUtil.isInCoffin(player);
		boolean recentExit = (System.currentTimeMillis() - lastExitMarkMs) < POST_LEAVE_MS;
		if (!nested && !recentExit) return;
		if (tag == null || tag.length() == 0) tag = "TICK";
		boolean remote = player.worldObj.isRemote;
		int tick = player.ticksExisted;
		if (remote) {
			if (tick == lastDenseTickC && tag.equals(lastDenseTagC)) return;
			lastDenseTickC = tick;
			lastDenseTagC = tag;
		} else {
			if (tick == lastDenseTickS && tag.equals(lastDenseTagS)) return;
			lastDenseTickS = tick;
			lastDenseTagS = tag;
		}
		dump(player, tag);
	}

	/**
	 * Extra probe when nested pos/prev fight or AABB sinks under the nest block.
	 */
	public static void checkNestDesync(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		if (!CoffinSleepUtil.isInCoffin(player)) return;

		double dPrevY = player.posY - player.prevPosY;
		boolean prevFight = Math.abs(dPrevY) > 0.25D;
		boolean underNest = false;
		ChunkCoordinates nest = CoffinSleepUtil.bedLocation(player);
		AxisAlignedBB bb = player.boundingBox;
		if (nest != null && bb != null && bb.minY < nest.posY - 0.1D) {
			underNest = true;
		}
		if (!prevFight && !underNest) return;

		boolean remote = player.worldObj.isRemote;
		int tick = player.ticksExisted;
		if (remote) {
			if (tick == lastDesyncTickC) return;
			lastDesyncTickC = tick;
		} else {
			if (tick == lastDesyncTickS) return;
			lastDesyncTickS = tick;
		}
		dump(player, "DESYNC");
	}

	/**
	 * Post-leave settle: AABB sunk under stand/nest floor, or pos/prev fight.
	 */
	public static void checkLeaveDesync(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		if (CoffinSleepUtil.isInCoffin(player)) return;
		boolean settling = CoffinSleepUtil.isPostLeaveSettling(player);
		boolean recentExit = (System.currentTimeMillis() - lastExitMarkMs) < POST_LEAVE_MS;
		if (!settling && !recentExit) return;

		double dPrevY = player.posY - player.prevPosY;
		boolean prevFight = Math.abs(dPrevY) > 0.25D;
		boolean sunk = false;
		AxisAlignedBB bb = player.boundingBox;
		ChunkCoordinates stand = CoffinSleepUtil.getPostLeaveStand(player);
		if (stand != null && bb != null && bb.minY < stand.posY - 0.1D) {
			sunk = true;
		} else {
			ChunkCoordinates nest = CoffinSleepUtil.bedLocation(player);
			if (nest != null && bb != null && bb.minY < nest.posY - 0.1D) {
				sunk = true;
			}
		}
		if (!prevFight && !sunk) return;

		boolean remote = player.worldObj.isRemote;
		int tick = player.ticksExisted;
		if (remote) {
			if (tick == lastLeaveDesyncTickC) return;
			lastLeaveDesyncTickC = tick;
		} else {
			if (tick == lastLeaveDesyncTickS) return;
			lastLeaveDesyncTickS = tick;
		}
		dump(player, "LEAVE_DESYNC");
	}

	/**
	 * Server-only: after START settle, |posY - standTargetY| &gt; 0.5 means packet/prediction fight
	 * without needing client world access.
	 */
	public static void checkCsGap(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (CoffinSleepUtil.isInCoffin(player)) return;
		if (!CoffinSleepUtil.isPostLeaveSettling(player)) return;
		double targetY = CoffinSleepUtil.getLeaveStandTargetY(player);
		if (Double.isNaN(targetY)) return;
		double gap = Math.abs(player.posY - targetY);
		if (gap <= 0.5D) return;
		int tick = player.ticksExisted;
		if (tick == lastCsGapTickS) return;
		lastCsGapTickS = tick;
		dump(player, "CS_GAP gap=" + String.format("%.3f", gap)
				+ " targetY=" + String.format("%.3f", targetY)
				+ " sunk=" + CoffinSleepUtil.isLeaveStandSunk(player));
	}

	/** Log when noClip flips during nest or post-leave window. */
	public static void checkNoClipFlip(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		boolean nested = CoffinSleepUtil.isInCoffin(player);
		boolean settling = CoffinSleepUtil.isPostLeaveSettling(player);
		boolean recentExit = (System.currentTimeMillis() - lastExitMarkMs) < POST_LEAVE_MS;
		if (!nested && !settling && !recentExit) return;

		boolean remote = player.worldObj.isRemote;
		Boolean prev = remote ? lastNoClipC : lastNoClipS;
		boolean now = player.noClip;
		if (prev != null && prev.booleanValue() == now) return;
		if (remote) {
			lastNoClipC = Boolean.valueOf(now);
		} else {
			lastNoClipS = Boolean.valueOf(now);
		}
		if (prev == null) return;
		LOG.info(format(player, "NOCLIP_FLIP")
				+ " was=" + prev.booleanValue() + " now=" + now);
	}

	/** Snapshot helper used around leaveNestPhysics edges. */
	public static void noteNoClip(EntityPlayer player, String tag) {
		if (!ENABLED || player == null) return;
		checkNoClipFlip(player);
		if (tag != null && tag.length() > 0) {
			// format already includes noClip; tag distinguishes edge
		}
	}

	/**
	 * Client START: already at standY+2×yOffset before the leave Y fix runs.
	 */
	public static void checkLeaveYLift(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (CoffinSleepUtil.isInCoffin(player)) return;
		if (!CoffinSleepUtil.isDoubleYOffsetPattern(player)) return;
		int tick = player.ticksExisted;
		if (tick == lastYLiftTickC) return;
		lastYLiftTickC = tick;
		ChunkCoordinates stand = CoffinSleepUtil.getPostLeaveStand(player);
		double good = stand != null
				? stand.posY + CoffinSleepUtil.STANDING_Y_OFFSET
				: Double.NaN;
		dump(player, "LEAVE_Y_LIFT goodY="
				+ (Double.isNaN(good) ? "?" : String.format("%.3f", good))
				+ " highY=" + String.format("%.3f",
						Double.isNaN(good) ? player.posY : good + CoffinSleepUtil.STANDING_Y_OFFSET));
	}

	/**
	 * Called from EntityPlayer setPosition / setPositionAndRotation mixin when
	 * |newY-oldY| &gt; 0.2 during nest or leave window (catches ≈1.62 double-Y lifts).
	 */
	public static void onSetPosition(EntityPlayer player, double newX, double newY, double newZ) {
		if (!ENABLED || player == null || player.worldObj == null) return;
		boolean nested = CoffinSleepUtil.isInCoffin(player);
		boolean settling = CoffinSleepUtil.isPostLeaveSettling(player);
		boolean locked = CoffinSleepUtil.isClientLeaveLocked(player);
		boolean recentExit = (System.currentTimeMillis() - lastExitMarkMs) < POST_LEAVE_MS;
		if (!nested && !settling && !locked && !recentExit) return;
		double dy = newY - player.posY;
		if (Math.abs(dy) <= 0.2D) return;

		// ≈+yOffset double-lift: always log (cross-mod caller hunt).
		boolean bigLift = Math.abs(dy) > 1.0D;
		boolean remote = player.worldObj.isRemote;
		if (!bigLift) {
			int tick = player.ticksExisted;
			if (remote) {
				if (tick == lastSetPosTickC) return;
				lastSetPosTickC = tick;
			} else {
				if (tick == lastSetPosTickS) return;
				lastSetPosTickS = tick;
			}
		}

		String caller = findInterestingCaller();
		LOG.info(format(player, "SETPOS")
				+ " newY=" + fmt3(newY)
				+ " dY=" + fmt3(dy)
				+ " caller=" + caller);
	}

	/** Once per tick: moveEntity produced stand+2×yOffset / bad AABB feet. */
	public static void dumpMoveYLift(EntityPlayer player, double goodY) {
		if (!ENABLED || player == null) return;
		int tick = player.ticksExisted;
		if (tick == lastMoveYLiftTickC) return;
		lastMoveYLiftTickC = tick;
		dump(player, "MOVE_Y_LIFT goodY=" + String.format("%.3f", goodY)
				+ " highY=" + String.format("%.3f", goodY + CoffinSleepUtil.STANDING_Y_OFFSET));
	}

	private static int lastLeaveLockSnapTickC = -1;
	private static int lastSetPosAbsorbTickC = -1;

	/** Client leave-lock rebuild of AABB/posY. */
	public static void dumpLeaveLockSnap(EntityPlayer player, double goodY) {
		if (!ENABLED || player == null) return;
		int tick = player.ticksExisted;
		if (tick == lastLeaveLockSnapTickC) return;
		lastLeaveLockSnapTickC = tick;
		dump(player, "LEAVE_LOCK_SNAP goodY=" + String.format("%.3f", goodY));
	}

	/** NetHandler setPosition double-Y rewritten to stand entity Y. */
	public static void dumpSetPosAbsorb(EntityPlayer player, double badY, double goodY) {
		if (!ENABLED || player == null) return;
		int tick = player.ticksExisted;
		if (tick == lastSetPosAbsorbTickC) return;
		lastSetPosAbsorbTickC = tick;
		dump(player, "SETPOS_ABSORB badY=" + String.format("%.3f", badY)
				+ " goodY=" + String.format("%.3f", goodY)
				+ " caller=" + findInterestingCaller());
	}

	private static int lastApplySkipTickC = -1;

	/** Duplicate PacketLeaveStand ignored (same stand already applied). */
	public static void dumpApplyLeaveStandSkip(EntityPlayer player, ChunkCoordinates stand) {
		if (!ENABLED || player == null) return;
		int tick = player.ticksExisted;
		if (tick == lastApplySkipTickC) return;
		lastApplySkipTickC = tick;
		String s = stand == null ? "?" : stand.posX + "," + stand.posY + "," + stand.posZ;
		dump(player, "APPLY_LEAVE_STAND_SKIP sameStand=" + s);
	}

	/**
	 * Vanilla {@code setPlayerLocation} would send S08 as {@code entityY + 1.62}.
	 * Log what that packet Y would have been (root-cause proof).
	 */
	public static void dumpS08WouldSend(EntityPlayer player, double entityY) {
		if (!ENABLED || player == null) return;
		double packetY = entityY + 1.6200000047683716D;
		dump(player, "S08_WOULD entityY=" + String.format("%.3f", entityY)
				+ " packetY=" + String.format("%.3f", packetY)
				+ " caller=" + findInterestingCaller());
	}

	/** BV omitted setPlayerLocation to avoid S08 entityY+1.62 double-offset. */
	public static void dumpS08Skipped(EntityPlayer player, String reason, double entityY) {
		if (!ENABLED || player == null) return;
		double packetY = entityY + 1.6200000047683716D;
		dump(player, "S08_SKIPPED reason=" + reason
				+ " entityY=" + String.format("%.3f", entityY)
				+ " wouldPacketY=" + String.format("%.3f", packetY));
	}

	/** Leave pos-sync mixin sent S08 with entity Y (no +1.62). */
	public static void dumpLeaveS08EntityY(EntityPlayer player, double entityY) {
		if (!ENABLED || player == null) return;
		dump(player, "LEAVE_S08_ENTITY_Y entityY=" + String.format("%.3f", entityY));
	}

	/**
	 * {@code setLocationAndAngles} probe — unthrottled when vanilla would apply ≈+yOffset lift.
	 */
	public static void onSetLocationAndAngles(EntityPlayer player, double x, double y, double z) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (!CoffinSleepUtil.isClientLeaveYProtect(player)
				&& !CoffinSleepUtil.isClientLeaveLocked(player)
				&& !CoffinSleepUtil.isInCoffin(player)) {
			boolean recentExit = (System.currentTimeMillis() - lastExitMarkMs) < POST_LEAVE_MS;
			if (!recentExit) return;
		}
		float yOff = player.yOffset >= 0.5F ? player.yOffset : CoffinSleepUtil.STANDING_Y_OFFSET;
		double wouldY = y + yOff;
		double dWould = wouldY - player.posY;
		double dIn = y - player.posY;
		boolean bigLift = Math.abs(dWould) > 0.5D || Math.abs(dIn) > 0.5D;
		if (!bigLift) return;
		boolean absorb = CoffinSleepUtil.shouldAbsorbSetLocationEntityY(player, y);
		LOG.info(format(player, "SETPOS_LOC")
				+ " inY=" + fmt3(y)
				+ " wouldY=" + fmt3(wouldY)
				+ " dWould=" + fmt3(dWould)
				+ " absorb=" + absorb
				+ " caller=" + findInterestingCaller());
	}

	private static int lastLiftLivingTickC = -1;

	/** HIGHEST LivingUpdate: remember Y while leave-locked. */
	public static void noteLeaveLiftLivingStart(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (!CoffinSleepUtil.isClientLeaveLocked(player) && !CoffinSleepUtil.isClientLeaveYProtect(player)) {
			return;
		}
		player.getEntityData().setDouble("BvLiftProbeY", player.posY);
	}

	/** LOWEST LivingUpdate: log if Y rose &gt;0.5 during entity update (mod vs vanilla). */
	public static void noteLeaveLiftLivingEnd(EntityPlayer player) {
		if (!ENABLED || player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (!CoffinSleepUtil.isClientLeaveLocked(player) && !CoffinSleepUtil.isClientLeaveYProtect(player)) {
			return;
		}
		if (!player.getEntityData().hasKey("BvLiftProbeY")) return;
		double before = player.getEntityData().getDouble("BvLiftProbeY");
		double after = player.posY;
		double dy = after - before;
		if (dy <= 0.5D) return;
		int tick = player.ticksExisted;
		if (tick == lastLiftLivingTickC) return;
		lastLiftLivingTickC = tick;
		dump(player, "LEAVE_LIFT_LIVING beforeY=" + fmt3(before)
				+ " afterY=" + fmt3(after)
				+ " dY=" + fmt3(dy)
				+ " caller=" + findInterestingCaller());
	}

	private static String findInterestingCaller() {
		try {
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			String bestMod = null;
			String bestAny = null;
			for (int i = 0; i < st.length; i++) {
				String cn = st[i].getClassName();
				if (cn == null) continue;
				if (cn.startsWith("com.mrsand.StatBoostChanges.mixins")) continue;
				if (cn.startsWith("com.mrsand.StatBoostChanges.util.CoffinPoseDebug")) continue;
				if (cn.startsWith("java.") || cn.startsWith("sun.") || cn.startsWith("org.spongepowered")) {
					continue;
				}
				String shortName = cn.substring(cn.lastIndexOf('.') + 1) + "." + st[i].getMethodName()
						+ ":" + st[i].getLineNumber();
				String lower = cn.toLowerCase();
				boolean interesting = lower.contains("legends") || lower.contains("tihyo")
						|| lower.contains("witchery") || lower.contains("emoniph")
						|| lower.contains("morph") || lower.contains("vampirism")
						|| lower.contains("teamlapen") || lower.contains("coffinsleep")
						|| lower.contains("nethandler") || lower.contains("entityplayer")
						|| lower.contains("playerhelper") || lower.contains("clientevent")
						|| lower.contains("generalevent") || lower.contains("shapeshift");
				if (interesting && bestMod == null) {
					bestMod = shortName;
				}
				if (bestAny == null && !cn.startsWith("net.minecraft")) {
					bestAny = shortName;
				}
			}
			if (bestMod != null) return bestMod;
			if (bestAny != null) return bestAny;
			if (st.length > 4) {
				return st[4].getClassName() + "." + st[4].getMethodName();
			}
		} catch (Throwable ignored) {
		}
		return "?";
	}

	/** @deprecated use {@link #tickDense} */
	public static void tickWhileNested(EntityPlayer player) {
		tickDense(player);
	}

	@SideOnly(Side.CLIENT)
	public static void onRenderPlayer(EntityPlayer rendered) {
		if (!ENABLED || rendered == null || rendered.worldObj == null) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.thePlayer == null) return;
		if (rendered != mc.thePlayer) return;

		boolean nested = CoffinSleepUtil.isInCoffin(rendered);
		boolean recentExit = (System.currentTimeMillis() - lastExitMarkMs) < POST_LEAVE_MS;
		if (!nested && !recentExit) return;

		// ~5/s — render fires many times per tick
		long now = System.currentTimeMillis();
		if (now - lastRenderLogMs < 200L) return;
		lastRenderLogMs = now;

		int tp = mc.gameSettings.thirdPersonView;
		LOG.info(format(rendered, "RENDER")
				+ " thirdPerson=" + tp
				+ " renderY=" + String.format("%.3f", rendered.posY)
				+ " lastTickY=" + String.format("%.3f", rendered.lastTickPosY));
	}

	public static String format(EntityPlayer player, String tag) {
		String side = (player.worldObj != null && player.worldObj.isRemote) ? "C" : "S";
		StringBuilder sb = new StringBuilder();
		sb.append("[CoffinPose ").append(side).append("/").append(tag).append("]");
		sb.append(" tick=").append(player.ticksExisted);

		boolean inCoffin = false;
		try {
			inCoffin = Loader.isModLoaded("legends") && PlayerHelper.isInCoffin(player);
		} catch (Throwable ignored) {
		}
		sb.append(" inCoffin=").append(inCoffin);

		ChunkCoordinates stored = CoffinSleepUtil.bedLocation(player);
		if (stored != null) {
			sb.append(" stored=").append(stored.posX).append(",").append(stored.posY).append(",").append(stored.posZ);
			appendStoredBlock(sb, player, stored);
		} else {
			sb.append(" stored=none");
		}

		sb.append(" ").append(CoffinSleepUtil.describeNestState(player));

		if (player.worldObj != null) {
			int fx = CoffinSleepUtil.getFeetBlockX(player);
			int fy = CoffinSleepUtil.getFeetBlockY(player);
			int fz = CoffinSleepUtil.getFeetBlockZ(player);
			sb.append(" feetY=").append(fy);
			try {
				Block feetAt = player.worldObj.getBlock(fx, fy, fz);
				Block feetBelow = player.worldObj.getBlock(fx, fy - 1, fz);
				sb.append(" feetAt=").append(feetAt == null ? "null" : feetAt.getClass().getSimpleName());
				sb.append(" feetBelow=").append(feetBelow == null ? "null" : feetBelow.getClass().getSimpleName());
			} catch (Throwable t) {
				sb.append(" feetAt=? feetBelow=?");
			}
			ChunkCoordinates leaveStand = CoffinSleepUtil.getLeaveLockStand(player);
			if (leaveStand != null) {
				sb.append(" leaveStand=").append(leaveStand.posX).append(",")
						.append(leaveStand.posY).append(",").append(leaveStand.posZ);
				sb.append(" ").append(CoffinSleepUtil.describeStandFloor(player.worldObj, leaveStand));
			}
		}

		sb.append(" pos=").append(fmt3(player.posX)).append(",")
				.append(fmt3(player.posY)).append(",").append(fmt3(player.posZ));
		sb.append(" prev=").append(fmt3(player.prevPosX)).append(",")
				.append(fmt3(player.prevPosY)).append(",").append(fmt3(player.prevPosZ));
		sb.append(" lastTick=").append(fmt3(player.lastTickPosX)).append(",")
				.append(fmt3(player.lastTickPosY)).append(",").append(fmt3(player.lastTickPosZ));
		sb.append(" dPrevY=").append(fmt3(player.posY - player.prevPosY));

		sb.append(" size=").append(fmt2(player.width)).append("x").append(fmt2(player.height));
		sb.append(" yOff=").append(fmt3(player.yOffset));
		sb.append(" eyeH=").append(fmt3(player.eyeHeight));
		try {
			sb.append(" defEye=").append(fmt3(player.getDefaultEyeHeight()));
		} catch (Throwable t) {
			sb.append(" defEye=?");
		}
		try {
			sb.append(" getEye=").append(fmt3(player.getEyeHeight()));
		} catch (Throwable t) {
			sb.append(" getEye=?");
		}

		AxisAlignedBB bb = player.boundingBox;
		if (bb != null) {
			sb.append(" aabbY=").append(fmt3(bb.minY)).append("..").append(fmt3(bb.maxY));
			sb.append(" aabbXZ=").append(fmt3(bb.maxX - bb.minX)).append("x")
					.append(fmt3(bb.maxZ - bb.minZ));
		} else {
			sb.append(" aabb=null");
		}

		sb.append(" onGround=").append(player.onGround);
		sb.append(" collided=").append(player.isCollided);
		sb.append(" noClip=").append(player.noClip);
		try {
			sb.append(" insideOpaque=").append(player.isEntityInsideOpaqueBlock());
		} catch (Throwable t) {
			sb.append(" insideOpaque=?");
		}

		sb.append(" sleepTimer=").append(player.sleepTimer);
		Boolean sleeping = readSleeping(player);
		if (sleeping != null) {
			sb.append(" sleeping=").append(sleeping.booleanValue());
		}
		sb.append(" ").append(CoffinDebug.describeResize(player));

		if (stored != null && bb != null) {
			sb.append(" aabbMinVsStoredY=").append(fmt3(bb.minY - stored.posY));
			sb.append(" deltaStoredY=").append(fmt3(player.posY - stored.posY));
			sb.append(" nestY=").append(stored.posY);
			sb.append(" deltaNestY=").append(fmt3(player.posY - stored.posY));
			sb.append(" aabbMinVsNest=").append(fmt3(bb.minY - stored.posY));
		}

		if (Loader.isModLoaded("legends")) {
			try {
				Object fr = LegendsProperties.FORCED_ROTATION.get(player);
				sb.append(" forcedRot=").append(fr);
			} catch (Throwable t) {
				sb.append(" forcedRot=?");
			}
			try {
				sb.append(" cantMove=").append(PlayerHelper.cantMove(player));
			} catch (Throwable t) {
				sb.append(" cantMove=?");
			}
			try {
				sb.append(" isLying=").append(PlayerHelper.isLying(player));
			} catch (Throwable t) {
				sb.append(" isLying=?");
			}
			try {
				sb.append(" action=").append(PlayerHelper.getAction(player));
			} catch (Throwable t) {
				sb.append(" action=?");
			}
			try {
				sb.append(" slow=").append(com.tihyo.legends.effects.LegendsEffects.hasSlowness(player));
			} catch (Throwable t) {
				sb.append(" slow=?");
			}
			try {
				sb.append(" morphSize=").append(fmt2(PlayerHelper.getSize(player)));
				sb.append(" morphed=").append(MorphHelper.isMorphed(player));
			} catch (Throwable t) {
				sb.append(" morph=?");
			}
		}

		if (Loader.isModLoaded("vampirism")) {
			try {
				de.teamlapen.vampirism.entity.player.VampirePlayer vp =
						de.teamlapen.vampirism.entity.player.VampirePlayer.get(player);
				sb.append(" vampSleepCoffin=").append(vp != null && vp.sleepingCoffin);
			} catch (Throwable t) {
				sb.append(" vampSleepCoffin=?");
			}
		}

		if (player.worldObj != null && player.worldObj.isRemote) {
			appendClientExtras(sb);
		}

		return sb.toString();
	}

	private static void appendStoredBlock(StringBuilder sb, EntityPlayer player, ChunkCoordinates stored) {
		if (player.worldObj == null) {
			sb.append(" storedBlock=noWorld");
			return;
		}
		try {
			Block b = player.worldObj.getBlock(stored.posX, stored.posY, stored.posZ);
			if (b == null) {
				sb.append(" storedBlock=null");
				return;
			}
			String cls = b.getClass().getSimpleName();
			boolean legends = CoffinSleepUtil.isLegendsCoffinBlock(b);
			if (!legends) {
				sb.append(" storedBlock=").append(cls);
				return;
			}
			sb.append(" storedBlock=").append(cls)
					.append(" open=").append(CoffinSleepUtil.isOpenCoffin(b))
					.append(" closed=").append(CoffinSleepUtil.isClosedCoffin(b));
		} catch (Throwable t) {
			sb.append(" storedBlock=?");
		}
	}

	@SideOnly(Side.CLIENT)
	private static void appendClientExtras(StringBuilder sb) {
		try {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc != null && mc.gameSettings != null) {
				sb.append(" thirdPerson=").append(mc.gameSettings.thirdPersonView);
			}
		} catch (Throwable ignored) {
		}
	}

	private static Boolean readSleeping(EntityPlayer player) {
		try {
			if (sleepingField == null) {
				try {
					sleepingField = EntityPlayer.class.getDeclaredField("sleeping");
				} catch (NoSuchFieldException e) {
					sleepingField = EntityPlayer.class.getDeclaredField("field_71083_bS");
				}
				sleepingField.setAccessible(true);
			}
			return Boolean.valueOf(sleepingField.getBoolean(player));
		} catch (Throwable t) {
			return null;
		}
	}

	private static String fmt3(double v) {
		return String.format("%.3f", v);
	}

	private static String fmt2(float v) {
		return String.format("%.2f", v);
	}

	private static void chat(EntityPlayer player, String text) {
		if (player == null || player.worldObj == null) return;
		try {
			player.addChatComponentMessage(new ChatComponentText("[CoffinPose] " + text)
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE)));
		} catch (Throwable ignored) {
		}
	}
}
