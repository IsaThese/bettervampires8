package com.mrsand.StatBoostChanges.common.management;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIHypnotizedStare extends EntityAIBase {
    protected EntityLivingBase entity; // The mob being hypnotized
    protected EntityLivingBase closestEntity; // The player they are staring at
    private float maxDistance;

    public EntityAIHypnotizedStare(EntityLivingBase entityLiving, float maxDistance) {
        this.entity = entityLiving;
        this.maxDistance = maxDistance;
        
        // Mutex Bit 2 = "Look" task. 
        // Mutex Bit 1 = "Move" task.
        // Setting it to 3 (1 | 2) means "Stop moving AND control the head"
        this.setMutexBits(3); 
    }

    /**
     * Should the task start executing?
     */
    @Override
    public boolean shouldExecute() {
        // 1. Only start if the mob is "tranced"
        if (!this.entity.getEntityData().getBoolean("tranced")) {
            return false;
        }

        // 2. Find the closest player to stare at
        if (this.entity.worldObj.isRemote) {
             // On Client, usually simpler, but AI runs on server.
             return false; 
        }

        // Find closest player within range
        this.closestEntity = this.entity.worldObj.getClosestPlayerToEntity(this.entity, this.maxDistance);
        
        return this.closestEntity != null;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean continueExecuting() {
        // Keep staring as long as:
        // 1. We are still tranced
        // 2. The target is alive
        // 3. The target is still within range
        return this.entity.getEntityData().getBoolean("tranced") 
            && this.closestEntity.isEntityAlive() 
            && this.entity.getDistanceSqToEntity(this.closestEntity) < (double)(this.maxDistance * this.maxDistance);
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void startExecuting() {
        // Optional: Reset navigation to stop them from walking away immediately
        if (this.entity instanceof net.minecraft.entity.EntityLiving) {
            ((net.minecraft.entity.EntityLiving)this.entity).getNavigator().clearPathEntity();
        }
    }

    /**
     * Updates the task
     */
    @Override
    public void updateTask() {
        if (this.closestEntity != null) {
            // This is the magic method the Villager uses to turn its head
            // 30.0F = max yaw speed, 30.0F = max pitch speed
//            this.entity.getLookHelper().setLookPositionWithEntity(this.closestEntity, 30.0F, 30.0F);
        }
    }
    
    @Override
    public void resetTask() {
        this.closestEntity = null;
    }
}
