package com.mrsand.StatBoostChanges.util;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Hypnosis Mastery rays that ignore decorative plants / pots so grab and drag
 * reach entities and solid destinations behind grass, flowers, and similar clutter.
 */
public final class HypnosisRayTrace {

	private static final int MAX_SKIP = 32;
	private static final double NUDGE = 0.05D;

	private HypnosisRayTrace() {}

	public static boolean isHypnosisPassThrough(World world, int x, int y, int z) {
		if (world == null) return false;
		Block block = world.getBlock(x, y, z);
		return isHypnosisPassThrough(block, world, x, y, z);
	}

	public static boolean isHypnosisPassThrough(Block block, World world, int x, int y, int z) {
		if (block == null) return false;
		Material mat = block.getMaterial();
		if (mat == Material.plants || mat == Material.vine) {
			return true;
		}
		if (block instanceof BlockFlower
				|| block instanceof BlockTallGrass
				|| block instanceof BlockDoublePlant
				|| block instanceof BlockFlowerPot) {
			return true;
		}
		if (block instanceof BlockBush && (mat == Material.plants || mat == Material.vine
				|| mat == Material.air || mat.isReplaceable())) {
			return true;
		}
		// Mod flowers (BoP, etc.): replaceable fluff that does not block movement.
		if (world != null && block.isReplaceable(world, x, y, z) && !mat.blocksMovement()) {
			return true;
		}
		return false;
	}

	/**
	 * Stock {@code World.func_147447_a} but continues through pass-through decorations.
	 */
	public static MovingObjectPosition rayTraceSkipPassThrough(World world, Vec3 from, Vec3 to) {
		if (world == null || from == null || to == null) return null;
		Vec3 start = Vec3.createVectorHelper(from.xCoord, from.yCoord, from.zCoord);
		double dx = to.xCoord - from.xCoord;
		double dy = to.yCoord - from.yCoord;
		double dz = to.zCoord - from.zCoord;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0E-6D) return null;
		dx /= len;
		dy /= len;
		dz /= len;

		for (int i = 0; i < MAX_SKIP; i++) {
			MovingObjectPosition hit = world.func_147447_a(start, to, false, false, true);
			if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
				return hit;
			}
			if (!isHypnosisPassThrough(world, hit.blockX, hit.blockY, hit.blockZ)) {
				return hit;
			}
			// Nudge past this decoration and keep going.
			start = Vec3.createVectorHelper(
					hit.hitVec.xCoord + dx * NUDGE,
					hit.hitVec.yCoord + dy * NUDGE,
					hit.hitVec.zCoord + dz * NUDGE);
		}
		return world.func_147447_a(start, to, false, false, true);
	}

	public static MovingObjectPosition rayTraceFromEyes(EntityLivingBase entity, double distance, float partialTicks) {
		if (entity == null || entity.worldObj == null) return null;
		Vec3 eye = entity.getPosition(partialTicks);
		Vec3 look = entity.getLook(partialTicks);
		Vec3 end = eye.addVector(look.xCoord * distance, look.yCoord * distance, look.zCoord * distance);
		return rayTraceSkipPassThrough(entity.worldObj, eye, end);
	}

	/**
	 * Crosshair living target using skip-ray for block occlusion (entity AABB unchanged).
	 */
	@SuppressWarnings("unchecked")
	public static EntityLivingBase getLivingFromCrosshairs(EntityPlayer player, float partialTicks, double distance) {
		if (player == null || player.worldObj == null) return null;
		double d0 = distance;
		MovingObjectPosition posHit = rayTraceFromEyes(player, d0, partialTicks);
		double d1 = d0;
		Vec3 vec3 = player.getPosition(partialTicks);
		Vec3 vec31 = player.getLook(partialTicks);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
		Entity pointedEntity = null;
		Vec3 vec33 = null;
		float f1 = 1.0F;
		List<Entity> list = player.worldObj.getEntitiesWithinAABBExcludingEntity(player,
				player.boundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
						.expand(f1, f1, f1));
		double d2 = d1;
		for (int i = 0; i < list.size(); i++) {
			Entity entity = list.get(i);
			if (!entity.canBeCollidedWith()) continue;
			float f2 = entity.getCollisionBorderSize();
			AxisAlignedBB axisalignedbb = entity.boundingBox.expand(f2, f2, f2);
			MovingObjectPosition mop = axisalignedbb.calculateIntercept(vec3, vec32);
			if (axisalignedbb.isVecInside(vec3)) {
				if (0.0D < d2 || d2 == 0.0D) {
					pointedEntity = entity;
					vec33 = mop == null ? vec3 : mop.hitVec;
					d2 = 0.0D;
				}
			} else if (mop != null) {
				double d3 = vec3.distanceTo(mop.hitVec);
				if (d3 < d2 || d2 == 0.0D) {
					if (entity == player.ridingEntity && !entity.canRiderInteract()) {
						if (d2 == 0.0D) {
							pointedEntity = entity;
							vec33 = mop.hitVec;
						}
					} else {
						pointedEntity = entity;
						vec33 = mop.hitVec;
						d2 = d3;
					}
				}
			}
		}
		if (pointedEntity != null && (d2 < d1 || posHit == null)) {
			posHit = new MovingObjectPosition(pointedEntity, vec33);
		}
		if (posHit != null && posHit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
				&& posHit.entityHit instanceof EntityLivingBase) {
			return (EntityLivingBase) posHit.entityHit;
		}
		return null;
	}
}
