//package com.mrsand.StatBoostChanges.mixins.late;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//import com.tihyo.legends.abilities.Ability;
//import com.tihyo.legends.abilities.AbilityHelper;
//import com.tihyo.legends.legacy.LegacyHelper;
//import com.tihyo.legends.legacy.LegacyRegistry;
//import com.tihyo.legends.morph.MorphHelper;
//
//import org.spongepowered.asm.mixin.injection.At;
//
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.world.World;
//
//@Mixin(World.class)
//public class WorldMixin {
//	
//	@Shadow public List playerEntities = new ArrayList();
//	
//	
//	@Inject(method = "getClosestVulnerablePlayer", at = @At("HEAD"), cancellable = true)
//	private void vampireNotVulnerable(double x, double y, double z, double distance, CallbackInfoReturnable<EntityPlayer> cir) {
//		
//		
//		    double d4 = -1.0D;
//	        EntityPlayer entityplayer = null;
//
//	        for (int i = 0; i < this.playerEntities.size(); ++i)
//	        {
//	            EntityPlayer entityplayer1 = (EntityPlayer)this.playerEntities.get(i);
//	            
//
//	            if (LegacyHelper.isActive(entityplayer1) && LegacyHelper.getLegacyClass(entityplayer1) == LegacyRegistry.vampire) {
//	                if (entityplayer1.worldObj.isDaytime() && !entityplayer1.worldObj.isRemote && !entityplayer1.isInWater()) {
//
//	            if (!entityplayer1.capabilities.disableDamage && entityplayer1.isEntityAlive())
//	            {
//	                double d5 = entityplayer1.getDistanceSq(x, y, z);
//	                double d6 = distance;
//
//	                if (entityplayer1.isSneaking())
//	                {
//	                    d6 = distance * 0.800000011920929D;
//	                }
//	                
//
//	                if (entityplayer1.isInvisible())
//	                {
//	                    float f = entityplayer1.getArmorVisibility();
//
//	                    if (f < 0.1F)
//	                    {
//	                        f = 0.1F;
//	                    }
//
//	                    d6 *= (double)(0.7F * f);
//	                }
//
//	                if ((distance < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4))
//	                {
//	                    d4 = d5;
//	                    entityplayer = entityplayer1;
//	                }
//	            }
//	        }
//
//	        cir.setReturnValue(entityplayer);
//	    }
//	
//	        }
//	}
//		
//	}
//
