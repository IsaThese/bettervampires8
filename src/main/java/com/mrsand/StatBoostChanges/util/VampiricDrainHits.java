package com.mrsand.StatBoostChanges.util;

import java.util.List;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Config-driven Vampiric Drain look targeting: longer reach + wider AABB expand
 * so large bosses (Queen) stay easy to lock. Sticky {@code CURRENT_TARGET_ID}
 * matches stock Legends {@code getTarget}.
 */
public final class VampiricDrainHits {

	public static final String NBT_DRAIN_LATCHED = VampireLatchHits.Channel.DRAIN.latchedKey;

	private VampiricDrainHits() {}

	/** Closest point distance from eyes to entity AABB (0 if inside). */
	public static double distanceToAabb(EntityPlayer player, Entity entity) {
		if (player == null || entity == null) return Double.MAX_VALUE;
		Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
		AxisAlignedBB box = entity.boundingBox;
		double cx = clamp(eye.xCoord, box.minX, box.maxX);
		double cy = clamp(eye.yCoord, box.minY, box.maxY);
		double cz = clamp(eye.zCoord, box.minZ, box.maxZ);
		double dx = eye.xCoord - cx;
		double dy = eye.yCoord - cy;
		double dz = eye.zCoord - cz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static double clamp(double v, double lo, double hi) {
		if (v < lo) return lo;
		if (v > hi) return hi;
		return v;
	}

	/**
	 * Drop-in for drain's {@code LegendsHelper.getTarget(player, 10)}:
	 * client acquires with range/wideness and syncs target id; both sides
	 * return sticky target while still in range.
	 */
	public static EntityLivingBase getDrainTarget(EntityPlayer player) {
		if (player == null || player.worldObj == null) return null;
		double range = StatBoostConfig.vampiricDrainRange;
		float widen = StatBoostConfig.vampiricDrainWideness;
		boolean remote = player.worldObj.isRemote;

		if (remote) {
			EntityLivingBase cross = findWideCrosshair(player, range, widen);
			if (cross != null) {
				try {
					LegendsProperties.CURRENT_TARGET_ID.set(player, Integer.valueOf(cross.getEntityId()));
				} catch (Throwable ignored) {
				}
			}
		}

		EntityLivingBase sticky = getStickyTarget(player, range);
		if (sticky != null) {
			if (!remote) {
				tryDrainLatch(player, sticky);
			}
			return sticky;
		}
		if (!remote) {
			EntityLivingBase found = findWideCrosshair(player, range, widen);
			if (found != null) {
				tryDrainLatch(player, found);
			}
			return found;
		}
		return null;
	}

	/** First lock this drain cast: Witchery blood once. Idempotent. */
	public static void tryDrainLatch(EntityPlayer player, EntityLivingBase target) {
		VampireLatchHits.tryDrainLatch(player, target);
	}

	public static void clearLatchIfDrainEnded(EntityPlayer player) {
		VampireLatchHits.clearEndedChannels(player);
	}

	private static EntityLivingBase getStickyTarget(EntityPlayer player, double range) {
		try {
			Object raw = LegendsProperties.CURRENT_TARGET_ID.get(player);
			int id = raw instanceof Integer ? ((Integer) raw).intValue() : 0;
			if (id <= 0) return null;
			Entity e = player.worldObj.getEntityByID(id);
			if (!(e instanceof EntityLivingBase)) return null;
			double centerDist = player.getDistanceToEntity(e);
			double aabbDist = distanceToAabb(player, e);
			// Giant body: AABB can be in range while center is not. OreSpawn parts
			// (QueenHead) often have broken/offset AABBs — keep lock if either metric fits.
			if (aabbDist > range && centerDist > range) {
				return null;
			}
			return (EntityLivingBase) e;
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Prefer look-ray AABB hits; fall back to nearest living whose collision box
	 * is within range and roughly in front (dot &gt; 0). Search volume is a cube
	 * of {@code distance} so large boss boxes are always candidates.
	 */
	@SuppressWarnings("unchecked")
	public static EntityLivingBase findWideCrosshair(EntityPlayer player, double distance, float wideness) {
		if (player == null || player.worldObj == null) return null;
		float partial = 1.0F;
		double d0 = distance;
		float expand = Math.max(0.0F, wideness);
		Vec3 eye = player.getPosition(partial);
		Vec3 look = player.getLook(partial);
		Vec3 end = eye.addVector(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0);

		Entity pointed = null;
		double closestRay = d0;
		Entity pointedPart = null;
		double closestPartRay = d0;
		Entity fallback = null;
		double closestAabb = d0;
		Entity fallbackPart = null;
		double closestPartAabb = d0;

		// Cube around player so Queen-sized AABBs are included even off the thin look corridor.
		AxisAlignedBB search = player.boundingBox.expand(d0 + expand, d0 + expand, d0 + expand);
		List<Entity> list = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, search);

		for (int i = 0; i < list.size(); i++) {
			Entity entity = list.get(i);
			if (!(entity instanceof EntityLivingBase)) continue;
			if (!entity.canBeCollidedWith()) continue;

			double centerDist = player.getDistanceToEntity(entity);
			double aabbDist = distanceToAabb(player, entity);
			// Same union rule as sticky: broken part AABBs still count via center.
			if (aabbDist > d0 && centerDist > d0) continue;

			boolean part = isBossPartEntity(entity);
			float border = entity.getCollisionBorderSize() + expand;
			AxisAlignedBB box = entity.boundingBox.expand(border, border, border);
			MovingObjectPosition mop = box.calculateIntercept(eye, end);
			double reachMetric = Math.min(aabbDist, centerDist);

			if (box.isVecInside(eye) || mop != null) {
				double dist = box.isVecInside(eye) ? 0.0D : eye.distanceTo(mop.hitVec);
				if (part) {
					if (dist < closestPartRay) {
						pointedPart = entity;
						closestPartRay = dist;
					}
				} else if (dist < closestRay) {
					pointed = entity;
					closestRay = dist;
				}
			}

			// Forward hemisphere fallback (looking roughly toward the box).
			double midX = (entity.boundingBox.minX + entity.boundingBox.maxX) * 0.5D;
			double midY = (entity.boundingBox.minY + entity.boundingBox.maxY) * 0.5D;
			double midZ = (entity.boundingBox.minZ + entity.boundingBox.maxZ) * 0.5D;
			double vx = midX - eye.xCoord;
			double vy = midY - eye.yCoord;
			double vz = midZ - eye.zCoord;
			double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
			if (vlen > 1.0E-4D) {
				double dot = (vx * look.xCoord + vy * look.yCoord + vz * look.zCoord) / vlen;
				if (dot > 0.15D) {
					if (part) {
						if (reachMetric < closestPartAabb) {
							fallbackPart = entity;
							closestPartAabb = reachMetric;
						}
					} else if (reachMetric < closestAabb) {
						fallback = entity;
						closestAabb = reachMetric;
					}
				}
			}
		}

		// Prefer main body (TheQueen) over OreSpawn part entities (QueenHead).
		Entity chosen = pointed != null ? pointed
				: (fallback != null ? fallback
				: (pointedPart != null ? pointedPart : fallbackPart));
		return chosen instanceof EntityLivingBase ? (EntityLivingBase) chosen : null;
	}

	/** OreSpawn / multipart bosses expose Head/Body parts that steal look lock. */
	private static boolean isBossPartEntity(Entity entity) {
		if (entity == null) return false;
		String n = entity.getClass().getSimpleName();
		return n.endsWith("Head") || n.endsWith("Body") || n.endsWith("Part");
	}

	/** True on blood-pulse ticks (same cadence as stock drain blood grant). */
	public static boolean isDrainDamagePulse(EntityPlayer player) {
		if (player == null) return false;
		try {
			return com.tihyo.legends.management.LegendsHelper.getTicks(player) % 10 == 0;
		} catch (Throwable t) {
			return player.ticksExisted % 10 == 0;
		}
	}
}
