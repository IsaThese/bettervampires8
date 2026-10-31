//package com.mrsand.StatBoostChanges.common.handlers;
//
//import cpw.mods.fml.common.eventhandler.EventPriority;
//import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//import cpw.mods.fml.common.gameevent.TickEvent;
//import de.teamlapen.vampirism.entity.player.VampirePlayer;
//import de.teamlapen.vampirism.entity.player.skills.Skills;
//import net.minecraft.client.Minecraft;
//import net.minecraft.entity.player.EntityPlayer;
//
//public class VampirismCameraHander {
//	
//	private final Minecraft mc = Minecraft.getMinecraft();
//    private float customAppliedOffset;
//    private boolean customShifted;
//    
//    
//    @SubscribeEvent(priority = EventPriority.LOWEST) // Executes AFTER Vampirism's default priority
//    public void onRenderTickStart(TickEvent.RenderTickEvent event) {
//        if (this.mc.theWorld != null && event.phase == TickEvent.Phase.START) {
//            EntityPlayer player = this.mc.thePlayer;
//
//            // Ensure we only manipulate first-person perspective (0)
//            if (this.mc.gameSettings.thirdPersonView == 0) {
//                
//                // IF TARGETING BAT MODE: Cancel out their downward shift and push it HIGHER
//                if (VampirePlayer.get(player).isSkillActive(Skills.batMode)) {
//                    
//                    // 1. Calculate exactly what Vampirism subtracted from the coordinates
//                    float vampSubtracted = player.yOffset - 0.68F + player.getDefaultEyeHeight();
//                    
//                    // 2. Define your desired layout (e.g., 1.5 blocks higher than normal player height)
//                    float myDesiredHeightAboveNormal = 3F;
//                    
//                    // 3. Total offset = add back what they took + add your extra height
//                    this.customAppliedOffset = vampSubtracted + myDesiredHeightAboveNormal;
//                    
//                    // Shift the coordinates UP
//                    player.lastTickPosY += this.customAppliedOffset;
//                    player.prevPosY += this.customAppliedOffset;
//                    player.posY += this.customAppliedOffset;
//                    this.customShifted = true;
//                }
//            }
//        }
//    }
//
//    @SubscribeEvent(priority = EventPriority.HIGHEST) // Executes BEFORE Vampirism cleans up
//    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
//        if (event.phase == TickEvent.Phase.END && this.customShifted) {
//            this.customShifted = false;
//            EntityPlayer player = this.mc.thePlayer;
//            
//            // Clean up our custom applied height adjustments before Vampirism does its own cleanup
//            player.lastTickPosY -= this.customAppliedOffset;
//            player.prevPosY -= this.customAppliedOffset;
//            player.posY -= this.customAppliedOffset;
//        }
//    }
//}
