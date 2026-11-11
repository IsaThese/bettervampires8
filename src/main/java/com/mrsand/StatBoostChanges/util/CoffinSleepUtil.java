package com.mrsand.StatBoostChanges.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.horror.blocks.RegisterBlocksHorror;
import com.tihyo.legends.management.PlayerHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.world.World;

/**
 * Legends coffin nest: enter open, close lid, Space to skip day→night while staying inside;
 * exit only by opening the lid. Mimics vanilla bed spawn / wake leave without sleepInBedAt.
 */
public final class CoffinSleepUtil {

	public static final String NBT_LEAVE_STAND = "BvLeaveStand";
	public static final String NBT_LEAVE_X = "BvLeaveStandX";
	public static final String NBT_LEAVE_Y = "BvLeaveStandY";
	public static final String NBT_LEAVE_Z = "BvLeaveStandZ";
	/** Client-only: hold AABB/posY assert until feet on stand floor + walk-away. */
	public static final String NBT_LEAVE_LOCK = "BvLeaveLock";
	public static final String NBT_LEAVE_LOCK_TICK = "BvLeaveLockTick";
	private static final String NBT_LEAVE_LOCK_SEEN = "BvLeaveLockSeen";

	private static final ThreadLocal<Boolean> ALLOW_EXIT = new ThreadLocal<Boolean>();
	private static final Map<Class<?>, Field> OPENED_FIELD_CACHE = new HashMap<Class<?>, Field>();
	/** Remaining ticks to re-assert standing offsets after nest leave (both sides). */
	private static final Map<UUID, Integer> POST_LEAVE_TICKS = new HashMap<UUID, Integer>();
	/** Leave-stand block used to re-pin if settle sinks into the coffin. */
	private static final Map<UUID, ChunkCoordinates> POST_LEAVE_STAND = new HashMap<UUID, ChunkCoordinates>();
	/** One-shot double-yOffset correction dump throttle per leave. */
	private static final Map<UUID, Boolean> LEAVE_Y_FIX_DONE = new HashMap<UUID, Boolean>();
	/** Client may extend settle once if still double-Y when the first window ends. */
	private static final Map<UUID, Boolean> LEAVE_Y_EXTEND_USED = new HashMap<UUID, Boolean>();
	/** True if double-Y was seen at any point during the current leave settle window. */
	private static final Map<UUID, Boolean> LEAVE_Y_SAW_HIGH = new HashMap<UUID, Boolean>();
	/** One MOVE_Y_GATE proof log per leave UUID. */
	private static final Map<UUID, Boolean> MOVE_Y_GATE_LOGGED = new HashMap<UUID, Boolean>();
	/** At most one PacketLeaveStand settle-repin per leave settle window. */
	private static final Map<UUID, Boolean> LEAVE_REPIN_SENT = new HashMap<UUID, Boolean>();
	/** Deduplicate leaveNestPhysics when both PlayerHelper + LegendsProperties fire. */
	private static final Map<UUID, Integer> LEAVE_PHYSICS_TICK = new HashMap<UUID, Integer>();

	/** Client/server post-leave ticks that keep re-asserting standing yOffset/eyeH/SCALE (not noClip). */
	public static final int POST_LEAVE_STANDING_TICKS = 40;
	/** Max client leave-lock ticks before forced clear (safety). */
	public static final int LEAVE_LOCK_MAX_TICKS = 200;
	/** Clear leave lock after this many ticks with stable matching Y on stand floor. */
	private static final int LEAVE_LOCK_GOOD_TICKS = 40;
	private static final String NBT_LEAVE_LOCK_GOOD = "BvLeaveLockGood";

	/** Forge/Legends standing eyeHeight (not yOffset). */
	public static final float STANDING_EYE_HEIGHT = 0.12F;
	public static final float STANDING_Y_OFFSET = 1.62F;
	/** Same Y Legends uses when placing the player in a coffin ({@code blockY + 0.9375}). */
	public static final float LEGENDS_COFFIN_Y = 0.9375F;

	/** Block Y of the player's feet (AABB minY), not entity/posY. */
	public static int getFeetBlockY(EntityPlayer player) {
		if (player == null) return 0;
		AxisAlignedBB bb = player.boundingBox;
		if (bb != null) {
			return MathHelper.floor_double(bb.minY + 0.01D);
		}
		float yOff = player.yOffset >= 0.5F ? player.yOffset : STANDING_Y_OFFSET;
		return MathHelper.floor_double(player.posY - yOff);
	}

	public static int getFeetBlockX(EntityPlayer player) {
		return player == null ? 0 : MathHelper.floor_double(player.posX);
	}

	public static int getFeetBlockZ(EntityPlayer player) {
		return player == null ? 0 : MathHelper.floor_double(player.posZ);
	}

	private static Method setSizeMethod;

	private CoffinSleepUtil() {}

	public static boolean isExitAllowed() {
		return Boolean.TRUE.equals(ALLOW_EXIT.get());
	}

	/** Any Horror coffin class (all colors), open or closed. */
	public static boolean isLegendsCoffinBlock(Block block) {
		if (block == null || !Loader.isModLoaded("legends")) return false;
		if (isKnownRegisterCoffin(block)) return true;
		String name = block.getClass().getName();
		return name.startsWith("com.tihyo.horror.blocks.BlockCoffin");
	}

