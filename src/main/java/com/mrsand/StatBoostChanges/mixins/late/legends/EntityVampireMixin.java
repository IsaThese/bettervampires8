package com.mrsand.StatBoostChanges.mixins.late.legends;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.brewing.potions.PotionEnslaved;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.tihyo.horror.entities.mobs.EntityVampire;
import com.tihyo.legends.effects.LegendsEffects;
import com.tihyo.legends.entities.mobs.EntitySmartCreature;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.interfaces.IHostile;
import com.tihyo.legends.management.interfaces.ILegendsCreature;
import com.tihyo.legends.management.interfaces.INightSpawn;
import com.tihyo.legends.management.interfaces.IVulnurableMob;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

@Mixin(value = EntityVampire.class, remap = false)
public abstract class EntityVampireMixin extends EntitySmartCreature implements ILegendsCreature, IHostile, IVulnurableMob, INightSpawn {
    
    //@Shadow public abstract IAttributeInstance func_110148_a(IAttribute attribute);
    //@Shadow public abstract BaseAttributeMap func_110140_aT();
    //@Shadow public World worldObj;
    //@Shadow protected AxisAlignedBB boundingBox;
    
    public EntityVampireMixin() {
        super((World)null);
    }
    
    public EntityVampireMixin(World world, int type) {
        super(world);
    }
    
    
    //Changes MaxHealth
	@Inject(method = "func_110147_ax", at = @At("RETURN"))
    protected void modifyAttributes(CallbackInfo ci) {
    	//ci.cancel();
    	
    	EntityVampire self = (EntityVampire)(Object)this;
        // Override all attributes
        (self.getEntityAttribute(SharedMonsterAttributes.maxHealth)).setBaseValue(40.0D); // maxHealth 100
        (self.getEntityAttribute(SharedMonsterAttributes.movementSpeed)).setBaseValue(0.1D);  // movementSpeed 1?

        // Register + set attack damage if missing
         //self.getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
        (self.getEntityAttribute(SharedMonsterAttributes.attackDamage)).setBaseValue(0.1D); // attackDamage 10
    }
    
    
//    @Inject(method = "func_70636_d", at = @At("HEAD")
//    		//value = "HEAD", 
//    		//target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity"
//    		//		+ "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;")
//    		,cancellable = true)
//    public void func_70636_d2(CallbackInfo ci) {
//    	ci.cancel();
//    	EntityVampire self = (EntityVampire)(Object)this;
//        List<Entity> list = self.worldObj.getEntitiesWithinAABBExcludingEntity(self, self.boundingBox.expand(10.0D, 20.0D, 10.0D));
//        for (int i1 = 0; i1 < list.size(); i1++) {
//          Entity entity1 = list.get(i1);
//          if (entity1 instanceof EntityLivingBase) {
//            EntityLivingBase entity = (EntityLivingBase)entity1;
//            if (entity1 instanceof EntityPlayer) {
//              EntityPlayer target = (EntityPlayer)entity1;
//              if (PlayerHelper.isInSameRealm(target, (EntityLivingBase)(Object)this))
//                //PlayerHelper.setGrounded(target, 1); 
//              	GeneralUtil.sendDebugMessage(target, "Cancelled Grounded");
//            } 
//          } 
//        } 
//      }
    
    @Shadow protected abstract boolean isFleeing(EntityVampire entity);
    @Shadow public abstract void punchAnimation();
	
