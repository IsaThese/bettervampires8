package de.teamlapen.vampirism.entity.ai;

import de.teamlapen.vampirism.entity.EntityVampire;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

public class VampireAIWatchLordPlayer extends EntityAIBase {

    private final EntityVampire vampire;
    private EntityPlayer targetLord;
    private final float maxDistance;
    private int lookTime;
    
    // Tracks the world time tick when the vampire is allowed to look at the lord again
    private long cooldownEndTick; 

    public VampireAIWatchLordPlayer(EntityVampire vampire, float maxDistance) {
        this.vampire = vampire;
        this.maxDistance = maxDistance;
        this.setMutexBits(2); // Locks the look helper while this task runs
    }

    @Override
    public boolean shouldExecute() {
        if (this.vampire.getAttackTarget() != null) {
            return false;
        }

        // 1. DONT LOOK WHILE MOVING
        // If the vampire is currently navigating/walking, do not start staring.
        if (!this.vampire.getNavigator().noPath()) {
            return false;
        }

        // 2. THE 1-SECOND BREAK CONTACT COOLDOWN
        // Prevents the AI from instantly restarting the moment it finishes a stare cycle.
        if (this.vampire.worldObj.getTotalWorldTime() < this.cooldownEndTick) {
            return false;
        }

        // 3. INSTANT DETECT
        // No probability checks here. If they are allowed to look, they will look immediately.
        EntityPlayer closest = this.vampire.worldObj.getClosestPlayerToEntity(this.vampire, this.maxDistance);
        
        if (closest != null && VampirePlayer.get(closest).isVampireLord()) {
            this.targetLord = closest;
            return true;
        }
        return false;
    }

    @Override
    public boolean continueExecuting() {
        if (this.vampire.getAttackTarget() != null) {
            return false;
        }
        
        // 4. BREAK EYE CONTACT IMMEDIATELY IF THEY START MOVING
        // If the vampire receives a pathfinding goal to walk, break eye contact instantly.
        if (!this.vampire.getNavigator().noPath()) {
            return false;
        }
        
        if (!this.targetLord.isEntityAlive()) {
            return false;
        }
        if (this.vampire.getDistanceSqToEntity(this.targetLord) > (double)(this.maxDistance * this.maxDistance)) {
            return false;
        }
        return this.lookTime > 0 && VampirePlayer.get(this.targetLord).isVampireLord();
    }

    @Override
    public void startExecuting() {
        // Stares continuously for 3 to 5 seconds (60 to 100 ticks)
        this.lookTime = 60 + this.vampire.getRNG().nextInt(40);
    }

    @Override
    public void resetTask() {
        this.targetLord = null;
        
        // 5. ENGAGE 1-SECOND LOOK AWAY BUFFER
        // 20 ticks = exactly 1 second. When a stare cycle ends OR they break contact to move,
        // they are forced to look away/ignore you for 1 second before they can snap back.
        this.cooldownEndTick = this.vampire.worldObj.getTotalWorldTime() + 20*3;
    }

    @Override
    public void updateTask() {
        // Anti-shaking vertical look calculation
        double dx = this.targetLord.posX - this.vampire.posX;
        double dz = this.targetLord.posZ - this.vampire.posZ;
        double horizontalDistSq = dx * dx + dz * dz;

        if (horizontalDistSq < 0.25) { 
            float yaw = this.vampire.rotationYaw;
            double lookX = -Math.sin(Math.toRadians(yaw)) * 0.1;
            double lookZ = Math.cos(Math.toRadians(yaw)) * 0.1;
            
            this.vampire.getLookHelper().setLookPosition(
                this.vampire.posX + lookX, 
                this.targetLord.posY + (double)this.targetLord.getEyeHeight(), 
                this.vampire.posZ + lookZ, 
                10.0F, 
                (float)this.vampire.getVerticalFaceSpeed()
            );
        } else {
            this.vampire.getLookHelper().setLookPositionWithEntity(this.targetLord, 10.0F, (float)this.vampire.getVerticalFaceSpeed());
        }
        
        this.lookTime--;
    }
}