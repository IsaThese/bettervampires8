package com.mrsand.StatBoostChanges.abilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.entity.EntityWitchHunter;
import com.emoniph.witchery.util.SoundEffect;
import com.emoniph.witchery.util.TimeUtil;
import com.mrsand.StatBoostChanges.abilities.powers.BvVampirePowers;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.HypnosisRayTrace;
import com.mrsand.StatBoostChanges.util.VampireEyesDebug;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.entities.mobs.EntityPunchingBag;
import com.tihyo.legends.entities.mobs.EntityStand;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.interfaces.IKaiju;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Hypnosis Mastery: hold to control.
 * <ul>
 *   <li>Default — free terrain/ledge drag; look down (hand gesture) = bite magnet standoff.</li>
 *   <li>Use item (right-click) toggles enslavement — stun in place and build a one-session bind (~6s).</li>
 *   <li>Use item again into enslavement on a bind you own — same ~6s session releases thrall/tame/paralyse.</li>
 * </ul>
 * Purple bar does not drain while channeling.
 */
public final class HypnosisMasteryLogic {

	/** +1 every 5 ticks; threshold 24 → ≈ 6s continuous focus to bind. */
	private static final int ATTEMPT_INTERVAL = 5;
	private static final int PARALYSE_THRESHOLD = 24;
	private static final int TAME_THRESHOLD = 24;
	private static final int THRALL_THRESHOLD = 24;
	private static final int SESSION_GAP_TICKS = 10;
	/** Debounce use-item toggles (RIGHT_CLICK_BLOCK repeats while held). */
	private static final int USE_TOGGLE_DEBOUNCE = 10;
	private static final int THRALL_DURATION_SECS = 240;
	/** Max approach speed (blocks/tick) — arrive slows further when close. */
	private static final double SPEED_CAP = 0.38D;
	private static final double SPEED_CAP_MOB = 0.48D;
	/** Blend toward desired velocity each tick (lower = smoother). */
	private static final double MOTION_BLEND = 0.38D;
	private static final double MOTION_BLEND_MOB = 0.42D;
	/** Distance (blocks) where arrive starts cutting speed. */
	private static final double ARRIVE_DIST = 2.25D;
	/**
	 * Arrive never drops below this fraction of the cap (close ground hits were crawling
	 * at MIN_APPROACH alone when the look-ray block sat under/beside the mob).
	 */
	private static final double ARRIVE_FLOOR = 0.40D;
	/** Soft floor so a nearly-arrived mob still creeps onto a ledge. */
	private static final double MIN_APPROACH = 0.14D;
	/** Enough lift to beat gravity and mount 1-high ledges (logs showed lift=true never fired). */
	private static final double MIN_LIFT = 0.22D;
	private static final double MIN_LIFT_MOB = 0.30D;
	private static final double MAX_LIFT = 0.48D;
	/** Extra Y blend when mounting so lift responds faster than horizontal drag. */
	private static final double LIFT_BLEND = 0.65D;
	private static final double PSYCHIC_RANGE = 25.0D;
	/** How far past the target (along look) before searching for a block to pull toward. */
	private static final double ENTITY_PAST_OFFSET = 0.45D;
	/**
	 * If the player's direct look-hit is within this horizontal range of the mob, prefer it
	 * over the past-entity ray (near-ledge: past-ray starts inside/beyond the ledge and
	 * only sees same-level floor — logs needed mob far away for lift=true).
	 */
	private static final double AIM_DEST_RANGE = 5.5D;
	/** Climb this many solid blocks above a ray hit so ledge-face/base mounts the top. */
	private static final int COLUMN_CLIMB = 3;
	/** Cap for bite-magnet standoff (horizontal center-to-center). */
	private static final double STANDOFF_MAX = 2.5D;
	/** Floor so tiny hitboxes are still biteable without face-gluing. */
	private static final double STANDOFF_MIN = 1.6D;
	/**
	 * Extra pad beyond touching hitbox radii (player arm / bite reach).
	 * Cow width 0.9 → half-radii + pad ≈ 2.5 (the requested bite ring).
	 */
	private static final double STANDOFF_BITE_PAD = 1.75D;
	/**
	 * Magnet approach uses a fraction of free-drag caps (logs: 0.48 felt too snappy).
	 */
	private static final double MAGNET_SPEED_SCALE = 0.58D;
	/**
	 * Look-down bite magnet thresholds (look vector Y). Level height keeps the mild nod;
	 * elevated above the target lerps toward near-straight-down so hovering ~3.5 blocks
	 * above a mob while aiming at it does not force magnet.
	 */
	private static final double LOOK_DOWN_MAGNET_LEVEL = -0.20D;
	/** ~89° pitch (within ~1–2° of vertical). */
	private static final double LOOK_DOWN_MAGNET_ELEVATED = -0.9994D;
	/** Feet-to-feet elevation where the elevated threshold is fully required. */
	private static final double MAGNET_ELEV_FULL = 3.5D;
	/** Heavy slowness on dragged non-players so they cannot walk out of the yank. */
	private static final int DRAG_SLOWNESS_AMPLIFIER = 5;
	private static final int DRAG_SLOWNESS_TICKS = 30;
	/**
	 * Vampirism {@code FreezeSkill} style lock while in bite magnet hold.
	 * Amp-5 was not enough — Zod walked out of the ring and jittered with micro-pulls.
	 */
	private static final int BITE_FREEZE_SLOWNESS_AMP = 10;
	private static final int BITE_FREEZE_JUMP_AMP = 128;
	private static final int BITE_FREEZE_TICKS = 40;
	/** Hold/freeze band past standoff so edge micro-pulls cannot oscillate (Zod ~2.52↔2.88). */
	private static final double STANDOFF_HOLD_SLACK = 0.55D;

	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<UUID, Session>();
	private static final String BV_AIM_OWNED_TAG = "bvHypnosisAimOwned";
	/** Mastery raised {@code HYPNOTIZING}; do not clear a mesmerize toggle we did not start. */
	private static final String BV_EYES_OWNED_TAG = "bvHypnosisEyesOwned";

	private HypnosisMasteryLogic() {}

	private static final class Session {
		int targetId = -1;
		int lastChannelTick;
		boolean boundThisSession;
		/** Use-item toggle: false = movement, true = stun/bind. */
		boolean enslavementMode;
		/** Debounce use-item presses while channeling. */
		int lastUseToggleTick;
	}

