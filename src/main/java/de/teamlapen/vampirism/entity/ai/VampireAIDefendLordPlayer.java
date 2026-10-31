package de.teamlapen.vampirism.entity.ai;

import de.teamlapen.vampirism.entity.EntityVampire;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.player.EntityPlayer;

//For regular vampires
public class VampireAIDefendLordPlayer extends EntityAITarget {

    private final EntityVampire vampire;
    private EntityLivingBase attackerTarget;
    private int lastAttackCheckedTime = 0;

    public VampireAIDefendLordPlayer(EntityVampire vampire) {
        super(vampire, false, false);
        this.vampire = vampire;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        // Scan for the nearest Lord player within a standard 16-block bodyguard radius
        EntityPlayer closestLord = this.vampire.worldObj.getClosestPlayerToEntity(this.vampire, 16.0D);
        
        if (closestLord != null && VampirePlayer.get(closestLord).isVampireLord()) {
            
            EntityLivingBase lastAttacker = closestLord.getLastAttacker();
            int hitTime = closestLord.getLastAttackerTime();
            
            // Verify a target exists and that it isn't this vampire itself
            if (lastAttacker != null && lastAttacker != this.vampire) {
                // Track the hit timestamp for internal state consistency
                this.lastAttackCheckedTime = hitTime; 
                
                this.attackerTarget = lastAttacker;
                return this.isSuitableTarget(this.attackerTarget, false);
            }
        }
        
        return false;
    }

    @Override
    public void startExecuting() {
        // 1. Tell the navigation/pathfinding system who to chase and swing at
        this.vampire.setAttackTarget(this.attackerTarget);
        
        // This stops its internal update loops from wiping out the attack target on the next tick!
        this.vampire.setRevengeTarget(this.attackerTarget);
        this.vampire.setTarget(this.attackerTarget);
        
        super.startExecuting();
    }
}