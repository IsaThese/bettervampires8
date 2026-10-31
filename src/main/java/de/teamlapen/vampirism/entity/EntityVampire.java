package de.teamlapen.vampirism.entity;

import de.teamlapen.vampirism.ModItems;
import de.teamlapen.vampirism.entity.ai.VampireAIDefendLordPlayer;
import de.teamlapen.vampirism.entity.ai.VampireAIFleeGarlic;
import de.teamlapen.vampirism.entity.ai.VampireAIFleeSun;
import de.teamlapen.vampirism.entity.ai.VampireAIMoveToBiteable;
import de.teamlapen.vampirism.entity.ai.VampireAIWatchLordPlayer;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIRestrictSun;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityVampire extends EntityDefaultVampire {
	private boolean inCastle = false;
	private int bloodtimer = 100;
	public EntityVampire(World par1World) {
		super(par1World);

		this.hasArms = true;
		
		this.targetTasks.addTask(2, new VampireAIDefendLordPlayer(this));
		
		this.tasks.addTask(3, new EntityAIAvoidEntity(this, EntityHunterBase.class, BALANCE.MOBPROP.VAMPIRE_DISTANCE_HUNTER, 1.0, 1.2));
		this.tasks.addTask(3, new EntityAIRestrictSun(this));
		this.tasks.addTask(4, new VampireAIFleeSun(this, 0.9F));
		this.tasks.addTask(4, new VampireAIFleeGarlic(this, 0.9F));
		
		this.tasks.addTask(5, new net.minecraft.entity.ai.EntityAIAttackOnCollide(this, net.minecraft.entity.EntityLivingBase.class, 1.0D, false));
		this.tasks.addTask(6, new VampireAIWatchLordPlayer(this, 12.0F));
		
		this.tasks.addTask(9, new VampireAIMoveToBiteable(this));
		this.tasks.addTask(10, new EntityAIMoveThroughVillage(this, 0.6, true));
		this.tasks.addTask(11, new EntityAIWander(this, 0.7));
		this.tasks.addTask(12, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(13, new EntityAIWatchClosest(this, EntityHunterBase.class, 8.0F));
		this.tasks.addTask(14, new EntityAILookIdle(this));

		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true, false, new IEntitySelector() {
			@Override
			public boolean isEntityApplicable(Entity entity) {
				if (entity instanceof EntityPlayer) {
					return VampirePlayer.get((EntityPlayer) entity).getLevel() <= BALANCE.VAMPIRE_FRIENDLY_LEVEL;
				}
				return false;
			}
		}));
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MAX_HEALTH);
		this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(BALANCE.MOBPROP.VAMPIRE_ATTACK_DAMAGE);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(BALANCE.MOBPROP.VAMPIRE_MOVEMENT_SPEED);
	}

	@Override
	public void onDeath(DamageSource s) {
		super.onDeath(s);
		if (s.getEntity() != null && s.getEntity() instanceof EntityPlayer) {
			this.dropItem(ModItems.vampireFang, 1);
		}
	}

	public void makeCastleVampire() {
		inCastle = true;
	}

	public boolean isInCastle() {
		return inCastle;
	}

	@Override
	public boolean wantsBlood() {
		return bloodtimer == 0;
	}

	@Override
	public void addBlood(int amt) {
		super.addBlood(amt);
		bloodtimer += amt * 40 + rand.nextInt(10) * 20;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (bloodtimer > 0) bloodtimer--;
	}
}
