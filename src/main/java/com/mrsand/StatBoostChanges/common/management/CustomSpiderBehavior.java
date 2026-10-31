package com.mrsand.StatBoostChanges.common.management;


import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class CustomSpiderBehavior extends EntitySpider {

    public CustomSpiderBehavior(World world) {
        super(world);
    }

    @Override
    protected Entity findPlayerToAttack() {
        float f = this.getBrightness(1.0F);

        if (f < 0.5F) {
            double d0 = 16.0D;
            EntityPlayer player = this.worldObj.getClosestVulnerablePlayerToEntity(this, d0);
            
            if (player != null) {
                GeneralUtil.sendDebugMessage(player, "Checking if mob should attack...");
                // If your custom utility says DON'T attack, return null
                if (LegendsUtil.mobAttack(player)) {
                    return null;
                }
            }
            return player;
        }
        return null;
    }
}
