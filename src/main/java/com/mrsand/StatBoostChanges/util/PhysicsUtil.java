package com.mrsand.StatBoostChanges.util;

import java.util.List;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.tihyo.legends.management.LegendsHelper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PhysicsUtil {

	public static void handlePhasingPhysics(EntityPlayer player) {
	    World world = player.worldObj;
	    
	    // We use the player's bounding box to find exactly which blocks they occupy.
	    // We shrink it slightly (0.05) so we don't count blocks we are just "touching."
	    AxisAlignedBB box = player.boundingBox.contract(0.05D, 0.05D, 0.05D);
	    
	    int minX = MathHelper.floor_double(box.minX);
	    int maxX = MathHelper.floor_double(box.maxX);
	    int minY = MathHelper.floor_double(box.minY);
	    int maxY = MathHelper.floor_double(box.maxY);
	    int minZ = MathHelper.floor_double(box.minZ);
	    int maxZ = MathHelper.floor_double(box.maxZ);
	
	    boolean isStuck = false;
	    boolean canPhase = false;
	    boolean onPorousFloor = false;
	
	    // Track for nudging
	    double targetX = player.posX;
	    double targetZ = player.posZ;
	    double bestDist = 999.0D;
	
	    // 1. COLLISION & SCANNING (Combined for efficiency)
	    for (int x = minX - 1; x <= maxX + 1; x++) {
	        for (int z = minZ - 1; z <= maxZ + 1; z++) {
	            for (int y = minY; y <= maxY + 1; y++) {
	                Block block = world.getBlock(x, y, z);
	                
	                // --- STUCK CHECK ---
	                // If any block inside our ACTUAL bounding box is solid and non-porous
	                if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
	                    if (block.getMaterial().isSolid() && !PhysicsUtil.isBlockPorous(block)) {
	                        isStuck = true;
	                    }
	                }
	
	                // --- PHASING SCAN ---
	                if (PhysicsUtil.isBlockPorous(block)) {
	                    canPhase = true;
	                }
	
	                // Check for 1-block gaps or air nearby
	                Block feetBlock = world.getBlock(x, MathHelper.floor_double(player.posY), z);
	                Block headBlock = world.getBlock(x, MathHelper.floor_double(player.posY + 1.0D), z);
	                
	                boolean isGap = (feetBlock.getMaterial() == Material.air || PhysicsUtil.isBlockPorous(feetBlock)) && 
	                                (headBlock.getMaterial().isSolid() && !PhysicsUtil.isBlockPorous(headBlock));
	                boolean isSafe = (feetBlock.getMaterial() == Material.air && headBlock.getMaterial() == Material.air);
	
	                if (isGap || isSafe) {
	                    canPhase = true;
	                    double dSq = player.getDistanceSq(x + 0.5D, player.posY, z + 0.5D);
	                    if (dSq < bestDist) {
	                        bestDist = dSq;
	                        targetX = x + 0.5D;
	                        targetZ = z + 0.5D;
	                    }
	                }
	
	                // Porous Floor Check (py - 1)
	                if (y == MathHelper.floor_double(player.posY - 0.1D) && PhysicsUtil.isBlockPorous(block)) {
	                    onPorousFloor = true;
	                }
	            }
	        }
	    }
	
	    // 2. PHYSICS EXECUTION
	    if (canPhase || onPorousFloor || isStuck) {
	        player.noClip = true;
	
	        // Nudge toward openings
	        if (canPhase && bestDist > 0.1D) {
	            player.motionX += (targetX - player.posX) * 0.05D;
	            player.motionZ += (targetZ - player.posZ) * 0.05D;
	        }
	
	        // Vertical Logic
	        if (player.isSneaking()) {
	            player.motionY = -0.15D;
	        } 
	        else if (player.isJumping) { // isJumping
	            player.motionY = 0.2D;
	        } 
	        else if (isStuck) {
	            // THE ANTI-FLICKER DRIFT:
	            // Constant upward pressure ensures we break out of floors.
	            player.motionY = 0.12D; 
	            
	            // If stuck in a floor, provide a hard nudge to exit the block faster
	            if (world.isAirBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY + 1.5D), MathHelper.floor_double(player.posZ))) {
	                player.motionY = 0.2D;
	            }
	        } 
	        else {
	            player.motionY = 0.0D; // Neutral buoyancy
	        }
	        return;
	    }
	
	// 3. CLEANUP
	    if (!player.capabilities.isCreativeMode) {
	        player.noClip = false;
	    }
	}

	/**
	 * Mist-form phasing (original BV buoyancy + L8 always-noClip).
	 * Hover at foot level (motionY=0); sneak to sink; jump to rise.
	 * Stuck escape ignores sole-overlap with the floor under always-noClip.
	 */
	public static void handleMistPhasing(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		World world = player.worldObj;

		AxisAlignedBB box = player.boundingBox.contract(0.05D, 0.05D, 0.05D);
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.floor_double(box.maxX);
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.floor_double(box.maxZ);
		// Don't treat the block under the soles as "stuck" (always-noClip sinks slightly).
		int stuckMinY = MathHelper.floor_double(player.posY + 0.2D);

		boolean isStuck = false;
		boolean canPhase = false;
		boolean onPorousFloor = false;
		double targetX = player.posX;
		double targetZ = player.posZ;
		double bestDist = 999.0D;

		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int z = minZ - 1; z <= maxZ + 1; z++) {
				for (int y = minY; y <= maxY + 1; y++) {
					Block block = world.getBlock(x, y, z);

					if (x >= minX && x <= maxX && y >= stuckMinY && y <= maxY && z >= minZ && z <= maxZ) {
						if (block.getMaterial().isSolid() && !isBlockPorous(block)) {
							isStuck = true;
						}
					}

					if (isBlockPorous(block)) {
						canPhase = true;
					}

					Block feetBlock = world.getBlock(x, MathHelper.floor_double(player.posY), z);
					Block headBlock = world.getBlock(x, MathHelper.floor_double(player.posY + 1.0D), z);
					boolean isGap = (feetBlock.getMaterial() == Material.air || isBlockPorous(feetBlock))
							&& (headBlock.getMaterial().isSolid() && !isBlockPorous(headBlock));
					boolean isSafe = feetBlock.getMaterial() == Material.air && headBlock.getMaterial() == Material.air;
					if (isGap || isSafe) {
						canPhase = true;
						double dSq = player.getDistanceSq(x + 0.5D, player.posY, z + 0.5D);
						if (dSq < bestDist) {
							bestDist = dSq;
							targetX = x + 0.5D;
							targetZ = z + 0.5D;
						}
					}

					if (y == MathHelper.floor_double(player.posY - 0.1D) && isBlockPorous(block)) {
						onPorousFloor = true;
					}
				}
			}
		}

		if (player.isEntityInsideOpaqueBlock()) {
			isStuck = true;
		}

		player.noClip = true;

		if ((canPhase || onPorousFloor) && bestDist > 0.1D && bestDist < 4.0D) {
			player.motionX += (targetX - player.posX) * 0.05D;
			player.motionZ += (targetZ - player.posZ) * 0.05D;
		}

		// Vertical: hover unless sneak/jump/stuck (flight lift stays cancelled)
		if (player.isSneaking()) {
			player.motionY = StatBoostConfig.mistSinkSpeed;
		} else if (player.isJumping) {
			player.motionY = StatBoostConfig.mistJumpSpeed;
		} else if (isStuck) {
			player.motionY = 0.12D;
			if (world.isAirBlock(
					MathHelper.floor_double(player.posX),
					MathHelper.floor_double(player.posY + 1.5D),
					MathHelper.floor_double(player.posZ))) {
				player.motionY = 0.2D;
			}
		} else {
			player.motionY = 0.0D; // Neutral buoyancy — hover; shift to sink
		}

		applyMistHorizontalSpeed(player);
	}

	/** WASD push + speed cap while misted (config-tuned). */
	private static void applyMistHorizontalSpeed(EntityPlayer player) {
		float forward = player.moveForward;
		float strafe = player.moveStrafing;
		if (forward != 0.0F || strafe != 0.0F) {
			float len = MathHelper.sqrt_float(forward * forward + strafe * strafe);
			if (len < 1.0E-4F) return;
			forward /= len;
			strafe /= len;
			float yaw = player.rotationYaw * (float) Math.PI / 180.0F;
			double sin = MathHelper.sin(yaw);
			double cos = MathHelper.cos(yaw);
			double speed = StatBoostConfig.mistHorizontalSpeed;
			player.motionX += (strafe * cos - forward * sin) * speed;
			player.motionZ += (forward * cos + strafe * sin) * speed;
		}

		double max = StatBoostConfig.mistMaxHorizontalSpeed;
		if (max <= 0.0D) return;
		double horiz = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
		if (horiz > max) {
			double scale = max / horiz;
			player.motionX *= scale;
			player.motionZ *= scale;
		}
	}

	/** Re-assert after Legends LivingUpdate clears noClip on non-phasing players. */
	public static void ensureMistNoClip(EntityPlayer player) {
		if (player == null) return;
		player.noClip = true;
	}

	/**
	 * After mist/bat exit: if embedded, teleport to nearest 2-high air with solid floor.
	 * @return true if safe (or not stuck); false if still stuck (kept noClip for retry)
	 */
	@SuppressWarnings("unchecked")
	public static boolean ejectFromSolidsIfNeeded(EntityPlayer player) {
		if (player == null || player.worldObj == null) return true;
		World world = player.worldObj;

		if (!needsSolidEject(player)) {
			return true;
		}

		int px = MathHelper.floor_double(player.posX);
		int py = MathHelper.floor_double(player.posY);
		int pz = MathHelper.floor_double(player.posZ);

		double bestGroundedDistSq = -1.0D;
		double gX = 0, gY = 0, gZ = 0;
		boolean foundGrounded = false;

		for (int x = px - 5; x <= px + 5; x++) {
			for (int z = pz - 5; z <= pz + 5; z++) {
				for (int y = py - 2; y <= py + 4; y++) {
					if (!world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) continue;

					Block floor = world.getBlock(x, y - 1, z);
					boolean hasGround = floor.getMaterial().isSolid() || floor.isOpaqueCube();
					if (!hasGround) continue; // final snap must stand on solid/opaque

					double dx = (x + 0.5D) - player.posX;
					double dy = y - player.posY;
					double dz = (z + 0.5D) - player.posZ;
					double distSq = dx * dx + dy * dy + dz * dz;
					if (!foundGrounded || distSq < bestGroundedDistSq) {
						bestGroundedDistSq = distSq;
						gX = x + 0.5D;
						gY = y;
						gZ = z + 0.5D;
						foundGrounded = true;
					}
				}
			}
		}

		if (foundGrounded) {
			player.setPositionAndUpdate(gX, gY, gZ);
			return true;
		}

		player.noClip = true;
		return false;
	}

	@SuppressWarnings("unchecked")
	private static boolean needsSolidEject(EntityPlayer player) {
		if (player.isEntityInsideOpaqueBlock()) return true;

		int fx = MathHelper.floor_double(player.posX);
		int fy = MathHelper.floor_double(player.posY);
		int fz = MathHelper.floor_double(player.posZ);
		World world = player.worldObj;
		if (!world.isAirBlock(fx, fy, fz) || !world.isAirBlock(fx, fy + 1, fz)) {
			return true;
		}

		List colliding = world.getCollidingBoundingBoxes(player, player.boundingBox);
		return colliding != null && !colliding.isEmpty();
	}

	public static boolean isBlockPorous(Block block) {
		return 
				//				block instanceof net.minecraft.block.BlockFence || 
				block instanceof net.minecraft.block.BlockPane || // Glass panes & Iron bars
				block instanceof net.minecraft.block.BlockDoor || 
				block instanceof net.minecraft.block.BlockTrapDoor ||
				block instanceof net.minecraft.block.BlockWeb ||
				block instanceof net.minecraft.block.BlockLeaves ||
				block instanceof BlockGlass || 
				block instanceof BlockStainedGlass ||
				block instanceof BlockStainedGlassPane;
		//				block instanceof BlockWall;
	}

	private static boolean isPathPassable(World world, EntityPlayer player, int tx, int tz) {
		int px = MathHelper.floor_double(player.posX);
		int py = MathHelper.floor_double(player.posY);
		int pz = MathHelper.floor_double(player.posZ);
	
		// Get the blocks the player is currently occupying or about to occupy
		// We check the path by looking at the blocks between current (px, pz) and target (tx, tz)
		int stepX = Integer.compare(tx, px);
		int stepZ = Integer.compare(tz, pz);
	
		// We check the "Intervening" blocks
		for (int ix = px; ix != tx + stepX; ix += (stepX == 0 ? 1 : stepX)) {
			for (int iz = pz; iz != tz + stepZ; iz += (stepZ == 0 ? 1 : stepZ)) {
	
				Block bFeet = world.getBlock(ix, py, iz);
				Block bHead = world.getBlock(ix, py + 1, iz);
	
				// LOGIC: Is this specific 1x2 column "Passable"?
				// - FULL PHASE: Both blocks are Porous or Air.
				// - SQUEEZE: Bottom is Porous/Air, Top is Solid.
				// - BLOCKED: Bottom is a Solid non-porous block.
	
				boolean feetPassable = (bFeet.getMaterial() == Material.air || isBlockPorous(bFeet));
	
				if (!feetPassable) return false; // If feet are blocked by solid stone, the mist stops.
	
				// If the code reaches here, the feet are clear. 
				// We don't care if the head is solid (that's the squeeze) 
				// but we MUST ensure the head isn't a "Forbidden" solid like Obsidian or Bedrock if you want.
	
				if (stepX == 0 && stepZ == 0) break; // Safety break
			}
			if (stepX == 0) break; // Safety break
		}
		return true;
	}

	/** Exact Legends 5 {@code LegendsMathHelper.rayTrace}: getPosition + getLook. */
	public static MovingObjectPosition rayTrace(EntityLivingBase entity, double distance, float partialTicks) {
		Vec3 vec3 = entity.getPosition(partialTicks);
		Vec3 vec31 = entity.getLook(partialTicks);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * distance, vec31.yCoord * distance, vec31.zCoord * distance);
		return entity.worldObj.func_147447_a(vec3, vec32, false, false, true);
	}

	/** Exact Legends 5 {@code LegendsHelper.getTargetFromCrosshairs}. */
	public static EntityLivingBase getTargetFromCrosshairs(EntityPlayer player, float partialTicks, double distance) {
		double d0 = distance;
		MovingObjectPosition posHit = rayTrace((EntityLivingBase) player, d0, partialTicks);
		double d1 = d0;
		Vec3 vec3 = player.getPosition(partialTicks);
		Vec3 vec31 = player.getLook(partialTicks);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
		Entity pointedEntity = null;
		Vec3 vec33 = null;
		float f1 = 1.0F;
		List<Entity> list = player.worldObj.getEntitiesWithinAABBExcludingEntity((Entity) player, player.boundingBox
				.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f1, f1, f1));
		double d2 = d1;
		for (int i = 0; i < list.size(); i++) {
			Entity entity = list.get(i);
			if (entity.canBeCollidedWith()) {
				float f2 = entity.getCollisionBorderSize();
				AxisAlignedBB axisalignedbb = entity.boundingBox.expand(f2, f2, f2);
				MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
				if (axisalignedbb.isVecInside(vec3)) {
					if (0.0D < d2 || d2 == 0.0D) {
						pointedEntity = entity;
						vec33 = (movingobjectposition == null) ? vec3 : movingobjectposition.hitVec;
						d2 = 0.0D;
					}
				} else if (movingobjectposition != null) {
					double d3 = vec3.distanceTo(movingobjectposition.hitVec);
					if (d3 < d2 || d2 == 0.0D)
						if (entity == player.ridingEntity && !entity.canRiderInteract()) {
							if (d2 == 0.0D) {
								pointedEntity = entity;
								vec33 = movingobjectposition.hitVec;
							}
						} else {
							pointedEntity = entity;
							vec33 = movingobjectposition.hitVec;
							d2 = d3;
						}
				}
			}
		}
		if (pointedEntity != null && (d2 < d1 || posHit == null))
			posHit = new MovingObjectPosition(pointedEntity, vec33);
		if (posHit != null)
			if (posHit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
				if (posHit.entityHit instanceof EntityLivingBase)
					return (EntityLivingBase) posHit.entityHit;
		return null;
	}

	/**
	 * Prefer {@link LegendsHelper#getTarget} for mastery — it syncs {@code CURRENT_TARGET_ID}.
	 * This local copy matches Legends 5 crosshair math for other BV call sites.
	 */
	public static EntityLivingBase getTarget(EntityPlayer player, double distance) {
		return LegendsHelper.getTarget(player, distance);
	}

	public static boolean isMobLookingAtPlayer(EntityLivingBase mob, EntityPlayer player, double angleTolerance) {
		// 1. Calculate the vector from the Mob's eyes to the Player's eyes
		// We normalize it so it has a length of 1.0
		Vec3 mobPos = Vec3.createVectorHelper(mob.posX, mob.posY + mob.getEyeHeight(), mob.posZ);
		Vec3 playerPos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
	
		Vec3 targetVec = playerPos.subtract(mobPos).normalize();
	
		// 2. Get the Mob's ACTUAL head look vector (Using your custom code)
		Vec3 mobLookVec = PhysicsUtil.getHeadLookVec(mob).normalize();
	
		// 3. Dot Product: Compare the two vectors
		// 1.0 = Looking directly at you
		// 0.0 = Looking 90 degrees away
		// -1.0 = Looking directly opposite
		double dot = targetVec.dotProduct(mobLookVec);
	
		// 4. Check if the line of sight is clear (No walls)
		boolean canSee = mob.canEntityBeSeen(player);
	
		// 5. FINAL VERDICT: Must have clear LoS AND be facing the player
		return canSee && dot > angleTolerance;
	}

	public static Vec3 getHeadLookVec(EntityLivingBase entity) {
		float yaw = entity.rotationYawHead;
		float pitch = entity.rotationPitch;
		float f = 0.017453292F; // Degrees to Radians conversion
	
		// FIX: Removed " - (float)Math.PI"
		// This flips the vector 180 degrees to point forward
		float f1 = MathHelper.cos(-yaw * f);
		float f2 = MathHelper.sin(-yaw * f);
		float f3 = -MathHelper.cos(-pitch * f);
		float f4 = MathHelper.sin(-pitch * f);
	
		return Vec3.createVectorHelper((double) (f2 * f3), (double) f4, (double) (f1 * f3));
	}

}
