package com.mrsand.StatBoostChanges.mixins.late;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.brewing.potions.PotionEnslaved;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.entity.EntityBolt;
import com.emoniph.witchery.entity.EntityWitchHunter;
import com.emoniph.witchery.network.PacketSound;
import com.emoniph.witchery.util.CreatureUtil;
import com.emoniph.witchery.util.SoundEffect;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
//legends code
@Mixin(value = EntityWitchHunter.class, remap = false)
public abstract class EntityWitchHunterMixin extends EntityCreature {
	public EntityWitchHunterMixin(World p_i1602_1_) {
		super(p_i1602_1_);
	}

	EntityWitchHunter self = (EntityWitchHunter)(Object)this;

	
	
	
	@Inject(method = "func_70636_d", at = @At("TAIL"), cancellable = false)
	public void onLivingUpdate(CallbackInfo ci){
		//Cause targeted vampires to slowdown
		if(self.getAttackTarget() instanceof EntityPlayer) {
		List<Entity> entities = GeneralUtil.getEntityListFromEntity(self, 60);
		for(Entity entity : entities) {
			if(entity instanceof EntityPlayer) {
				EntityPlayer player = (EntityPlayer)entity;
				ExtendedPlayer playerEx = ExtendedPlayer.get(player);
				if(playerEx.isVampire()) {
	                //player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, TimeUtil.secsToTicks(2), 3, true));
	                LegendsUtil.setGrounded(player, 2);
				}
			}
		}
		  }
	}
	
	@Inject(method = "func_70628_a", at = @At("HEAD"), cancellable = true)
	protected void dropFewItems(boolean par1, int par2, CallbackInfo ci){
		ci.cancel();
			int j = (par2 > 0) ? self.worldObj.rand.nextInt(par2 + 1) : 0;
		    for (int k = 0; k < j; k++)
		      self.entityDropItem(Witchery.Items.GENERIC.itemBoltStake.createStack(), 0.0F); 
		    int i = (self.worldObj.rand.nextInt(100) < 70) ? 2 : 0;
		    if (i > 0 && par2 > 0) {		
				i += self.worldObj.rand.nextInt(par2 + 1); 
			}
		    for(int k = 0; k < i; k++)
		    	self.entityDropItem(new ItemStack(Items.emerald, 1), 0.0F);

		    EntityPlayer player = null;
			List<Entity> entities = GeneralUtil.getEntityListFromEntity(self, 60);
			for(Entity entity : entities) {
				if(entity instanceof EntityPlayer) {
					player = (EntityPlayer)entity;
					ExtendedPlayer playerEx = ExtendedPlayer.get(player);
					if(playerEx.isVampire()) {
						SuspicionUtil.decreaseSuspicion(player, 500);
		                Witchery.packetPipeline.sendTo((IMessage)new PacketSound(SoundEffect.WITCHERY_RANDOM_POOF, (Entity)player, 1.0F, 1.0F), player);
		                
		                return;
					}
				}
				else if(entity instanceof EntityCreature) {
					if(PotionEnslaved.isMobEnslaved((EntityCreature)entity)) {
						String ownerName = entity.getEntityData().getString("WITCEnslaverName");
		                if (ownerName != null && !ownerName.isEmpty()) {
		                    EntityPlayer owner = self.worldObj.getPlayerEntityByName(ownerName);
		                    if (owner != null) {
		                        ExtendedPlayer ownerEx = ExtendedPlayer.get(owner);
		                        if (ownerEx != null && ownerEx.isVampire()) {
		                            SuspicionUtil.decreaseSuspicion(owner, 500);
		                            Witchery.packetPipeline.sendTo((IMessage) new PacketSound(SoundEffect.WITCHERY_RANDOM_POOF, (Entity) owner, 1.0F, 1.0F), owner);
		                            return;
		                        }
		                    }
		                }
					}
				}
			}
		  }
	
	@Inject(method = "func_110147_ax", at = @At("TAIL"), cancellable = true)
	protected void applyEntityAttributes(CallbackInfo ci) {
        //(self.getEntityAttribute(SharedMonsterAttributes.movementSpeed)).setBaseValue(0.5D);
	}
//	@Shadow
//    public abstract ItemStack getHeldItem();

    /**
     * @author mrsand
     * @reason Dynamic crossbow damage based on Vampire Level, fully deobfuscated.
     */
	
	// attackEntityWithRangedAttack
    @Overwrite
    public void func_82196_d(EntityLivingBase target, float distanceFactor) {
        // Calculate base variance based on world difficulty
        EntityBolt entityarrow = new EntityBolt(this.worldObj, this, target, 1.6F, (float)(14 - this.worldObj.difficultySetting.getDifficultyId() * 4));
        
        int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, this.getHeldItem());
        int punchLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, this.getHeldItem());
        
        // --- DYNAMIC DAMAGE SCALING ---
        float damageMultiplier = 2.0F; // 2.0F is the Vanilla Witch Hunter base multiplier
        
        // Check if the person we are shooting is a player
        if (target instanceof EntityPlayer) {
            ExtendedPlayer playerEx = ExtendedPlayer.get((EntityPlayer) target);
            if (playerEx != null && playerEx.getVampireLevel() > 0) {
                // Add 0.5 to the multiplier for every vampire level. 
                // Level 1 = 2.5x damage. Level 5 = 4.5x damage. Level 10 = 7.0x damage.
                // You can change 0.5F to whatever scaling feels balanced to you!
                damageMultiplier += (playerEx.getVampireLevel() * 0.5F); 
            }
        }

        // Apply our dynamic multiplier to the final damage calculation
        entityarrow.setDamage((double)(distanceFactor * damageMultiplier) + this.rand.nextGaussian() * 0.25D + (double)((float)this.worldObj.difficultySetting.getDifficultyId() * 0.11F));

        // Apply Enchantments (Power, Punch, Flame)
        if (powerLevel > 0) {
            entityarrow.setDamage(entityarrow.getDamage() + (double)powerLevel * 0.5D + 0.5D);
        }

        if (punchLevel > 0) {
            entityarrow.setKnockbackStrength(punchLevel);
        }

        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, this.getHeldItem()) > 0 || (CreatureUtil.isVampire(this.getAttackTarget()) && this.worldObj.rand.nextInt(3) == 0)) {
            entityarrow.setFire(100);
        }

        // Apply special bolt types against specific Witchery creatures
        if (this.getAttackTarget() != null) {
            if (CreatureUtil.isWerewolf(this.getAttackTarget())) {
                entityarrow.setBoltType(4);
            } else if (CreatureUtil.isUndead(this.getAttackTarget())) {
                entityarrow.setBoltType(3);
            } else if (this.worldObj.rand.nextInt(4) == 0) {
                entityarrow.setBoltType(2);
            }
        }

        // Play the sound and shoot
        this.playSound("random.bow", 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        this.worldObj.spawnEntityInWorld(entityarrow);
    }
}