	private static boolean isKnownRegisterCoffin(Block block) {
		try {
			return block == RegisterBlocksHorror.coffin
					|| block == RegisterBlocksHorror.coffinOpen
					|| block == RegisterBlocksHorror.coffinBlack
					|| block == RegisterBlocksHorror.coffinBlackOpen
					|| block == RegisterBlocksHorror.coffinGray
					|| block == RegisterBlocksHorror.coffinGrayOpen
					|| block == RegisterBlocksHorror.coffinRed
					|| block == RegisterBlocksHorror.coffinRedOpen
					|| block == RegisterBlocksHorror.coffinBlue
					|| block == RegisterBlocksHorror.coffinBlueOpen
					|| block == RegisterBlocksHorror.coffinPink
					|| block == RegisterBlocksHorror.coffinPinkOpen;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Open lid: prefer {@code opened} field on BlockCoffin*, else known *Open registry entries,
	 * else class/registry name contains "Open".
	 */
	public static boolean isOpenCoffin(Block block) {
		if (!isLegendsCoffinBlock(block)) return false;
		Boolean opened = readOpenedField(block);
		if (opened != null) return opened.booleanValue();
		try {
			if (block == RegisterBlocksHorror.coffinOpen
					|| block == RegisterBlocksHorror.coffinBlackOpen
					|| block == RegisterBlocksHorror.coffinGrayOpen
					|| block == RegisterBlocksHorror.coffinRedOpen
					|| block == RegisterBlocksHorror.coffinBlueOpen
					|| block == RegisterBlocksHorror.coffinPinkOpen) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		String reg = null;
		try {
			reg = Block.blockRegistry.getNameForObject(block);
		} catch (Throwable ignored) {
		}
		String cls = block.getClass().getSimpleName();
		return (reg != null && reg.toLowerCase().contains("open"))
				|| cls.toLowerCase().contains("open");
	}

	public static boolean isClosedCoffin(Block block) {
		return isLegendsCoffinBlock(block) && !isOpenCoffin(block);
	}

	private static Boolean readOpenedField(Block block) {
		try {
			Class<?> cls = block.getClass();
			Field f = OPENED_FIELD_CACHE.get(cls);
			if (f == null && !OPENED_FIELD_CACHE.containsKey(cls)) {
				try {
					f = cls.getDeclaredField("opened");
					f.setAccessible(true);
				} catch (NoSuchFieldException e) {
					f = null;
				}
				OPENED_FIELD_CACHE.put(cls, f);
			}
			if (f == null) return null;
			return Boolean.valueOf(f.getBoolean(block));
		} catch (Throwable t) {
			return null;
		}
	}

	public static boolean isAnyModCoffin(Block block) {
		if (isLegendsCoffinBlock(block)) return true;
		if (Loader.isModLoaded("witchery") && block == com.emoniph.witchery.Witchery.Blocks.COFFIN) return true;
		if (Loader.isModLoaded("vampirism") && VampirismUtil.isCoffin(block)) return true;
		return false;
	}

	public static boolean isInCoffin(EntityPlayer player) {
		if (player == null || player.worldObj == null) return false;
		if (!Loader.isModLoaded("legends")) return false;
		return PlayerHelper.isInCoffin(player);
	}

	/** True if nested and the bed coordinate from that click has a closed lid. */
	public static boolean isInClosedLegendsCoffin(EntityPlayer player) {
		if (!isInCoffin(player)) return false;
		ChunkCoordinates bed = bedLocation(player);
		if (bed == null || player.worldObj == null) return false;
		Block b = player.worldObj.getBlock(bed.posX, bed.posY, bed.posZ);
		return isClosedCoffin(b);
	}

	/** Bed set by {@link #bindCoffinSpawn}. Not a second stored nest. */
	public static ChunkCoordinates bedLocation(EntityPlayer player) {
		if (player == null) return null;
		return player.playerLocation;
	}

	public static String describeNestState(EntityPlayer player) {
		if (player == null || player.worldObj == null) return "bed=none";
		ChunkCoordinates bed = bedLocation(player);
		if (bed == null) return "bed=none";
		Block b = player.worldObj.getBlock(bed.posX, bed.posY, bed.posZ);
		return "bed=" + bed.posX + "," + bed.posY + "," + bed.posZ
				+ " block=" + (b == null ? "null" : b.getClass().getSimpleName())
				+ " open=" + isOpenCoffin(b) + " closed=" + isClosedCoffin(b);
	}

	/** Call after successful enter (setInCoffin true). Does not move the player. */
	public static void onEnterCoffin(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		clearClientLeaveLock(player);
		clearLeaveStand(player);
		if (player.worldObj.isRemote) {
			CoffinPoseDebug.dump(player, "ENTER");
			return;
		}
		clearAndSyncVExtLeaveStand(player);
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		if (vPlayer != null) {
			vPlayer.setInteger(PlayerKeys.SLEEP_START, (int) player.worldObj.getWorldTime());
			vPlayer.syncClient();
		}
		CoffinPoseDebug.dump(player, "ENTER");
	}

	/**
	 * Client leave edge: collision only. Does not write motion, fall, rotation, size, or offsets.
	 */
	public static void applyPostLeaveStanding(EntityPlayer player) {
		if (player == null) return;
		player.noClip = false;
	}

	/**
	 * Keep the lying model aligned with the coffin while camera yaw stays free.
	 * Does not write rotationYaw / rotationYawHead / rotationPitch.
	 */
	public static void pinNestBodyFacing(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("legends") || !isInCoffin(player)) return;
		applyNestFacing(player);
	}

	private static void applyNestFacing(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("legends")) return;
		try {
			Object fr = com.tihyo.legends.management.properties.LegendsProperties.FORCED_ROTATION.get(player);
			if (!(fr instanceof Float)) return;
			float facing = ((Float) fr).floatValue();
			player.renderYawOffset = facing;
			player.prevRenderYawOffset = facing;
		} catch (Throwable ignored) {
		}
	}

	/** Kill interp/packet spikes after a nest soft-pin / leave absorb. */
	public static void syncNestInterp(EntityPlayer player) {
		if (player == null) return;
		player.prevPosX = player.posX;
		player.prevPosY = player.posY;
		player.prevPosZ = player.posZ;
		player.lastTickPosX = player.posX;
		player.lastTickPosY = player.posY;
		player.lastTickPosZ = player.posZ;
	}

	/**
	 * Server PlayerTick START while settling: countdown only.
	 * Does not rewrite yOffset or size after the one {@code setPlayerLocation}.
	 * Clears VExtendedPlayer leave-stand on settle end / walk-away / re-enter.
	 */
	public static void tickPostLeaveSettleStart(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		UUID id = player.getUniqueID();
		Integer left = POST_LEAVE_TICKS.get(id);
		if (left == null) return;
		if (isInCoffin(player)) {
			POST_LEAVE_TICKS.remove(id);
			clearLeaveStandServerMaps(player);
			LEAVE_REPIN_SENT.remove(id);
			clearAndSyncVExtLeaveStand(player);
			return;
		}
		player.noClip = false;

		if (maybeClearLeaveStandWalkAwayServer(player)) {
			POST_LEAVE_TICKS.remove(id);
			LEAVE_REPIN_SENT.remove(id);
			player.noClip = false;
			return;
		}

		int n = left.intValue() - 1;
		if (n <= 0) {
			POST_LEAVE_TICKS.remove(id);
			clearLeaveStandServerMaps(player);
			LEAVE_REPIN_SENT.remove(id);
			clearAndSyncVExtLeaveStand(player);
			player.noClip = false;
		} else {
			POST_LEAVE_TICKS.put(id, Integer.valueOf(n));
		}
	}

	/** Persist leave-stand on VExtendedPlayer and sync to clients. */
	public static void syncVExtLeaveStand(EntityPlayer player, ChunkCoordinates stand) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote || stand == null) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;
		v.setLeaveStand(stand.posX, stand.posY, stand.posZ);
		v.syncClient();
	}

	/** Clear VExtendedPlayer leave-stand and sync. */
	public static void clearAndSyncVExtLeaveStand(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;
		if (!v.getBoolean(PlayerKeys.LEAVE_STANDING) && v.getLeaveStand() == null) return;
		v.clearLeaveStand();
		v.syncClient();
		CoffinPoseDebug.dump(player, "LEAVE_STAND_CLEAR_SERVER");
	}

	/** Server: drop leave-stand once player walks ≥3 blocks away (XZ). */
	private static boolean maybeClearLeaveStandWalkAwayServer(EntityPlayer player) {
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null || !v.getBoolean(PlayerKeys.LEAVE_STANDING)) return false;
		ChunkCoordinates stand = v.getLeaveStand();
		if (stand == null) stand = getPostLeaveStand(player);
		if (stand == null) return false;
		double dx = Math.abs(player.posX - (stand.posX + 0.5D));
		double dz = Math.abs(player.posZ - (stand.posZ + 0.5D));
		if (dx < 3.0D && dz < 3.0D) return false;
		clearLeaveStandServerMaps(player);
		clearAndSyncVExtLeaveStand(player);
		CoffinPoseDebug.dump(player, "LEAVE_STAND_OFF walkAway");
		return true;
	}

	/** Start settle window after nest leave (both sides). */
	public static void beginPostLeaveSettle(EntityPlayer player) {
		if (player == null) return;
		UUID id = player.getUniqueID();
		POST_LEAVE_TICKS.put(id, Integer.valueOf(POST_LEAVE_STANDING_TICKS));
		LEAVE_Y_FIX_DONE.remove(id);
		LEAVE_Y_EXTEND_USED.remove(id);
		LEAVE_Y_SAW_HIGH.remove(id);
		LEAVE_REPIN_SENT.remove(id);
	}

	/** Client leave protect: lock NBT, stand NBT/map, or settle countdown. */
	public static boolean isClientLeaveYProtect(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return false;
		if (isInCoffin(player)) return false;
		return isClientLeaveLocked(player) || getLeaveLockStand(player) != null || isPostLeaveSettling(player);
	}

	public static boolean isClientLeaveLocked(EntityPlayer player) {
		return player != null && player.getEntityData().getBoolean(NBT_LEAVE_LOCK);
	}

	/** Stand for leave-lock asserts (lock/stand NBT preferred over shared server map). */
	public static ChunkCoordinates getLeaveLockStand(EntityPlayer player) {
		if (player == null) return null;
		if (player.getEntityData().getBoolean(NBT_LEAVE_STAND)
				|| player.getEntityData().getBoolean(NBT_LEAVE_LOCK)) {
			ChunkCoordinates fromNbt = readLeaveStandNbt(player);
			if (fromNbt != null) return fromNbt;
		}
		return getPostLeaveStand(player);
	}

	/** Begin client-only leave lock (not cleared by server settle maps). */
	public static void beginClientLeaveLock(EntityPlayer player, ChunkCoordinates stand) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (stand != null) {
			storeLeaveStand(player, stand);
		}
		player.getEntityData().setBoolean(NBT_LEAVE_LOCK, true);
		player.getEntityData().setInteger(NBT_LEAVE_LOCK_TICK, 0);
		player.getEntityData().setInteger(NBT_LEAVE_LOCK_GOOD, 0);
		MOVE_Y_GATE_LOGGED.remove(player.getUniqueID());
		forceLegendsStandingOffsets(player);
		ChunkCoordinates lockStand = stand != null ? stand : getLeaveLockStand(player);
		CoffinPoseDebug.dump(player, "LEAVE_LOCK_ON"
				+ (lockStand == null ? "" : " stand=" + lockStand.posX + "," + lockStand.posY + "," + lockStand.posZ
						+ " " + describeStandFloor(player.worldObj, lockStand)));
	}

	/** Floor block under a leave-stand feet-air cell (for debug). */
	public static String describeStandFloor(World world, ChunkCoordinates stand) {
		if (world == null || stand == null) return "standFloor=?";
		Block floor = world.getBlock(stand.posX, stand.posY - 1, stand.posZ);
		Block at = world.getBlock(stand.posX, stand.posY, stand.posZ);
		String floorName = floor == null ? "null" : floor.getClass().getSimpleName();
		String atName = at == null ? "null" : at.getClass().getSimpleName();
		return "standFloor=" + floorName + "(coffin=" + isLegendsCoffinBlock(floor) + ")"
				+ " standAt=" + atName;
	}

	public static void clearClientLeaveLock(EntityPlayer player) {
		if (player == null) return;
		player.getEntityData().removeTag(NBT_LEAVE_LOCK);
		player.getEntityData().removeTag(NBT_LEAVE_LOCK_TICK);
		player.getEntityData().removeTag(NBT_LEAVE_LOCK_SEEN);
		player.getEntityData().removeTag(NBT_LEAVE_LOCK_GOOD);
		MOVE_Y_GATE_LOGGED.remove(player.getUniqueID());
	}

	/**
	 * Correct leave-lock double-Y using current XZ (do not pin back to stand center).
	 */
	public static void correctLeaveLockYOnly(EntityPlayer player, ChunkCoordinates stand) {
		if (player == null || stand == null) return;
		forceLegendsStandingOffsets(player);
		player.ySize = 0.0F;
		double goodY = stand.posY + STANDING_Y_OFFSET;
		player.setPosition(player.posX, goodY, player.posZ);
		syncNestInterp(player);
	}

	/**
	 * Incoming {@code setLocationAndAngles} Y is already entity Y (standFeet+yOffset),
	 * so vanilla {@code y + yOffset} would double-apply.
	 */
	public static boolean shouldAbsorbSetLocationEntityY(EntityPlayer player, double incomingY) {
		if (!isClientLeaveYProtect(player)) return false;
		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null) return false;
		double feetY = stand.posY;
		double goodEntityY = feetY + STANDING_Y_OFFSET;
		// Bare feet Y → let vanilla add yOffset.
		if (Math.abs(incomingY - feetY) < 0.25D) return false;
		// Already entity Y → absorb (do not add yOffset again).
		return Math.abs(incomingY - goodEntityY) < 0.25D;
	}

	/**
	 * Former double-Y S08 absorb — disabled. Leave uses one entity-Y S08; absorbing
	 * setPosition fought walk/jump after leave.
	 */
	public static boolean absorbLeaveSetPositionDoubleY(EntityPlayer player, double x, double y, double z) {
		return false;
	}

	/** Former double-Y absorb for {@code setPositionAndRotation} — disabled. */
	public static boolean absorbLeaveSetPositionAndRotationDoubleY(EntityPlayer player, double x, double y,
			double z, float yaw, float pitch) {
		return false;
	}

	/** Absorb must not leave the player permanently airborne at stand floor. */
	private static void restoreLeaveOnGroundIfStanding(EntityPlayer player, ChunkCoordinates stand) {
		if (player == null || stand == null) return;
		AxisAlignedBB bb = player.boundingBox;
		if (bb != null && Math.abs(bb.minY - stand.posY) < 0.25D) {
			player.onGround = true;
			player.motionY = 0.0D;
			player.fallDistance = 0.0F;
		}
	}

	public static void noteLeaveYSawHigh(EntityPlayer player) {
		if (player == null) return;
		if (isDoubleYOffsetPattern(player)) {
			LEAVE_Y_SAW_HIGH.put(player.getUniqueID(), Boolean.TRUE);
		}
	}

	public static boolean didLeaveYSawHigh(EntityPlayer player) {
		return player != null && Boolean.TRUE.equals(LEAVE_Y_SAW_HIGH.get(player.getUniqueID()));
	}

	public static boolean isPostLeaveSettling(EntityPlayer player) {
		return player != null && POST_LEAVE_TICKS.containsKey(player.getUniqueID());
	}

	/** Leave-stand block for settle re-pin / debug (map, VExtendedPlayer, else NBT). */
	public static ChunkCoordinates getPostLeaveStand(EntityPlayer player) {
		if (player == null) return null;
		ChunkCoordinates stand = POST_LEAVE_STAND.get(player.getUniqueID());
		if (stand != null) return stand;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v != null) {
			stand = v.getLeaveStand();
			if (stand != null) return stand;
		}
		return readLeaveStandNbt(player);
	}

	public static void storeLeaveStand(EntityPlayer player, ChunkCoordinates stand) {
		if (player == null || stand == null) return;
		POST_LEAVE_STAND.put(player.getUniqueID(), stand);
		player.getEntityData().setBoolean(NBT_LEAVE_STAND, true);
		player.getEntityData().setInteger(NBT_LEAVE_X, stand.posX);
		player.getEntityData().setInteger(NBT_LEAVE_Y, stand.posY);
		player.getEntityData().setInteger(NBT_LEAVE_Z, stand.posZ);
	}

	/** Server settle teardown: shared map only — never wipe client leave-lock NBT. */
	public static void clearLeaveStandServerMaps(EntityPlayer player) {
		if (player == null) return;
		UUID id = player.getUniqueID();
		POST_LEAVE_STAND.remove(id);
		LEAVE_Y_FIX_DONE.remove(id);
		LEAVE_REPIN_SENT.remove(id);
		if (player.worldObj != null && !player.worldObj.isRemote) {
			player.getEntityData().removeTag(NBT_LEAVE_STAND);
			player.getEntityData().removeTag(NBT_LEAVE_X);
			player.getEntityData().removeTag(NBT_LEAVE_Y);
			player.getEntityData().removeTag(NBT_LEAVE_Z);
		}
	}

	public static void clearLeaveStand(EntityPlayer player) {
		if (player == null) return;
		clearLeaveStandServerMaps(player);
		if (player.worldObj != null && player.worldObj.isRemote && !isClientLeaveLocked(player)) {
			player.getEntityData().removeTag(NBT_LEAVE_STAND);
			player.getEntityData().removeTag(NBT_LEAVE_X);
			player.getEntityData().removeTag(NBT_LEAVE_Y);
			player.getEntityData().removeTag(NBT_LEAVE_Z);
		}
	}

	private static ChunkCoordinates readLeaveStandNbt(EntityPlayer player) {
		if (player == null) return null;
		if (!player.getEntityData().getBoolean(NBT_LEAVE_STAND)
				&& !player.getEntityData().getBoolean(NBT_LEAVE_LOCK)) {
			return null;
		}
		if (!player.getEntityData().hasKey(NBT_LEAVE_X)) return null;
		return new ChunkCoordinates(
				player.getEntityData().getInteger(NBT_LEAVE_X),
				player.getEntityData().getInteger(NBT_LEAVE_Y),
				player.getEntityData().getInteger(NBT_LEAVE_Z));
	}

	/** yOffset + eyeHeight + SCALE only (does not touch noClip/position). */
	public static void forceLegendsStandingOffsets(EntityPlayer player) {
		if (player == null) return;
		player.yOffset = STANDING_Y_OFFSET;
		player.eyeHeight = STANDING_EYE_HEIGHT;
		resetLegendsScale(player);
	}

	/**
	 * Client post-leave tick: offsets + leave-lock AABB/pos assert (no noClip).
	 * Call on ClientTick START/END and LivingUpdate while locked.
	 */
	public static void tickClientPostLeave(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (maybeClearLeaveStandOnJump(player)) return;
		assertClientLeaveLockPose(player);
		maybeClearLeaveStandWalkAway(player);
	}

	/**
	 * Intentional jump: drop leave-lock only so AABB clamps cannot fight gravity.
	 * Keep leave-stand NBT until walk-away.
	 * @return true if lock was cleared
	 */
	public static boolean maybeClearLeaveStandOnJump(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return false;
		if (isInCoffin(player)) return false;
		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null && !isClientLeaveLocked(player)) return false;
		double goodY = stand != null ? stand.posY + STANDING_Y_OFFSET : player.posY;
		boolean jumped = player.motionY > 0.2D;
		boolean airborneHigh = !player.onGround && player.posY > goodY + 0.75D;
		if (!jumped && !airborneHigh) return false;
		clearClientLeaveLock(player);
		CoffinPoseDebug.dump(player, "LEAVE_STAND_OFF jump"
				+ " motionY=" + String.format("%.3f", player.motionY)
				+ " posY=" + String.format("%.3f", player.posY));
		return true;
	}

	/**
	 * Client: local leave-stand cleanup when walked away; server authoritative clear
	 * happens in {@link #tickPostLeaveSettleStart}.
	 */
	public static void maybeClearLeaveStandWalkAway(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (isClientLeaveLocked(player) || isInCoffin(player)) return;
		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null) return;
		double dx = Math.abs(player.posX - (stand.posX + 0.5D));
		double dz = Math.abs(player.posZ - (stand.posZ + 0.5D));
		if (dx < 3.0D && dz < 3.0D) return;
		clearLeaveStand(player);
		CoffinPoseDebug.dump(player, "LEAVE_STAND_OFF walkAway");
	}

	/**
	 * Force standing offsets and rebuild pose when AABB feet sit on entity Y / pos is high.
	 * @return true if a snap fired
	 */
	public static boolean assertClientLeaveLockPose(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return false;
		if (isInCoffin(player)) return false;
		if (!isClientLeaveLocked(player) && !isClientLeaveYProtect(player)) return false;

		player.noClip = false;
		maybeClearClientLeaveLock(player);
		return false;
	}

	/**
	 * Release leave lock only when Y is stably matching stand (no false walkAway).
	 * Keeps leave-stand NBT so NetHandler double-Y absorb continues after lock ends.
	 */
	public static void maybeClearClientLeaveLock(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (!isClientLeaveLocked(player)) return;
		// Assert can fire multiple times per game tick — advance lock age once per ticksExisted.
		int age = player.getEntityData().getInteger(NBT_LEAVE_LOCK_TICK);
		int seen = player.getEntityData().getInteger(NBT_LEAVE_LOCK_SEEN);
		boolean newTick = seen != player.ticksExisted;
		if (newTick) {
			age++;
			player.getEntityData().setInteger(NBT_LEAVE_LOCK_TICK, age);
			player.getEntityData().setInteger(NBT_LEAVE_LOCK_SEEN, player.ticksExisted);
		}

		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null) {
			clearClientLeaveLock(player);
			CoffinPoseDebug.dump(player, "LEAVE_LOCK_OFF noStand");
			return;
		}
		if (age >= LEAVE_LOCK_MAX_TICKS) {
			clearClientLeaveLock(player);
			CoffinPoseDebug.dump(player, "LEAVE_LOCK_OFF maxTicks");
			return;
		}
		AxisAlignedBB bb = player.boundingBox;
		double goodY = stand.posY + STANDING_Y_OFFSET;
		boolean feetOnFloor = bb != null && Math.abs(bb.minY - stand.posY) < 0.25D;
		boolean yMatches = Math.abs(player.posY - goodY) < 0.15D;
		boolean poseOk = feetOnFloor && yMatches && !isDoubleYOffsetPattern(player);
		int good = player.getEntityData().getInteger(NBT_LEAVE_LOCK_GOOD);
		if (poseOk) {
			if (newTick) {
				good++;
				player.getEntityData().setInteger(NBT_LEAVE_LOCK_GOOD, good);
			}
			if (good >= LEAVE_LOCK_GOOD_TICKS) {
				clearClientLeaveLock(player);
				CoffinPoseDebug.dump(player, "LEAVE_LOCK_OFF stableY");
			}
		} else if (newTick) {
			player.getEntityData().setInteger(NBT_LEAVE_LOCK_GOOD, 0);
		}
	}

	public static void endClientPostLeave(EntityPlayer player) {
		if (player == null) return;
		player.noClip = false;
		UUID id = player.getUniqueID();
		POST_LEAVE_TICKS.remove(id);
		LEAVE_Y_EXTEND_USED.remove(id);
		LEAVE_Y_SAW_HIGH.remove(id);
		// Keep BvLeaveLock + stand NBT until geometry/walk-away clears them.
	}

	/**
	 * True when client entity Y is exactly standFeet+2×yOffset (S08 double-offset).
	 * Does not treat normal jumps (higher Y) as double-Y.
	 */
	public static boolean isDoubleYOffsetPattern(EntityPlayer player) {
		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null || player == null) return false;
		double goodPosY = stand.posY + STANDING_Y_OFFSET;
		return Math.abs(player.posY - (goodPosY + STANDING_Y_OFFSET)) < 0.25D;
	}

	/**
	 * End of the client leave window. Does not move the player or rewrite fall, motion, or offsets.
	 */
	public static int tryExtendClientLeaveIfHigh(EntityPlayer player) {
		endClientPostLeave(player);
		return 0;
	}

	/**
	 * If client feet sit on standFeet+yOffset (double yOffset), snap to standFeet+yOffset.
	 * Re-applies whenever the high pattern returns (Legends/interp re-raises after leave).
	 * Client snaps keep motion so correction does not feel frozen.
	 */
	public static boolean tryFixDoubleYOffset(EntityPlayer player) {
		if (player == null) return false;
		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null) return false;
		if (!isDoubleYOffsetPattern(player)) return false;

		double goodPosY = stand.posY + STANDING_Y_OFFSET;
		boolean remote = player.worldObj != null && player.worldObj.isRemote;
		if (remote) {
			correctLeaveLockYOnly(player, stand);
		} else {
			PhysicsUtil.teleportToSafeStand(player, stand, true);
			syncNestInterp(player);
		}
		UUID id = player.getUniqueID();
		// Throttle identical dumps; still re-snap every time pattern appears.
		if (!Boolean.TRUE.equals(LEAVE_Y_FIX_DONE.get(id))) {
			LEAVE_Y_FIX_DONE.put(id, Boolean.TRUE);
			CoffinPoseDebug.dump(player, "LEAVE_Y_FIX targetY=" + String.format("%.3f", goodPosY));
		}
		return true;
	}

	/**
	 * After setPosition while leave-protected: correct only exact stand+2×yOffset.
	 * Skips while jumping so harden cannot fight gravity.
	 */
	public static void hardenLeaveSetPosition(EntityPlayer player) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote) return;
		if (!isClientLeaveYProtect(player)) return;
		if (player.motionY > 0.1D) return;
		ChunkCoordinates stand = getLeaveLockStand(player);
		if (stand == null) return;
		if (!isDoubleYOffsetPattern(player)) return;
		forceLegendsStandingOffsets(player);
		player.ySize = 0.0F;
		double goodY = stand.posY + STANDING_Y_OFFSET;
		correctLeaveLockYOnly(player, stand);
		CoffinPoseDebug.dumpLeaveLockSnap(player, goodY);
	}

	/** True when AABB is under the leave-stand floor (buried). */
	public static boolean isLeaveStandSunk(EntityPlayer player) {
		ChunkCoordinates stand = getPostLeaveStand(player);
		if (stand == null || player == null) return false;
		AxisAlignedBB bb = player.boundingBox;
		return bb != null && bb.minY < stand.posY - 0.1D;
	}

	/** Stand target entity Y (feet air + yOffset). */
	public static double getLeaveStandTargetY(EntityPlayer player) {
		ChunkCoordinates stand = getPostLeaveStand(player);
		if (stand == null) return Double.NaN;
		return stand.posY + STANDING_Y_OFFSET;
	}

	/**
	 * Re-pin only when sunk under stand floor (never near+drifted yank).
	 * VExtendedPlayer sync is one-shot per settle via {@link #LEAVE_REPIN_SENT}.
	 * @return true if a snap fired
	 */
	private static boolean maybeRepinLeaveStand(EntityPlayer player, boolean sendPacket) {
		ChunkCoordinates stand = getPostLeaveStand(player);
		if (stand == null) return false;
		AxisAlignedBB bb = player.boundingBox;
		boolean sunk = bb != null && bb.minY < stand.posY - 0.1D;
		if (!sunk) return false;

		forceLegendsStandingOffsets(player);
		invokeSetSize(player, 0.6F, 1.8F);
		PhysicsUtil.teleportToSafeStand(player, stand);
		syncNestInterp(player);
		if (sendPacket && player instanceof EntityPlayerMP) {
			EntityPlayerMP mp = (EntityPlayerMP) player;
			LEAVE_REPIN_SENT.put(mp.getUniqueID(), Boolean.TRUE);
			CoffinPoseDebug.dumpS08Skipped(player, "repin", mp.posY);
			syncVExtLeaveStand(mp, stand);
		}
		CoffinPoseDebug.dump(player, "SETTLE_REPIN sunk=true"
				+ " stand=" + stand.posX + "," + stand.posY + "," + stand.posZ
				+ " packet=" + sendPacket);
		return true;
	}

	/**
	 * @deprecated Settle physics is START-only via {@link #tickPostLeaveSettleStart}.
	 */
	public static void tickPostLeaveSettle(EntityPlayer player) {
		tickPostLeaveSettleStart(player);
	}

	public static void bindCoffinSpawn(EntityPlayer player, int x, int y, int z) {
		ChunkCoordinates bed = new ChunkCoordinates(x, y, z);
		player.playerLocation = bed;
		player.setSpawnChunk(bed, false);
	}

	/** Drop the bed coordinate when it still points at the coffin they left. */
	public static void clearCoffinBedLocation(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		ChunkCoordinates bed = player.playerLocation;
		if (bed == null) return;
		Block block = player.worldObj.getBlock(bed.posX, bed.posY, bed.posZ);
		if (!isLegendsCoffinBlock(block)) return;
		player.playerLocation = null;
	}

	/**
	 * Space while sealed: skip day→night, stay inside. No wakeUpPlayer / exit.
	 */
	public static boolean tryPassDay(EntityPlayer player) {
		CoffinDebug.log(player, "tryPassDay begin " + describeNestState(player));
		if (player.worldObj.isRemote) return false;
		if (!GeneralUtil.isVampire(player)) {
			CoffinDebug.logImportant(player, "DENY not vampire");
			msg(player, "Only vampires rest in a coffin.", EnumChatFormatting.RED);
			return false;
		}
		if (!isInCoffin(player)) {
			CoffinDebug.logImportant(player, "DENY not in coffin");
			msg(player, "You must be inside a coffin.", EnumChatFormatting.RED);
			return false;
		}

		if (player.playerLocation == null) {
			CoffinDebug.logImportant(player, "DENY no bed");
			msg(player, "You must be inside a coffin.", EnumChatFormatting.RED);
			return false;
		}

		if (!isInClosedLegendsCoffin(player)) {
			CoffinDebug.logImportant(player, "DENY lid open " + describeNestState(player));
			msg(player, "Close the coffin lid before resting.", EnumChatFormatting.RED);
			return false;
		}
		if (!player.worldObj.isDaytime()) {
			CoffinDebug.logImportant(player, "DENY not daytime time=" + (player.worldObj.getWorldTime() % 24000L));
			msg(player, "You can only pass the day while the sun is up.", EnumChatFormatting.RED);
			return false;
		}
		if (!hasBloodToRest(player)) {
			CoffinDebug.logImportant(player, "DENY low blood");
			msg(player, "You are too drained to rest.", EnumChatFormatting.RED);
			return false;
		}

		int suspicion = SuspicionUtil.getSuspicion(player);
		SuspicionUtil.checkForSuspicionWitnesses(player, 1500, 128);
		if (suspicion >= 5100) {
			WitcheryUtil.spawnHunter(player, true);
			CoffinDebug.logImportant(player, "DENY high suspicion=" + suspicion);
			msg(player, "Hunters found you while resting.", EnumChatFormatting.RED);
			return false;
		}

		World world = player.worldObj;
		long t = world.getWorldTime();
		long newTime = t - t % 24000L + 13000L;
		world.setWorldTime(newTime);

		ChunkCoordinates pos = player.playerLocation;
		if (pos != null) {
			bindCoffinSpawn(player, pos.posX, pos.posY, pos.posZ);
		}

		onVampireRestComplete(player);
		CoffinDebug.logImportant(player, "PASS_DAY ok -> time " + (newTime % 24000L) + " still in coffin");
		msg(player, "Night falls. You remain sealed in your coffin.", EnumChatFormatting.DARK_RED);
		return true;
	}

	public static boolean hasBloodToRest(EntityPlayer player) {
		if (!Loader.isModLoaded("witchery")) return true;
		ExtendedPlayer ePlayer = ExtendedPlayer.get(player);
		if (ePlayer == null) return true;
		int max = ePlayer.getMaxBloodPower();
		if (max <= 0) return true;
		return ePlayer.getBloodPower() > (int) (max * 0.3F);
	}

	public static void onVampireRestComplete(EntityPlayer player) {
		WitcheryUtil.decreaseBloodToCap(player, 0, 0.3F, 0.3F);

		Village village = GeneralUtil.getClosestVillage(player, 90);
		boolean canSeeSky = VampireSunRules.canSeeSky(player);
		boolean safeNest = village == null && !canSeeSky;

		if (safeNest) {
			msg(player, "Your rest goes undisturbed.", EnumChatFormatting.GRAY);
			return;
		}

		if (village != null) {
			int rep = village.getReputationForPlayer(player.getCommandSenderName());
			int bump = rep > 0 ? 150 : 300;
			SuspicionUtil.increaseSuspicion(player, bump);
			SuspicionUtil.checkForSuspicionWitnesses(player, 0, 128, "", false);
			int suspicion = SuspicionUtil.getSuspicion(player);
			sendRumor(player, suspicion);
		} else {
			SuspicionUtil.increaseSuspicion(player, 150);
			msg(player, "Something in the night notes your resting place.", EnumChatFormatting.RED);
		}
	}

	private static void sendRumor(EntityPlayer player, int suspicion) {
		if (suspicion >= 5100) {
			return;
		} else if (suspicion >= 4000) {
			msg(player, "The mortals suspect a vampire walks among them", EnumChatFormatting.RED);
		} else if (suspicion >= 3000) {
			msg(player, "You can smell their fear", EnumChatFormatting.RED);
		} else if (suspicion >= 1500) {
			msg(player, "There have been rumors about blood-draining...", EnumChatFormatting.RED);
		} else {
			msg(player, "The nearby mortals find no trace of your true nature", EnumChatFormatting.RED);
		}
	}

	/**
	 * Lid opened while nested: mimic wakeUpPlayer leave + clear isInCoffin.
	 */
	public static void exitCoffin(EntityPlayer player) {
		if (player.worldObj.isRemote) return;
		CoffinDebug.logImportant(player, "EXIT " + describeNestState(player));
		CoffinPoseDebug.dump(player, "EXIT_BEFORE");
		CoffinPoseDebug.markExit();

		ALLOW_EXIT.set(Boolean.TRUE);
		try {
			leaveNestPhysics(player, true);
			CoffinPoseDebug.dump(player, "EXIT_AFTER_SIZE");

			if (Loader.isModLoaded("legends")) {
				PlayerHelper.setInCoffin(player, false, 0.0F);
			}

			CoffinPoseDebug.dump(player, "EXIT_DONE");
		} finally {
			ALLOW_EXIT.remove();
		}
	}

	/**
	 * When Legends clears {@code IS_IN_COFFIN} without going through {@link #exitCoffin}
	 * (client already sees inCoffin=false). Restores standing pose and optionally
	 * teleports beside the nest.
	 */
	public static void onNestFlagCleared(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		clearCoffinBedLocation(player);
		if (isExitAllowed()) return;
		CoffinPoseDebug.markExit();
		CoffinPoseDebug.dump(player, "FORCED_LEAVE");
		leaveNestPhysics(player, true);
	}

	/** True if leaveNestPhysics already ran this entity tick (PlayerHelper + props double-fire). */
	private static boolean markLeavePhysicsThisTick(EntityPlayer player) {
		if (player == null) return true;
		UUID id = player.getUniqueID();
		Integer last = LEAVE_PHYSICS_TICK.get(id);
		if (last != null && last.intValue() == player.ticksExisted) {
			return true;
		}
		LEAVE_PHYSICS_TICK.put(id, Integer.valueOf(player.ticksExisted));
		return false;
	}

	/**
	 * Client: store the server leave stand and standing offsets. Position comes only from
	 * vanilla {@code setPlayerLocation} — do not lock or teleport again.
	 */
	public static void applyLeaveStandFromServer(EntityPlayer player, ChunkCoordinates stand) {
		if (player == null || player.worldObj == null || !player.worldObj.isRemote || stand == null) return;
		ChunkCoordinates cur = getLeaveLockStand(player);
		boolean sameStand = cur != null
				&& cur.posX == stand.posX && cur.posY == stand.posY && cur.posZ == stand.posZ;
		if (sameStand && player.getEntityData().getBoolean(NBT_LEAVE_STAND)) {
			CoffinPoseDebug.dumpApplyLeaveStandSkip(player, stand);
			return;
		}
		storeLeaveStand(player, stand);
		forceLegendsStandingOffsets(player);
		player.ySize = 0.0F;
		player.noClip = false;
		CoffinPoseDebug.dump(player, "APPLY_LEAVE_STAND stand=" + stand.posX + "," + stand.posY + "," + stand.posZ
				+ " targetY=" + String.format("%.3f", stand.posY + STANDING_Y_OFFSET)
				+ " " + describeStandFloor(player.worldObj, stand));
	}

	/**
	 * Exit does not change position, rotation, motion, or fallDistance.
	 * Legends already writes rotation. {@code noClip} is cleared so the nest pose
	 * does not keep the player phased after the coffin flag is false.
	 */
	public static void leaveNestPhysics(EntityPlayer player, boolean snapStand) {
		if (player == null || player.worldObj == null) return;

		boolean duplicate = markLeavePhysicsThisTick(player);
		if (duplicate) {
			player.noClip = false;
			CoffinPoseDebug.dump(player, "LEAVE_DEDUPED");
			return;
		}

		CoffinPoseDebug.noteNoClip(player, "LEAVE_EDGE_BEFORE");
		CoffinPoseDebug.dump(player, "LEAVE_EDGE_BEFORE");
		player.noClip = false;
		CoffinPoseDebug.noteNoClip(player, "LEAVE_EDGE_AFTER");
		CoffinPoseDebug.dump(player, "LEAVE_EDGE_AFTER snapStand=" + snapStand);
	}

	/**
	 * Prefer a clear adjacent 2-high stand with solid floor (mist rules), else null
	 * so caller can mist-search a wider radius.
	 */
	public static ChunkCoordinates findLeaveStand(World world, ChunkCoordinates nest, EntityPlayer player) {
		if (world == null || nest == null) {
			return null;
		}
		Block block = world.getBlock(nest.posX, nest.posY, nest.posZ);
		if (block != null && block.isBed(world, nest.posX, nest.posY, nest.posZ, player)) {
			try {
				block.setBedOccupied(world, nest.posX, nest.posY, nest.posZ, player, false);
			} catch (Throwable ignored) {
			}
			ChunkCoordinates spawn = block.getBedSpawnPosition(world, nest.posX, nest.posY, nest.posZ, player);
			if (spawn != null && isStandClear(world, player, spawn.posX, spawn.posY, spawn.posZ, nest)) {
				return spawn;
			}
		}

		int[][] xz = new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
				{ 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
		// Prefer nestY+1 first so AABB feet are above coffin top when adjacent.
		int[] dys = new int[] { 1, 0, 2, -1 };
		for (int di = 0; di < dys.length; di++) {
			int dy = dys[di];
			for (int i = 0; i < xz.length; i++) {
				int x = nest.posX + xz[i][0];
				int y = nest.posY + dy;
				int z = nest.posZ + xz[i][1];
				if (isStandClear(world, player, x, y, z, nest)) {
					return new ChunkCoordinates(x, y, z);
				}
			}
		}
		return PhysicsUtil.findSafeStandNear(world, player,
				nest.posX, nest.posY, nest.posZ, 5,
				nest.posX, nest.posY, nest.posZ);
	}

	/** Mist rules: 2-high air, non-coffin floor, empty collision AABB, not on coffin column. */
	private static boolean isStandClear(World world, EntityPlayer player, int x, int y, int z,
			ChunkCoordinates nest) {
		if (!PhysicsUtil.isTwoHighStand(world, x, y, z)) return false;
		Block floor = world.getBlock(x, y - 1, z);
		if (isLegendsCoffinBlock(floor)) return false;
		if (isLegendsCoffinBlock(world.getBlock(x, y, z))
				|| isLegendsCoffinBlock(world.getBlock(x, y + 1, z))) {
			return false;
		}
		// Reject other halves of a 2-block coffin (any legends coffin in this column).
		for (int cy = y - 1; cy <= y + 2; cy++) {
			if (isLegendsCoffinBlock(world.getBlock(x, cy, z))) return false;
		}
		if (nest != null && x == nest.posX && z == nest.posZ) return false;
		double half = 0.3D;
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
				x + 0.5D - half, (double) y, z + 0.5D - half,
				x + 0.5D + half, (double) y + 1.8D, z + 0.5D + half);
		boolean wasNoClip = player != null && player.noClip;
		if (player != null) player.noClip = false;
		try {
			return world.getCollidingBoundingBoxes(player, box).isEmpty();
		} finally {
			if (player != null) player.noClip = wasNoClip;
		}
	}

	public static void restoreStandingSize(EntityPlayer player) {
		invokeSetSize(player, 0.6F, 1.8F);
		assertStandingOffsets(player);
	}

	/**
	 * Re-assert standing offsets without touching position (client post-leave loop).
	 */
	public static void assertStandingOffsets(EntityPlayer player) {
		if (player == null) return;
		player.noClip = false;
		forceLegendsStandingOffsets(player);
	}

	/** Reset Legends SCALE so onPlayerTick eyeHeight formula stays at standing 0.12. */
	public static void resetLegendsScale(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("legends")) return;
		try {
			float scale = PlayerHelper.getDefaultSize(player);
			com.tihyo.legends.management.properties.LegendsProperties.SCALE.set(player, Float.valueOf(scale));
		} catch (Throwable ignored) {
		}
	}

	public static void clearForcedRotation(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		try {
			com.tihyo.legends.management.properties.LegendsProperties.FORCED_ROTATION.set(player, Float.valueOf(0.0F));
		} catch (Throwable ignored) {
		}
	}

	/**
	 * {@link PlayerHelper#isLying} without treating {@code isInCoffin} as lying.
	 * Used so nest does not trip Legends {@code cantMove} / motion÷100; real lying/bath still count.
	 */
	public static boolean isLyingIgnoringCoffin(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("legends")) return false;
		try {
			String tag = PlayerHelper.getActionTag(player);
			if ("lying".equals(tag)) return true;
			return PlayerHelper.isInAdamantiumBath(player);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Strip nest leftovers from stock Legends (slowness / grounded / action / FR / SCALE / coffin flag).
	 */
	public static void clearLegendsNestSideEffects(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("legends")) return;
		try {
			if (com.tihyo.legends.effects.LegendsEffects.hasSlowness(player)) {
				player.removePotionEffect(com.tihyo.legends.effects.LegendsEffects.slowness.id);
			}
		} catch (Throwable ignored) {
		}
		try {
			PlayerHelper.setGrounded(player, 0);
		} catch (Throwable ignored) {
		}
		try {
			PlayerHelper.setAction(player, "none");
		} catch (Throwable ignored) {
		}
		clearForcedRotation(player);
		resetLegendsScale(player);
		// Ensure coffin flag is false and syncs (packet path may have already cleared locally).
		try {
			if (PlayerHelper.isInCoffin(player)) {
				boolean nestedAllow = isExitAllowed();
				if (!nestedAllow) {
					ALLOW_EXIT.set(Boolean.TRUE);
				}
				try {
					PlayerHelper.setInCoffin(player, false, 0.0F);
				} finally {
					if (!nestedAllow) {
						ALLOW_EXIT.remove();
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}

	/**
	 * Former moveEntity double-Y clamp — disabled. Leave uses one entity-Y S08;
	 * clamping after move fought jump arcs.
	 */
	public static boolean clampLeaveMoveYLift(EntityPlayer player) {
		return false;
	}

	private static void invokeSetSize(EntityPlayer player, float width, float height) {
		try {
			if (setSizeMethod == null) {
				try {
					setSizeMethod = Entity.class.getDeclaredMethod("func_70105_a", float.class, float.class);
				} catch (NoSuchMethodException e) {
					setSizeMethod = Entity.class.getDeclaredMethod("setSize", float.class, float.class);
				}
				setSizeMethod.setAccessible(true);
			}
			setSizeMethod.invoke(player, Float.valueOf(width), Float.valueOf(height));
		} catch (Throwable t) {
			player.width = width;
			player.height = height;
		}
	}

	/** After coffin block activate: bind the clicked bed while nested. Does not store a second coordinate. */
	public static void afterCoffinActivated(EntityPlayer player, World world, int x, int y, int z) {
		if (player == null || world == null) return;
		if (!isInCoffin(player)) {
			if (world.isRemote) {
				CoffinDebug.log(player, "ACTIVATE_CLIENT " + x + "," + y + "," + z
						+ " sneak=" + player.isSneaking() + " " + describeNestState(player));
			}
			return;
		}
		bindCoffinSpawn(player, x, y, z);
		if (world.isRemote) {
			CoffinDebug.log(player, "ACTIVATE_CLIENT " + x + "," + y + "," + z
					+ " sneak=" + player.isSneaking() + " " + describeNestState(player));
			if (!(player.isSneaking() && isOpenCoffin(world.getBlock(x, y, z)))) {
				com.mrsand.StatBoostChanges.client.CoffinCamera.onEnter(player);
			}
			CoffinDebug.logEnter(player);
			return;
		}
		CoffinDebug.log(player, "ACTIVATE " + x + "," + y + "," + z
				+ " sneak=" + player.isSneaking() + " " + describeNestState(player));
		if (player.isSneaking() && isOpenCoffin(world.getBlock(x, y, z))) {
			exitCoffin(player);
			return;
		}
		CoffinDebug.logEnter(player);
	}

	/** After setInCoffin(true, …). Runs on client and server. Does not move the player. */
	public static void afterSetInCoffinTrue(EntityPlayer player) {
		onEnterCoffin(player);
	}

	private static void msg(EntityPlayer player, String text, EnumChatFormatting color) {
		player.addChatComponentMessage(new ChatComponentText(text)
				.setChatStyle(new ChatStyle().setColor(color).setItalic(true)));
	}
}
