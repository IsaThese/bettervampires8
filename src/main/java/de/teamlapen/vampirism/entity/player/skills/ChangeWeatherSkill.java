package de.teamlapen.vampirism.entity.player.skills;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.storage.WorldInfo;

public class ChangeWeatherSkill extends DefaultSkill {

    @Override
	public int getCooldown() {
        int multiplierCooldown = 0;
        return BALANCE.VP_SKILLS.WEATHER_COOLDOWN * multiplierCooldown;
	}

	@Override
	public int getMinLevel() {
		return BALANCE.VP_SKILLS.WEATHER_MIN_LEVEL;
	}

	@Override
	public boolean canBeUsedBy(VampirePlayer vampire, EntityPlayer player) {
        return vampire.isVampireLord() && !vampire.isInSunlight();
    }

	@Override
	public int getMinU() {
		return 48;
	}

	@Override
	public int getMinV() {
		return 0;
	}

	@Override
	public String getUnlocalizedName() {
		return "skill.vampirism.change_weather";
	}

	@Override
	public boolean onActivated(VampirePlayer vampire, EntityPlayer player) {
		WorldInfo info = player.worldObj.getWorldInfo();
		if(Loader.isModLoaded("witchery")){
			if(WitcheryUtil.hasBloodPower(player,0, .5F)){
				//This is intentional
				WitcheryUtil.decreaseBlood(player, 0, .05F, true);
			}
			else{
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("You don't have enough power, need more than half")
						.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return false;
			}
		}
		info.setRaining(!info.isRaining());
		return true;
	}

}