    @Inject(method = "func_70636_d", 
    	    at = @At(value = "INVOKE", target = "Lcom/tihyo/legends/entities/mobs/EntitySmartCreature;func_70636_d()V", 
            shift = At.Shift.AFTER), cancellable = true)
    public void change(CallbackInfo ci) {
    	ci.cancel();
    	EntityVampire self = (EntityVampire)(Object)this;
    	//EntitySmartCreature ob = (EntitySmartCreature)(Object)this;
    	//ob.onLivingUpdate();
        //self.onLivingUpdate();
        self.fallDistance = 0.0F;
        regeneration(1.0F, (!self.worldObj.isDaytime() && self.ticksExisted % 10 == 0));
        if (!isFleeing(self))
          wander(1.0D, 25); 
        swim(0.02D, 0.04F);
        EntityLivingBase targ = getTarget();
        if(targ instanceof EntityPlayer) {
        	if(!GeneralUtil.isVampire((EntityPlayer)targ)) {
        		approachTarget(getTarget(), 1.2D, 1.0F, -1.8F);
        	}
        	else {forgetTarget((EntityLivingBase)this, getTarget(), 0.0F);
        		
        	}
        }
        else if (targ != null) {
        	approachTarget(findTarget((EntityLivingBase)this, 25.0D), 1.2D, 1.0F, -1.8F);
        }
        		
		//approachTarget((targ != null && targ instanceof EntityPlayer && GeneralUtil.isVampire((EntityPlayer)targ)) ? getTarget() : findTarget((EntityLivingBase)this, 25.0D), 1.2D, 1.0F, -1.8F);
        forgetTarget((EntityLivingBase)this, getTarget(), 50.0F);
        punchAnimation();
        if (self.ticksExisted < 10)
          setTargetToTrack((EntityLivingBase)this, null); 
        if (self.ticksExisted % 500 == 0)
          self.worldObj.playSoundAtEntity((Entity)self, "horror:hiss", 2.0F, 1.0F); 
        if (this.worldObj.isDaytime() && !self.worldObj.isRemote && !isChild()) {
          float f = getBrightness(1.0F);
          if (f > 0.5F && this.rand.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ))) {
            boolean flag = true;
            ItemStack itemstack = getEquipmentInSlot(4);
            if (itemstack != null) {
              if (itemstack.isItemDamaged()) {
                itemstack.setItemDamage(itemstack.getItemDamage() + this.rand.nextInt(2));
                if (itemstack.getItemDamage() >= itemstack.getMaxDamage()) {
                  renderBrokenItemStack(itemstack);
                  setCurrentItemOrArmor(4, (ItemStack)null);
                } 
              } 
              flag = false;
            } 
            if (flag) {
              setFire(5);
              attackEntityFrom(DamageSource.onFire, 5.0F);
            } 
          } 
        } 
        List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity)this, this.boundingBox.expand(10.0D, 20.0D, 10.0D));
        for (int i1 = 0; i1 < list.size(); i1++) {
          Entity entity1 = list.get(i1);
          if (entity1 instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase)entity1;
            if (entity1 instanceof EntityPlayer) {
              EntityPlayer target = (EntityPlayer)entity1;
              
              if (PlayerHelper.isInSameRealm(target, (EntityLivingBase)this) && !GeneralUtil.isVampire(target)) {
                 PlayerHelper.setGrounded(target, 1); 
              }
            } 
          } 
        } 
      }
    
    
    @Shadow public abstract float getSwing(); 
    @Shadow public abstract void setPunch(float value);
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    public void attackChanges(EntityLivingBase entity, CallbackInfo ci) {
    	ci.cancel();
    	float attackDamage = .5F; //Default is 5
        if (canAttackEntity(entity)) {
          if (getSwing() <= 0.0F)
            setPunch(.2F); 
          entity.attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase)this), entity.isPotionActive(LegendsEffects.werewolfVenom.id) ? (attackDamage / 2.0F) : attackDamage);
          if (this.ticksExisted % 100 == 0)
            LegendsEffects.addBleeding(entity, 20, 1); 
          if (isBurning())
            entity.setFire(5); 
          if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            if (!LegacyHelper.hasLegacy(player) && !PlayerHelper.hasVampireInfection(player) && rand.nextInt(10) < 11)
              PowersHandler.vampireInfection(player, true); 
          } 
        } 
      }
    @Inject(method = "isAlly", at = @At("RETURN"), cancellable = true)
    public void isAllyChanges(EntityLivingBase entity, CallbackInfoReturnable <Boolean>cir) {
    	if(entity instanceof EntityPlayer) {
    		cir.setReturnValue(GeneralUtil.isVampire((EntityPlayer)entity));
    	}
    }
    
    @Inject(method = "func_70097_a", at = @At("HEAD"), cancellable = true)
    public void attackEntityFrom(DamageSource source, float damage, CallbackInfoReturnable <Boolean>cir) {
    	cir.cancel();
        if (source.getEntity() != null && source.getEntity() instanceof EntityLivingBase) {
          if (source.getEntity() instanceof EntityPlayer) {
        	  if(!GeneralUtil.isVampire((EntityPlayer)source.getEntity()))
        		  setAngeredTargetToTrack((EntityLivingBase)this, (EntityLivingBase)source.getEntity());
          } else {
            if (isAlly((EntityLivingBase)source.getEntity()))
              cir.setReturnValue(false);; 
            setAngeredTargetToTrack((EntityLivingBase)this, (EntityLivingBase)source.getEntity());
          } 
        } else {
          findTarget((EntityLivingBase)this, 100.0D);
        } 
        cir.setReturnValue(super.attackEntityFrom(source, damage));
      } 
    
    @Inject(method = "isSuitableTarget", at = @At("HEAD"), cancellable = true)
    public void isSuitableTarget(EntityLivingBase searcher, EntityLivingBase entity, boolean isLooking, CallbackInfoReturnable <Boolean>cir) {
    		if(entity instanceof EntityPlayer) {
    			cir.cancel();
    			EntityPlayer player = (EntityPlayer)entity;
        		if(Loader.isModLoaded("witchery")) {
        			if(PotionEnslaved.isMobEnslavedBy((EntityLiving) searcher, player)) {
        				cir.setReturnValue(false);
        			}
        			
        		}
        		if(GeneralUtil.isVampire(player))
        			cir.setReturnValue(false);
    			cir.setReturnValue(!player.capabilities.isCreativeMode);	
    		}
    }
}

