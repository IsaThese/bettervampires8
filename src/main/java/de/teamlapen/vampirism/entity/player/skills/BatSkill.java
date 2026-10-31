package de.teamlapen.vampirism.entity.player.skills;

import java.util.UUID;

import com.mrsand.StatBoostChanges.util.LegendsUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.PlayerModifiers;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentTranslation;

public class BatSkill extends DefaultSkill implements ILastingSkill {
	public final static float BAT_WIDTH = 0.5F;
	public final static float BAT_HEIGHT = 0.8F;
	public final static float BAT_EYE_HEIGHT = 0.85F * BAT_HEIGHT;
	public final static float PLAYER_WIDTH = 0.6F;
	public final static float PLAYER_HEIGHT = 1.8F;

	public final UUID speedModifierUUID = UUID.fromString("eb7a2e48-ce60-4629-b5f5-7a196d1035af");

	public final UUID healthModifierUUID = UUID.fromString("4392fccb-4bfd-4290-b2e6-5cc91429053c");

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
		return !vampire.isSkillActive(Skills.vampireRage);
	}

	/**
	 * Show while active so bat can be cancelled.
	 * Block starting bat in sunlight; vampire forest is exempt via {@link VampirePlayer#isInSunlight()}.
	 */
	@Override
	public int canUse(VampirePlayer vampire, EntityPlayer player) {
		if (getMinLevel() == -1)
			return -1;
		if (vampire.getLevel() < getMinLevel())
			return 0;
		if (!canBeUsedBy(vampire, player))
			return -2;
		if (vampire.isSkillActive(this) || LegendsUtil.isBat(player))
			return 1;
		if (player == null || player.worldObj == null)
			return -2;
		if (vampire.isInSunlight())
			return -2;
		return 1;
	}

	@Override
	public int getCooldown() {
		return 2;
	}

	@Override
	public int getDuration(int level) {
		return 100000;
	}

	@Override
	public int getMinLevel() {
		return 4;
	}

	@Override
	public int getMinU() {
		return 64;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.bat_skill";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		if (Loader.isModLoaded("legends")) {
			// Already bat from SummonBat (skill not active): one press should exit, not arm the skill
			if (LegendsUtil.isBat(player)) {
				LegendsUtil.morphBat(player, false);
				return false;
			}
			return LegendsUtil.morphBat(player, true);
		}
		setModifier(player, true);
		double reduc = player.getHealth() - player.getMaxHealth();
		if (reduc < 0) {
			reduc = 0;
		}
		player.setHealth((float) (player.getHealth() - reduc));
		vampire.getExtraDataTag().setDouble("bat_skill_health", reduc);
		return true;
	}

	@Override
	public void onDeactivated(VampirePlayer vampire, EntityPlayer player) {
		if (Loader.isModLoaded("legends")) {
			LegendsUtil.morphBat(player, false);
			return;
		}
		setModifier(player, false);
		if (player.getHealth() > 0) {
			player.setHealth((float) (vampire.getExtraDataTag().getDouble("bat_skill_health") + player.getHealth()));
			if (player.onGround) {
				player.addPotionEffect(new PotionEffect(Potion.resistance.id, 20, 100));
			}
			player.addPotionEffect(new PotionEffect(Potion.resistance.id, 60, 100));

		}

	}

	@Override
	public void onReActivated(VampirePlayer vampire, EntityPlayer player) {
		if (Loader.isModLoaded("legends")) {
			if (!LegendsUtil.isBat(player)) {
				LegendsUtil.morphBat(player, true);
			}
			return;
		}
		setModifier(player, true);
	}

	@Override
	public boolean onUpdate(VampirePlayer vampire, EntityPlayer player) {
		if (vampire.gettingSundamage() && !player.worldObj.isRemote) {
			player.addChatMessage(new ChatComponentTranslation("text.vampirism.cant_fly_day"));
			return true;
		}
		// Wolf/beast/mist took over Legends form but BatSkill stayed active → BreakSpeed still canceled.
		// Use explicit other-forms only (not !isBat) to avoid ending skill during morph sync lag.
		if (Loader.isModLoaded("legends") && !player.worldObj.isRemote) {
			if (LegendsUtil.isWolf(player) || LegendsUtil.isVampireBeast(player) || LegendsUtil.isMist(player)) {
				return true;
			}
		}
		return false;
	}

	private void setModifier(EntityPlayer player, boolean enabled) {
		if (enabled) {
			IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.maxHealth);
			if (health.getModifier(healthModifierUUID) == null) {
				health.applyModifier(new AttributeModifier(healthModifierUUID, "Bat Health Reduction", -0.9, 2).setSaved(false));
			}


			player.capabilities.allowFlying = true;
			player.capabilities.isFlying = true;
			player.sendPlayerAbilities();
		} else {
			IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.maxHealth);
			PlayerModifiers.rmMod(health, healthModifierUUID);

			if (!player.capabilities.isCreativeMode) {
				player.capabilities.allowFlying = false;
			}
			player.capabilities.isFlying = false;
			player.sendPlayerAbilities();
		}

	}

}
