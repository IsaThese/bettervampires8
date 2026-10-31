package com.mrsand.StatBoostChanges.util;

import java.util.List;

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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
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

	// Copied from legends Code
	public static MovingObjectPosition rayTrace(EntityLivingBase entity, double distance, float partialTicks) {
		Vec3 vec3 = entity.getPosition(partialTicks);
		Vec3 vec31 = entity.getLook(partialTicks);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * distance, vec31.yCoord * distance, vec31.zCoord * distance);
		return entity.worldObj.func_147447_a(vec3, vec32, false, false, true); // func_147447 returns first hit object,
		// and does math
	}

	// Copied from Legends Code
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

	// Modified from Legends Code
	public static EntityLivingBase getTarget(EntityPlayer player, double distance) {
		if (getTargetFromCrosshairs(player, 1.0F, distance) != null) {
			NBTTagCompound nbt = player.getEntityData();
			nbt.setInteger("staringEntityHypnosisID",
					Integer.valueOf(getTargetFromCrosshairs(player, 1.0F, distance).getEntityId()));
		} else {
			player.getEntityData().removeTag("staringEntityHypnosisID");
		}
		Entity entity = player.worldObj.getEntityByID(player.getEntityData().getInteger("staringEntityHypnosisID"));
		if (entity != null && entity instanceof EntityLivingBase && player.getDistanceToEntity(entity) <= distance) {
			return (EntityLivingBase) entity;
		}
		//	      LegendsPropertyModifier.set(player, 44, Integer.valueOf(getTargetFromCrosshairs(player, 1.0F, distance).getEntityId())); 
		//	    if (player.worldObj.getEntityByID(LegendsProperties.getInt(player, 44)) != null && player.worldObj.getEntityByID(LegendsProperties.getInt(player, 44)) instanceof EntityLivingBase && player.getDistanceToEntity(player.worldObj.getEntityByID(LegendsProperties.getInt(player, 44))) <= distance)
		//	      return (EntityLivingBase)player.worldObj.getEntityByID(LegendsProperties.getInt(player, 44)); 
	
		return null;
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