	/**
	 * Use-item (right-click) while channeling: flip movement ↔ enslavement.
	 * Returns true when mastery is channeling so callers can cancel place/use.
	 * Debounced presses still return true without flipping again.
	 */
	public static boolean tryToggleEnslavementFromUse(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) {
			return false;
		}
		if (!VampireRestoredAbilities.isHypnosisMasteryChanneling(player)) {
			return false;
		}
		int now = LegendsHelper.getTicks(player);
		touchSession(player, now);
		Session session = SESSIONS.get(player.getUniqueID());
		if (session == null) {
			return true;
		}
		if (session.lastUseToggleTick != 0
				&& now - session.lastUseToggleTick >= 0
				&& now - session.lastUseToggleTick < USE_TOGGLE_DEBOUNCE) {
			return true;
		}
		session.lastUseToggleTick = now == 0 ? 1 : now;
		session.enslavementMode = !session.enslavementMode;
		if (session.enslavementMode) {
			notifyBind(player, "You focus on domination");
		} else {
			notifyBind(player, "You focus on control");
		}
		return true;
	}

	/**
	 * Raise-hand pose: Legends 5 applied aiming via addEffectAndSync every hold tick.
	 * L8 uses AIMING_RIGHT / PlayerDataPacket but only syncs on value change — force sync
	 * when already 20, and apply locally on the client so FP/F5 do not wait on the packet.
	 * Only while Hypnosis Mastery is actively channeling.
	 */
	public static void refreshAimingPose(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		if (!VampireRestoredAbilities.isHypnosisMasteryChanneling(player)) return;
		if (player.isSwingInProgress) return;
		if (VampireRestoredAbilities.getHypnosisStrength(player) <= 5) return;

		Integer aimingObj = (Integer) LegendsProperties.AIMING_RIGHT.get(player);
		int aimingBefore = aimingObj == null ? 0 : aimingObj.intValue();

		// Same raise wind-up as L8 base Hypnosis (PowersHandler.hypnosis → setAnimationTicks).
		NBTTagCompound data = player.getEntityData();
		if (!data.getBoolean(BV_AIM_OWNED_TAG)) {
			PlayerHelper.setAnimationTicks(player, 0.0F, 9.0F, 4.0F);
		}
		data.setBoolean(BV_AIM_OWNED_TAG, true);

		if (player.worldObj.isRemote) {
			LegendsProperties.AIMING_RIGHT.setWithoutNotify(player, Integer.valueOf(20));
			LegendsProperties.AIMING_RIGHT_TYPE.setWithoutNotify(player, Integer.valueOf(0));
			if (HypnosisDebug.enabled() && LegendsHelper.getTicks(player) % 10 == 0) {
				HypnosisDebug.logAiming(player, "client-local", aimingBefore, PlayerHelper.isAimingRightArm(player));
			}
			return;
		}

		boolean wouldNoOp = aimingBefore == 20;
		PowersHandler.aimingRightArm(player, 1, 0);
		// aimingRightArm skips set/sync when already 20 — observers never refresh without this.
		if (wouldNoOp) {
			LegendsProperties.AIMING_RIGHT.sync(player);
			LegendsProperties.AIMING_RIGHT_TYPE.sync(player);
		}
		// Powers-only eyes: claim HYPNOTIZING once on server (beginHypnosisChannel → syncClient).
		beginMasteryEyesIfNeeded(player);
		if (HypnosisDebug.enabled() && LegendsHelper.getTicks(player) % 10 == 0) {
			HypnosisDebug.logAiming(player, wouldNoOp ? "server-forceSync" : "server-set",
					aimingBefore, PlayerHelper.isAimingRightArm(player));
		}
	}

	/** Server-only: one begin for mastery-owned eyes; skip if mesmerize already owns the flag. */
	private static void beginMasteryEyesIfNeeded(EntityPlayer player) {
		if (player.worldObj.isRemote) return;
		NBTTagCompound data = player.getEntityData();
		if (data.getBoolean(BV_EYES_OWNED_TAG)) return;
		VExtendedPlayer v = VExtendedPlayer.get(player);
		if (v == null) return;
		if (v.getBoolean(PlayerKeys.HYPNOTIZING)) {
			VampireEyesDebug.logState(player, "mastery-eyes-skip-owned-elsewhere");
			return;
		}
		WitcheryUtil.beginHypnosisChannel(player);
		data.setBoolean(BV_EYES_OWNED_TAG, true);
		VampireEyesDebug.logState(player, "mastery-eyes-begin");
	}

	/**
	 * Drop the raise-hand pose immediately when mastery hold ends (do not wait for L8's ~1s decay).
	 * Only clears AIMING we previously raised so other powers keep their pose.
	 */
	public static void clearAimingPose(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		NBTTagCompound data = player.getEntityData();
		boolean clearedAim = data.getBoolean(BV_AIM_OWNED_TAG);
		boolean clearedEyes = data.getBoolean(BV_EYES_OWNED_TAG);
		if (!clearedAim && !clearedEyes) return;

		if (clearedAim) {
			data.removeTag(BV_AIM_OWNED_TAG);
			Integer aimingObj = (Integer) LegendsProperties.AIMING_RIGHT.get(player);
			int aiming = aimingObj == null ? 0 : aimingObj.intValue();

			if (player.worldObj.isRemote) {
				LegendsProperties.AIMING_RIGHT.setWithoutNotify(player, Integer.valueOf(0));
				LegendsProperties.AIMING_RIGHT_TYPE.setWithoutNotify(player, Integer.valueOf(0));
			} else {
				LegendsProperties.AIMING_RIGHT.set(player, Integer.valueOf(0));
				LegendsProperties.AIMING_RIGHT_TYPE.set(player, Integer.valueOf(0));
			}
			if (HypnosisDebug.enabled()) {
				HypnosisDebug.logAiming(player, "clear", aiming, PlayerHelper.isAimingRightArm(player));
			}
		}

		// Only drop HYPNOTIZING when mastery started it (leave mesmerize toggle alone).
		if (clearedEyes) {
			data.removeTag(BV_EYES_OWNED_TAG);
			if (!player.worldObj.isRemote) {
				WitcheryUtil.endHypnosisChannel(player);
				VampireEyesDebug.logState(player, "mastery-eyes-end");
			}
		}
		clearCurrentTargetId(player);
	}

	/**
	 * Call every tick while holding (client + server). Client pass refreshes aim pose +
	 * pinned target; server pass aims / stun-or-pull / bind progress.
	 */
	public static void tickChannel(EntityPlayer player) {
		if (player == null) return;

		if (player.worldObj.isRemote) {
			refreshClientTarget(player);
			return;
		}

		int now = LegendsHelper.getTicks(player);
		touchSession(player, now);

		// Infinite channel: bar is never spent here (still gates at > 5 so empty bar blocks use).
		if (VampireRestoredAbilities.getHypnosisStrength(player) <= 5) return;

		refreshAimingPose(player);

		Session session = SESSIONS.get(player.getUniqueID());
		boolean enslavement = session != null && session.enslavementMode;

		EntityLivingBase target = resolveStickyTarget(player, session, now);

		if (HypnosisDebug.enabled() && now % 10 == 0) {
			int key = BvVampirePowers.hypnosisMastery.getAssignedKey(player);
			int duration = key > 0 ? LegacyHelper.getDuration(player, key) : -1;
			Vec3 lookDbg = player.getLook(1.0F);
			Integer aim = (Integer) LegendsProperties.AIMING_RIGHT.get(player);
			HypnosisDebug.logMastery(player,
					"str=" + VampireRestoredAbilities.getHypnosisStrength(player)
							+ " key=" + key + " dur=" + duration
							+ " enslave=" + enslavement
							+ " aim=" + (aim == null ? 0 : aim.intValue())
							+ " isAiming=" + PlayerHelper.isAimingRightArm(player)
							+ " target=" + (target == null ? "null" : target.getCommandSenderName())
							+ " locked=" + (session != null && session.targetId > 0)
							+ " sessBound=" + (session != null && session.boundThisSession)
							+ " att=" + (target == null ? 0 : target.getEntityData().getInteger("VampireHypnosisAttempts"))
							+ String.format(" look=%.2f,%.2f,%.2f", lookDbg.xCoord, lookDbg.yCoord, lookDbg.zCoord));
		}

		if (target == null) {
			return;
		}

		if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.isCreativeMode) {
			return;
		}

		pinCurrentTargetId(player, target);

		LegendsAbilities.hypnosis(target);

		if (target instanceof EntityLiving) {
			EntityLiving living = (EntityLiving) target;
			if (living.getNavigator() != null) {
				living.getNavigator().clearPathEntity();
			}
			living.setAttackTarget(null);
			living.setRevengeTarget(null);
		}

		if (target instanceof EntityCreeper) {
			((EntityCreeper) target).setCreeperState(-1);
		}

		if (enslavement) {
			// Enslavement focus: stun in place (no pull) and build bind.
			applyStun(target);
			tickBindProgress(player, target, now);
		} else {
			double lookY = player.getLook(1.0F).yCoord;
			if (lookY <= lookDownMagnetThreshold(player, target)) {
				applyBiteStandoffPull(player, target);
			} else {
				applyPsychicMoveToBlock(player, target);
			}
		}

		if (HypnosisDebug.enabled() && now % 10 == 0) {
			HypnosisDebug.logMastery(player,
					String.format("%s %s motion=%.3f,%.3f,%.3f",
							enslavement ? "stun" : "blockPull",
							target.getCommandSenderName(), target.motionX, target.motionY, target.motionZ));
		}
	}

	/** Expire abandoned hold sessions so attempt progress does not carry across releases. */
	public static void tickSessionExpiry(EntityPlayer player) {
		if (player == null || player.worldObj.isRemote) return;
		Session s = SESSIONS.get(player.getUniqueID());
		if (s == null) return;
		int now = LegendsHelper.getTicks(player);
		if (now - s.lastChannelTick > SESSION_GAP_TICKS) {
			endSession(player, s, player.worldObj);
		}
	}

	/**
	 * Client: keep CURRENT_TARGET_ID sticky while channeling; acquire via Legends
	 * getTarget (stock pin sync) when unset, with skip-ray as plant fallback.
	 * Does not swap mid-hold when looking at another entity.
	 */
	public static void refreshClientTarget(EntityPlayer player) {
		if (player == null || !player.worldObj.isRemote) return;
		EntityLivingBase locked = resolvePinnedTarget(player);
		if (locked == null) {
			// Stock Legends getTarget updates+syncs CURRENT_TARGET_ID on client.
			EntityLivingBase crosshair = LegendsHelper.getTarget(player, PSYCHIC_RANGE);
			if (crosshair == null) {
				crosshair = HypnosisRayTrace.getLivingFromCrosshairs(player, 1.0F, PSYCHIC_RANGE);
				if (crosshair != null && canControlTarget(crosshair)
						&& !(crosshair instanceof EntityPlayer
								&& ((EntityPlayer) crosshair).capabilities.isCreativeMode)) {
					// Skip-ray plant fallback — use notifying set so server can acquire.
					LegendsProperties.CURRENT_TARGET_ID.set(player, Integer.valueOf(crosshair.getEntityId()));
				}
			}
		}
		refreshAimingPose(player);
	}

	/**
	 * Free terrain drag (look level/up): aim/past/ledge without reeling into the player's face.
	 * Inbound aim dests prefer past; leftover pull-toward-player motion is stripped.
	 */
	private static void applyPsychicMoveToBlock(EntityPlayer player, EntityLivingBase target) {
		Vec3 eye = player.getPosition(1.0F);
		Vec3 look = player.getLook(1.0F);
		double midY = target.posY + (double) target.height * 0.5D;
		double toX = target.posX - eye.xCoord;
		double toY = midY - eye.yCoord;
		double toZ = target.posZ - eye.zCoord;
		double along = toX * look.xCoord + toY * look.yCoord + toZ * look.zCoord;
		if (along < 0.0D) along = 0.0D;

		double past = along + Math.max((double) target.width, (double) target.height) * 0.5D + ENTITY_PAST_OFFSET;

		Vec3 start = eye.addVector(look.xCoord * past, look.yCoord * past, look.zCoord * past);
		Vec3 end = eye.addVector(look.xCoord * PSYCHIC_RANGE, look.yCoord * PSYCHIC_RANGE, look.zCoord * PSYCHIC_RANGE);
		MovingObjectPosition pastHit = past < PSYCHIC_RANGE
				? HypnosisRayTrace.rayTraceSkipPassThrough(player.worldObj, start, end)
				: null;
		MovingObjectPosition aimHit = HypnosisRayTrace.rayTraceFromEyes(player, PSYCHIC_RANGE, 1.0F);

		DestPick pick = pickPsychicDest(player, target, aimHit, pastHit);
		// Unlocked: never reel in via floor-between-you aim — prefer past when aim is inbound.
		if (pick != null && isDestInbound(player, target, pick.destX, pick.destZ)) {
			DestPick pastPick = destFromHit(player, target, pastHit, "past");
			if (pastPick != null && !isDestInbound(player, target, pastPick.destX, pastPick.destZ)) {
				pick = pastPick;
			} else if (pastPick != null && "aim".equals(pick.source)) {
				pick = pastPick;
			}
		}
		if (pick == null) {
			if (HypnosisDebug.dragEnabled()) {
				HypnosisDebug.logDrag(player, LegendsHelper.getTicks(player),
						String.format("noBlockHit id=%d name=%s lookY=%.2f magnet=false along=%.2f past=%.2f rayStart=%.2f,%.2f,%.2f rayEnd=%.2f,%.2f,%.2f ent=%.2f,%.2f,%.2f vel=%.3f,%.3f,%.3f",
								target.getEntityId(), target.getCommandSenderName(),
								look.yCoord,
								along, past,
								start.xCoord, start.yCoord, start.zCoord,
								end.xCoord, end.yCoord, end.zCoord,
								target.posX, target.posY, target.posZ,
								target.motionX, target.motionY, target.motionZ));
			}
			return;
		}

		double destX = pick.destX;
		double destY = pick.destY;
		double destZ = pick.destZ;
		boolean clampedDown = pick.clampedDown;
		boolean inbound = isDestInbound(player, target, destX, destZ);

		double dx = destX - target.posX;
		double dy = destY - target.posY;
		double dz = destZ - target.posZ;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double targetToPlayer = horizDist(target.posX, target.posZ, player.posX, player.posZ);

		if (dist < 0.12D) {
			target.motionX *= 0.3D;
			target.motionZ *= 0.3D;
			if (target.motionY > 0.0D) target.motionY *= 0.3D;
			target.velocityChanged = true;
			if (HypnosisDebug.dragEnabled()) {
				HypnosisDebug.logDrag(player, LegendsHelper.getTicks(player),
						String.format("arrived id=%d name=%s src=%s lookY=%.2f magnet=false block=%d,%d,%d ent=%.2f,%.2f,%.2f dist=%.3f",
								target.getEntityId(), target.getCommandSenderName(), pick.source,
								look.yCoord,
								pick.blockX, pick.blockY, pick.blockZ,
								target.posX, target.posY, target.posZ, dist));
			}
			return;
		}

		boolean nonPlayer = !(target instanceof EntityPlayer);
		double speedCap = nonPlayer ? SPEED_CAP_MOB : SPEED_CAP;
		double blend = nonPlayer ? MOTION_BLEND_MOB : MOTION_BLEND;
		double minLift = nonPlayer ? MIN_LIFT_MOB : MIN_LIFT;

		double arrive = dist / ARRIVE_DIST;
		if (arrive > 1.0D) arrive = 1.0D;
		if (arrive < ARRIVE_FLOOR) arrive = ARRIVE_FLOOR;
		double speed = speedCap * arrive;
		if (speed < MIN_APPROACH) speed = MIN_APPROACH;

		double nx = dx / dist;
		double ny = dy / dist;
		double nz = dz / dist;

		double wantX = nx * speed;
		double wantY = ny * speed;
		double wantZ = nz * speed;

		boolean needLift = destY > target.posY + 0.15D;
		if (needLift) {
			if (wantY < minLift) wantY = minLift;
			if (wantY > MAX_LIFT) wantY = MAX_LIFT;
			if (nonPlayer) target.onGround = false;
		}

		// Unlocked: never close distance to the player (ledge lift / lateral / push away OK).
		if (inbound) {
			double[] stripped = stripTowardPlayer(player, target, wantX, wantY, wantZ);
			wantX = stripped[0];
			wantY = stripped[1];
			wantZ = stripped[2];
		}

		double beforeX = target.motionX;
		double beforeY = target.motionY;
		double beforeZ = target.motionZ;
		double yBlend = needLift ? LIFT_BLEND : blend;

		target.motionX = beforeX + (wantX - beforeX) * blend;
		target.motionY = beforeY + (wantY - beforeY) * yBlend;
		target.motionZ = beforeZ + (wantZ - beforeZ) * blend;

		target.fallDistance = 0.0F;
		target.velocityChanged = true;

		if (nonPlayer) {
			target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, DRAG_SLOWNESS_TICKS,
					DRAG_SLOWNESS_AMPLIFIER, true));
		}

		if (HypnosisDebug.dragEnabled()) {
			HypnosisDebug.logDrag(player, LegendsHelper.getTicks(player),
					String.format(
							"id=%d name=%s src=%s lookY=%.2f magnet=false "
									+ "ent=%.2f,%.2f,%.2f block=%d,%d,%d dest=%.2f,%.2f,%.2f "
									+ "hitVec=%.2f,%.2f,%.2f dist=%.2f arrive=%.2f speed=%.3f "
									+ "want=%.3f,%.3f,%.3f velIn=%.3f,%.3f,%.3f velOut=%.3f,%.3f,%.3f "
									+ "lift=%s clampDown=%s inbound=%s pDist=%.2f "
									+ "rayStart=%.2f,%.2f,%.2f along=%.2f past=%.2f",
							target.getEntityId(),
							target.getCommandSenderName(),
							pick.source,
							look.yCoord,
							target.posX, target.posY, target.posZ,
							pick.blockX, pick.blockY, pick.blockZ,
							destX, destY, destZ,
							pick.hitX, pick.hitY, pick.hitZ,
							dist, arrive, speed,
							wantX, wantY, wantZ,
							beforeX, beforeY, beforeZ,
							target.motionX, target.motionY, target.motionZ,
							needLift, clampedDown, inbound, targetToPlayer,
							start.xCoord, start.yCoord, start.zCoord,
							along, past));
		}
	}

	/**
	 * Look-Y threshold for bite magnet: mild at level height, near-vertical when the player
	 * is {@link #MAGNET_ELEV_FULL}+ blocks above the target (feet-to-feet).
	 */
	private static double lookDownMagnetThreshold(EntityPlayer player, EntityLivingBase target) {
		double elev = player.posY - target.posY;
		if (elev <= 0.0D) return LOOK_DOWN_MAGNET_LEVEL;
		double t = elev / MAGNET_ELEV_FULL;
		if (t > 1.0D) t = 1.0D;
		return LOOK_DOWN_MAGNET_LEVEL
				+ (LOOK_DOWN_MAGNET_ELEVATED - LOOK_DOWN_MAGNET_LEVEL) * t;
	}

	/** True when player is above target by at most {@link #MAGNET_ELEV_FULL} (feet-to-feet). */
	private static boolean withinMagnetElevGate(EntityPlayer player, EntityLivingBase target) {
		double elev = player.posY - target.posY;
		return elev > 0.15D && elev <= MAGNET_ELEV_FULL;
	}

	/**
	 * Bite magnet ring: hitbox touch (+ pad), or mob melee-ish reach ({@code width*2}), capped at
	 * {@link #STANDOFF_MAX}.
	 */
	private static double biteStandoffDistance(EntityPlayer player, EntityLivingBase target) {
		double touch = player.width * 0.5D + target.width * 0.5D + STANDOFF_BITE_PAD;
		// Vanilla collide-AI style reach uses ~2× attacker width; treat the mob the same way.
		double melee = (double) target.width * 2.0D + player.width * 0.5D;
		double d = Math.max(touch, melee);
		if (d < STANDOFF_MIN) d = STANDOFF_MIN;
		if (d > STANDOFF_MAX) d = STANDOFF_MAX;
		return d;
	}

	/**
	 * Same lock as Vampirism {@code FreezeSkill}: Slowness X + Jump 128, plus hard stop / AI clear.
	 * No Resistance — they must stay biteable in combat.
	 */
	private static void applyBiteFreeze(EntityLivingBase target) {
		applyStun(target);
		target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, BITE_FREEZE_TICKS,
				BITE_FREEZE_SLOWNESS_AMP, true));
		target.addPotionEffect(new PotionEffect(Potion.jump.id, BITE_FREEZE_TICKS,
				BITE_FREEZE_JUMP_AMP, true));
		if (target instanceof EntityLiving) {
			EntityLiving living = (EntityLiving) target;
			if (living.getNavigator() != null) {
				living.getNavigator().clearPathEntity();
			}
			living.setAttackTarget(null);
			living.setRevengeTarget(null);
		}
	}

	/**
	 * Look-down bite magnet: reel in only when farther than the hold band; at/inside use
	 * Vampirism-style freeze (Zod combat logs: AI walked out → micro-pull jitter).
	 * When the player is levitating within the elev gate, lift the target to player feet Y.
	 * Optional block-aim lift when not matching player elevation.
	 */
	private static void applyBiteStandoffPull(EntityPlayer player, EntityLivingBase target) {
		double lookY = player.getLook(1.0F).yCoord;
		double standoff = biteStandoffDistance(player, target);
		double targetToPlayer = horizDist(target.posX, target.posZ, player.posX, player.posZ);
		boolean nonPlayer = !(target instanceof EntityPlayer);
		boolean matchPlayerElev = withinMagnetElevGate(player, target);

		// Already in bite range (at or inside hold band): hard freeze — unless still catching
		// player hover elevation (levitating vampire within 3.5 blocks above).
		if (targetToPlayer <= standoff + STANDOFF_HOLD_SLACK && !matchPlayerElev) {
			if (nonPlayer) {
				applyBiteFreeze(target);
			} else {
				applyStun(target);
			}
			if (HypnosisDebug.dragEnabled()) {
				HypnosisDebug.logDrag(player, LegendsHelper.getTicks(player),
						String.format("standoffHold id=%d name=%s lookY=%.2f magnet=true stand=%.2f pDist=%.2f ent=%.2f,%.2f,%.2f freeze=true",
								target.getEntityId(), target.getCommandSenderName(),
								lookY, standoff, targetToPlayer,
								target.posX, target.posY, target.posZ));
			}
			return;
		}

		double rdx = target.posX - player.posX;
		double rdz = target.posZ - player.posZ;
		double rh = Math.sqrt(rdx * rdx + rdz * rdz);
		if (rh < 1.0E-4D) {
			Vec3 look = player.getLook(1.0F);
			rdx = look.xCoord;
			rdz = look.zCoord;
			rh = Math.sqrt(rdx * rdx + rdz * rdz);
			if (rh < 1.0E-4D) return;
		}
		rdx /= rh;
		rdz /= rh;

		double destX = player.posX + rdx * standoff;
		double destZ = player.posZ + rdz * standoff;
		double destY = target.posY;
		boolean needLift = false;

		// Levitating player within elev gate: hover the target up to the player's feet Y.
		if (matchPlayerElev) {
			destY = player.posY;
			needLift = true;
		} else {
			// Optional mount while reeling: aim higher block near the mob.
			MovingObjectPosition aimHit = HypnosisRayTrace.rayTraceFromEyes(player, PSYCHIC_RANGE, 1.0F);
			DestPick aim = destFromHit(player, target, aimHit, "aim");
			if (aim != null) {
				double ahx = aim.destX - target.posX;
				double ahz = aim.destZ - target.posZ;
				if (Math.sqrt(ahx * ahx + ahz * ahz) <= AIM_DEST_RANGE && aim.destY > target.posY + 0.15D) {
					destY = aim.destY;
					needLift = true;
				}
			}
		}

		double dx = destX - target.posX;
		double dy = destY - target.posY;
		double dz = destZ - target.posZ;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist < 0.12D) {
			if (nonPlayer) applyBiteFreeze(target);
			else applyStun(target);
			return;
		}

		// Kill AI walking while we reel (FreezeSkill potions); forced motion still applies after.
		if (nonPlayer) {
			target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, BITE_FREEZE_TICKS,
					BITE_FREEZE_SLOWNESS_AMP, true));
			target.addPotionEffect(new PotionEffect(Potion.jump.id, BITE_FREEZE_TICKS,
					BITE_FREEZE_JUMP_AMP, true));
			if (target instanceof EntityLiving) {
				EntityLiving living = (EntityLiving) target;
				if (living.getNavigator() != null) {
					living.getNavigator().clearPathEntity();
				}
				living.setAttackTarget(null);
				living.setRevengeTarget(null);
			}
		}

		double speedCap = (nonPlayer ? SPEED_CAP_MOB : SPEED_CAP) * MAGNET_SPEED_SCALE;
		double blend = nonPlayer ? MOTION_BLEND_MOB : MOTION_BLEND;
		double minLift = nonPlayer ? MIN_LIFT_MOB : MIN_LIFT;
		double minApproach = MIN_APPROACH * MAGNET_SPEED_SCALE;

		double arrive = dist / ARRIVE_DIST;
		if (arrive > 1.0D) arrive = 1.0D;
		if (arrive < ARRIVE_FLOOR) arrive = ARRIVE_FLOOR;
		double speed = speedCap * arrive;
		if (speed < minApproach) speed = minApproach;

		double nx = dx / dist;
		double ny = dy / dist;
		double nz = dz / dist;
		double wantX = nx * speed;
		double wantY = ny * speed;
		double wantZ = nz * speed;

		if (needLift) {
			if (wantY < minLift) wantY = minLift;
			if (wantY > MAX_LIFT) wantY = MAX_LIFT;
			if (nonPlayer) target.onGround = false;
		}

		double beforeX = target.motionX;
		double beforeY = target.motionY;
		double beforeZ = target.motionZ;
		double yBlend = needLift ? LIFT_BLEND : blend;

		target.motionX = beforeX + (wantX - beforeX) * blend;
		target.motionY = beforeY + (wantY - beforeY) * yBlend;
		target.motionZ = beforeZ + (wantZ - beforeZ) * blend;
		target.fallDistance = 0.0F;
		target.velocityChanged = true;

		if (HypnosisDebug.dragEnabled()) {
			HypnosisDebug.logDrag(player, LegendsHelper.getTicks(player),
					String.format(
							"id=%d name=%s src=magnet lookY=%.2f magnet=true stand=%.2f "
									+ "ent=%.2f,%.2f,%.2f dest=%.2f,%.2f,%.2f dist=%.2f speed=%.3f "
									+ "want=%.3f,%.3f,%.3f velOut=%.3f,%.3f,%.3f lift=%s pDist=%.2f",
							target.getEntityId(), target.getCommandSenderName(),
							lookY, standoff,
							target.posX, target.posY, target.posZ,
							destX, destY, destZ, dist, speed,
							wantX, wantY, wantZ,
							target.motionX, target.motionY, target.motionZ,
							needLift, targetToPlayer));
		}
	}

	private static double horizDist(double x0, double z0, double x1, double z1) {
		double dx = x0 - x1;
		double dz = z0 - z1;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static boolean isDestInbound(EntityPlayer player, EntityLivingBase target, double destX, double destZ) {
		double targetToPlayer = horizDist(target.posX, target.posZ, player.posX, player.posZ);
		double destToPlayer = horizDist(destX, destZ, player.posX, player.posZ);
		return destToPlayer + 0.15D < targetToPlayer;
	}

	/** Remove the horizontal component of want that closes distance to the player. */
	private static double[] stripTowardPlayer(EntityPlayer player, EntityLivingBase target,
			double wantX, double wantY, double wantZ) {
		double rdx = target.posX - player.posX;
		double rdz = target.posZ - player.posZ;
		double rh = Math.sqrt(rdx * rdx + rdz * rdz);
		if (rh < 1.0E-4D) {
			return new double[] { 0.0D, wantY, 0.0D };
		}
		rdx /= rh;
		rdz /= rh;
		double radial = wantX * rdx + wantZ * rdz;
		if (radial < 0.0D) {
			wantX -= rdx * radial;
			wantZ -= rdz * radial;
		}
		return new double[] { wantX, wantY, wantZ };
	}

	private static final class DestPick {
		final String source;
		final int blockX, blockY, blockZ;
		final double destX, destY, destZ;
		final double hitX, hitY, hitZ;
		final boolean clampedDown;

		DestPick(String source, int blockX, int blockY, int blockZ,
				double destX, double destY, double destZ,
				double hitX, double hitY, double hitZ, boolean clampedDown) {
			this.source = source;
			this.blockX = blockX;
			this.blockY = blockY;
			this.blockZ = blockZ;
			this.destX = destX;
			this.destY = destY;
			this.destZ = destZ;
			this.hitX = hitX;
			this.hitY = hitY;
			this.hitZ = hitZ;
			this.clampedDown = clampedDown;
		}
	}

	/**
	 * Prefer the block the player is aiming at when it sits near the mob (near-ledge mount);
	 * otherwise the first block past the entity along look. Climb short solid columns so
	 * hitting a ledge face/base resolves to the standable top.
	 */
	private static DestPick pickPsychicDest(EntityPlayer player, EntityLivingBase target,
			MovingObjectPosition aimHit, MovingObjectPosition pastHit) {
		DestPick aim = destFromHit(player, target, aimHit, "aim");
		DestPick past = destFromHit(player, target, pastHit, "past");

		if (aim != null) {
			double ahx = aim.destX - target.posX;
			double ahz = aim.destZ - target.posZ;
			double aHoriz = Math.sqrt(ahx * ahx + ahz * ahz);
			boolean aimNear = aHoriz <= AIM_DEST_RANGE;
			boolean aimHigher = aim.destY > target.posY + 0.15D;
			boolean pastFlat = past == null || past.destY <= target.posY + 0.15D;
			// Near ledge: aim at the raised block even when past-ray only sees floor.
			if (aimNear && (aimHigher || past == null || pastFlat || aim.destY >= past.destY)) {
				return aim;
			}
		}
		return past != null ? past : aim;
	}

	private static DestPick destFromHit(EntityPlayer player, EntityLivingBase target,
			MovingObjectPosition hit, String source) {
		if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.hitVec == null) {
			return null;
		}
		int topY = climbStandTop(player.worldObj, hit.blockX, hit.blockY, hit.blockZ);
		double destX = hit.blockX + 0.5D;
		double destY = topY + 1.05D;
		double destZ = hit.blockZ + 0.5D;
		boolean clampedDown = false;
		if (destY < target.posY) {
			destY = target.posY;
			clampedDown = true;
		}
		return new DestPick(source, hit.blockX, topY, hit.blockZ,
				destX, destY, destZ,
				hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord,
				clampedDown);
	}

	/** Top Y of a short solid stack above the hit (ledge face/base → ledge top). */
	private static int climbStandTop(net.minecraft.world.World world, int x, int y, int z) {
		int top = y;
		for (int i = 1; i <= COLUMN_CLIMB; i++) {
			int ny = y + i;
			if (world.isAirBlock(x, ny, z)) break;
			net.minecraft.block.Block block = world.getBlock(x, ny, z);
			if (block == null || !block.getMaterial().blocksMovement()) break;
			top = ny;
		}
		return top;
	}

	/** Hold them still without yanking (enslavement focus). */
	private static void applyStun(EntityLivingBase target) {
		target.motionX = 0.0D;
		target.motionZ = 0.0D;
		if (target.motionY > 0.0D) {
			target.motionY = 0.0D;
		}
		target.fallDistance = 0.0F;
		target.velocityChanged = true;
	}

	private static void tickBindProgress(EntityPlayer player, EntityLivingBase target, int now) {
		if (target instanceof EntityPlayer) return;

		Session s = SESSIONS.get(player.getUniqueID());
		if (s == null || s.boundThisSession) return;
		if (now % ATTEMPT_INTERVAL != 0) return;

		// Stand-hold again on a bind you own → release (~6s new session).
		if (isReleasableBind(player, target)) {
			tickReleaseProgress(player, target, s);
			return;
		}

		// Villager / guard → long paralyse.
		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& !(target instanceof com.emoniph.witchery.entity.EntityVillagerWere)) {
			int attempt = bumpAttempts(player, target, PARALYSE_THRESHOLD);
			if (attempt < PARALYSE_THRESHOLD) return;
			if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
				WitcheryUtil.resetHypnosisAttempts(target);
				return;
			}
			WitcheryUtil.resetHypnosisAttempts(target);
			int amp = WitcheryUtil.getVampireLevel(player) >= 8 ? 5 : 4;
			target.addPotionEffect(new PotionEffect(Witchery.Potions.PARALYSED.id,
					TimeUtil.secsToTicks(THRALL_DURATION_SECS), amp));
			if (target instanceof EntityVillageGuard) {
				EntityVillageGuard guard = (EntityVillageGuard) target;
				guard.setTarget(null);
				guard.setAttackTarget(null);
				guard.setRevengeTarget(null);
			}
			if (target instanceof EntityVillager) {
				GeneralUtil.removeAvoidanceTasks((EntityVillager) target);
			}
			notifyBind(player, "Their mind locks — they are under your sway");
			playBindSuccess(player, target, true);
			s.boundThisSession = true;
			return;
		}

		// Tameables → tame once if untamed.
		if (target instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) target;
			if (tameable.isTamed()) {
				// Someone else's pet — do not steal or release.
				s.boundThisSession = true;
				return;
			}
			int attempt = bumpAttempts(player, target, TAME_THRESHOLD);
			if (attempt < TAME_THRESHOLD) return;
			if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
				WitcheryUtil.resetHypnosisAttempts(target);
				return;
			}
			WitcheryUtil.resetHypnosisAttempts(target);
			tameable.setTamed(true);
			tameable.func_152115_b(player.getUniqueID().toString());
			if (tameable instanceof net.minecraft.entity.passive.EntityOcelot) {
				((net.minecraft.entity.passive.EntityOcelot) tameable)
						.setTameSkin(1 + tameable.worldObj.rand.nextInt(3));
			}
			tameable.worldObj.setEntityState(tameable, (byte) 7);
			tameable.setAttackTarget(null);
			tameable.setRevengeTarget(null);
			if (tameable.getNavigator() != null) {
				tameable.getNavigator().clearPathEntity();
			}
			notifyBind(player, "It's now loyal to you");
			playBindSuccess(player, target, true);
			s.boundThisSession = true;
			return;
		}

		// Other creatures → DIY thrall once.
		if (target instanceof EntityCreature
				&& !(target instanceof EntityWitchHunter)
				&& !(target instanceof EntitySquid)) {
			EntityLiving living = (EntityLiving) target;
			if (WitcheryUtil.isThralled(living)) {
				// Another controller's thrall — do not steal.
				s.boundThisSession = true;
				return;
			}
			int attempt = bumpAttempts(player, target, THRALL_THRESHOLD);
			if (attempt < THRALL_THRESHOLD) return;
			if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
				WitcheryUtil.resetHypnosisAttempts(target);
				return;
			}
			WitcheryUtil.resetHypnosisAttempts(living);
			living.getEntityData().removeTag("fearTimer");
			WitcheryUtil.startThrall(living, player, TimeUtil.secsToTicks(THRALL_DURATION_SECS));
			// startThrall already plays the hypnosis sound — avoid doubling it.
			notifyBind(player, "It bends to your will");
			playBindSuccess(player, target, false);
			s.boundThisSession = true;
		}
	}

	/** True when stand-hold should release instead of bind. */
	private static boolean isReleasableBind(EntityPlayer player, EntityLivingBase target) {
		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& !(target instanceof com.emoniph.witchery.entity.EntityVillagerWere)
				&& target.isPotionActive(Witchery.Potions.PARALYSED)) {
			return true;
		}
		if (target instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) target;
			return tameable.isTamed() && tameable.func_152114_e(player);
		}
		if (target instanceof EntityLiving) {
			return WitcheryUtil.isThralledBy((EntityLiving) target, player);
		}
		return false;
	}

	private static void tickReleaseProgress(EntityPlayer player, EntityLivingBase target, Session s) {
		int attempt = bumpAttempts(player, target, THRALL_THRESHOLD);
		if (attempt < THRALL_THRESHOLD) return;
		if (!WitcheryUtil.tryHypnosisBloodCost(player)) {
			WitcheryUtil.resetHypnosisAttempts(target);
			return;
		}
		WitcheryUtil.resetHypnosisAttempts(target);

		if ((target instanceof EntityVillager || target instanceof EntityVillageGuard)
				&& target.isPotionActive(Witchery.Potions.PARALYSED)) {
			target.removePotionEffect(Witchery.Potions.PARALYSED.id);
		} else if (target instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) target;
			tameable.setTamed(false);
			tameable.func_152115_b("");
			tameable.setAttackTarget(null);
			tameable.setRevengeTarget(null);
			if (tameable.getNavigator() != null) {
				tameable.getNavigator().clearPathEntity();
			}
			tameable.worldObj.setEntityState(tameable, (byte) 6);
		} else if (target instanceof EntityLiving) {
			WitcheryUtil.clearThrall((EntityLiving) target);
		}

		notifyBind(player, "You release your hold on their mind");
		playBindSuccess(player, target, true);
		s.boundThisSession = true;
	}

	private static int bumpAttempts(EntityPlayer player, EntityLivingBase target, int threshold) {
		NBTTagCompound nbt = target.getEntityData();
		int attempt = nbt.getInteger("VampireHypnosisAttempts") + 1;
		nbt.setInteger("VampireHypnosisAttempts", attempt);
		if (HypnosisDebug.dragEnabled()) {
			Session s = SESSIONS.get(player.getUniqueID());
			HypnosisDebug.logBind(player,
					String.format("masteryBump id=%d name=%s enslave=%s attempt=%d/%d",
							target.getEntityId(), target.getCommandSenderName(),
							s != null && s.enslavementMode, attempt, threshold));
		}
		playBindProgress(player, attempt, threshold);
		return attempt;
	}

	/** Ding at start; rising hypnosis pitch while focus builds — no chat until success. */
	private static void playBindProgress(EntityPlayer player, int attempt, int threshold) {
		if (attempt == 1) {
			if (HypnosisDebug.dragEnabled()) {
				Session s = SESSIONS.get(player.getUniqueID());
				HypnosisDebug.logBind(player, "ding random.orb attempt=1 enslave="
						+ (s != null && s.enslavementMode));
			}
			player.worldObj.playSoundAtEntity(player, "random.orb", 1.0F, 5.0F);
			return;
		}
		float progress = (float) attempt / (float) threshold;
		float pitch = 0.1F + (progress * 1.9F);
		WitcheryUtil.playHypnoSound(player.worldObj, player, 0.35F, pitch);
	}

	private static void playBindSuccess(EntityPlayer player, EntityLivingBase target, boolean playHypnosisSound) {
		if (playHypnosisSound) {
			SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(target.worldObj, player, 0.5F, 1.0F);
		}
		player.worldObj.playSoundAtEntity(player, "mob.wolf.growl", 1.0F, 1.0F);
	}

	private static void notifyBind(EntityPlayer player, String message) {
		if (player == null || player.worldObj.isRemote) return;
		player.addChatMessage(new ChatComponentText(message)
				.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
	}

	private static void touchSession(EntityPlayer player, int now) {
		Session s = SESSIONS.get(player.getUniqueID());
		if (s != null && now - s.lastChannelTick > SESSION_GAP_TICKS) {
			endSession(player, s, player.worldObj);
			s = null;
		}
		if (s == null) {
			s = new Session();
			SESSIONS.put(player.getUniqueID(), s);
		}
		s.lastChannelTick = now;
	}

	/**
	 * Sticky lock: once set, never swap to another entity until the session ends.
	 * Alive + within psychic range keeps control even if crosshair misses.
	 */
	private static EntityLivingBase resolveStickyTarget(EntityPlayer player, Session session, int now) {
		if (session == null) return null;

		EntityLivingBase locked = getLockedTarget(player, session);
		if (locked != null) {
			session.lastChannelTick = now;
			return locked;
		}

		// Lock cleared / invalid — acquire via stock Legends pin/crosshair (server reads
		// CURRENT_TARGET_ID synced from client). Skip-ray is plant fallback only.
		if (session.targetId > 0) {
			clearLockedTarget(player, session);
		}

		EntityLivingBase legends = LegendsHelper.getTarget(player, PSYCHIC_RANGE);
		EntityLivingBase acquire = legends != null ? legends
				: HypnosisRayTrace.getLivingFromCrosshairs(player, 1.0F, PSYCHIC_RANGE);
		if (acquire == null || !canControlTarget(acquire)) {
			return null;
		}
		if (acquire instanceof EntityPlayer && ((EntityPlayer) acquire).capabilities.isCreativeMode) {
			return null;
		}
		// First lock still requires touch range; stickiness ignores brief LoS gaps after.
		if (!PlayerHelper.canBeTouched(player, acquire)) {
			return null;
		}

		session.targetId = acquire.getEntityId();
		session.boundThisSession = false;
		session.lastChannelTick = now;
		WitcheryUtil.resetHypnosisAttempts(acquire);
		return acquire;
	}

	private static EntityLivingBase getLockedTarget(EntityPlayer player, Session session) {
		if (player == null || session == null || session.targetId <= 0) return null;
		Entity entity = player.worldObj.getEntityByID(session.targetId);
		if (!(entity instanceof EntityLivingBase)) return null;
		EntityLivingBase living = (EntityLivingBase) entity;
		if (!living.isEntityAlive()) return null;
		if (player.getDistanceToEntity(living) > PSYCHIC_RANGE) return null;
		if (!canControlTarget(living)) return null;
		return living;
	}

	private static void clearLockedTarget(EntityPlayer player, Session session) {
		if (session == null) return;
		if (session.targetId > 0 && player != null && player.worldObj != null) {
			Entity old = player.worldObj.getEntityByID(session.targetId);
			if (old instanceof EntityLivingBase) {
				WitcheryUtil.resetHypnosisAttempts((EntityLivingBase) old);
			}
		}
		session.targetId = -1;
		session.boundThisSession = false;
	}

	private static void pinCurrentTargetId(EntityPlayer player, EntityLivingBase target) {
		if (player == null || target == null) return;
		int id = target.getEntityId();
		Integer cur = (Integer) LegendsProperties.CURRENT_TARGET_ID.get(player);
		if (cur != null && cur.intValue() == id) return;
		if (player.worldObj.isRemote) {
			LegendsProperties.CURRENT_TARGET_ID.setWithoutNotify(player, Integer.valueOf(id));
		} else {
			LegendsProperties.CURRENT_TARGET_ID.set(player, Integer.valueOf(id));
		}
	}

	private static EntityLivingBase resolvePinnedTarget(EntityPlayer player) {
		if (player == null || player.worldObj == null) return null;
		Integer cur = (Integer) LegendsProperties.CURRENT_TARGET_ID.get(player);
		if (cur == null || cur.intValue() <= 0) return null;
		Entity entity = player.worldObj.getEntityByID(cur.intValue());
		if (!(entity instanceof EntityLivingBase)) return null;
		EntityLivingBase living = (EntityLivingBase) entity;
		if (!living.isEntityAlive()) return null;
		if (player.getDistanceToEntity(living) > PSYCHIC_RANGE) return null;
		if (!canControlTarget(living)) return null;
		return living;
	}

	private static void clearCurrentTargetId(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		Integer cur = (Integer) LegendsProperties.CURRENT_TARGET_ID.get(player);
		if (cur == null || cur.intValue() == 0) return;
		if (player.worldObj.isRemote) {
			LegendsProperties.CURRENT_TARGET_ID.setWithoutNotify(player, Integer.valueOf(0));
		} else {
			LegendsProperties.CURRENT_TARGET_ID.set(player, Integer.valueOf(0));
		}
	}

	private static void endSession(EntityPlayer player, Session s, net.minecraft.world.World world) {
		if (s == null) return;
		if (s.targetId > 0 && world != null) {
			Entity old = world.getEntityByID(s.targetId);
			if (old instanceof EntityLivingBase) {
				WitcheryUtil.resetHypnosisAttempts((EntityLivingBase) old);
			}
		}
		SESSIONS.remove(player.getUniqueID());
		clearCurrentTargetId(player);
	}

	/** Inlined Legends 5.4 {@code LegendsHelper.canLiftTarget} eligibility. */
	private static boolean canControlTarget(EntityLivingBase target) {
		return target != null
				&& !(target instanceof IKaiju)
				&& !(target instanceof EntityPunchingBag)
				&& !(target instanceof EntityStand)
				&& target.height < 20.0F;
	}
}
