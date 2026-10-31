package com.mrsand.StatBoostChanges.util;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public class VisualUtil {

	private static final ResourceLocation OTHER_SKIN = new ResourceLocation("bettervampires", "textures/otherskin.png");

	public static void changeForm(EntityPlayer player, Boolean changeToOtherForm) {
		if (!(player instanceof AbstractClientPlayer)) return;
		AbstractClientPlayer client = (AbstractClientPlayer)player;
		VExtendedPlayer props = VExtendedPlayer.get(player);
		if (props == null) return;
		if(props.getBoolean(PlayerKeys.IS_STARVING) || props.getBoolean(PlayerKeys.HYPNOTIZING) || LegendsUtil.bloodVision(player)) {
			changeToOtherForm = true;
		}

		boolean wasOther = props.getBoolean(PlayerKeys.OTHERFORM);

		if (changeToOtherForm) {
			ResourceLocation currentSkin = client.getLocationSkin();
			if (currentSkin != null && !currentSkin.equals(OTHER_SKIN)) {
				props.setOriginalSkin(currentSkin);
			}
			client.func_152121_a(MinecraftProfileTexture.Type.SKIN, OTHER_SKIN);
			props.setBoolean(PlayerKeys.OTHERFORM, true);
		} else {
			ResourceLocation original = props.getOriginalSkin();
			if (original != null) {
				client.func_152121_a(MinecraftProfileTexture.Type.SKIN, original);
				props.setOriginalSkin(null);
			}

			props.setBoolean(PlayerKeys.OTHERFORM, false);
		}
		// Level-10 tick calls this every frame — only upload when OTHERFORM actually flips
		if (wasOther != props.getBoolean(PlayerKeys.OTHERFORM)) {
			props.sync();
		}
	}
	public static void updateVillageSidebar(EntityPlayer player, int distance) {
	    Scoreboard scoreboard = player.getWorldScoreboard();
	    
	    // 1. Find or create the "Village" objective
	    ScoreObjective objective = scoreboard.getObjective("VillageTracker");
	    if (objective == null) {
	        objective = scoreboard.addScoreObjective("VillageTracker", IScoreObjectiveCriteria.field_96641_b);
	        objective.setDisplayName(EnumChatFormatting.GOLD + "NEAREST VILLAGE");
	        
	        // 2. Set the display slot to the Sidebar (id 1)
	        scoreboard.func_96530_a(1, objective); 
	    }

	    // 3. Update the distance value for the player
	    // This looks like: [PlayerName] ... [Distance]
	    Score score = scoreboard.func_96529_a(player.getCommandSenderName(), objective);
	    score.setScorePoints(distance); 
	}
}
