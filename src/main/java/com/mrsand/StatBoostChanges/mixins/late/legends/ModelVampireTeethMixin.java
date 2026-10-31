package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tihyo.horror.client.models.legacy.ModelVampireTeeth;

import net.minecraft.client.model.ModelRenderer;

@Mixin(value = ModelVampireTeeth.class, remap = false)
public abstract class ModelVampireTeethMixin {
	@Shadow public abstract void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z);
	
	@Inject(method = "<init>", at = @At("RETURN"))
    private void adjustTeethHeight(float expand, CallbackInfo ci) {
        ModelVampireTeeth model = (ModelVampireTeeth) (Object) this;
        
        //func_78790_a add texture box
        
        // Define how much you want to shift the teeth down (e.g., 1.0F is one block unit)
        float shiftDown = 1.0F; 

        // Shift the Y-coordinate (index 1 of the rotation point array is Y)
        model.Teeth1.rotationPointY += shiftDown;
        model.Teeth2.rotationPointY += shiftDown;
        model.Teeth3.rotationPointY += shiftDown;
        model.Teeth4.rotationPointY += shiftDown;
        model.Teeth5.rotationPointY += shiftDown;
        model.Teeth6.rotationPointY += shiftDown;
        
//        model.Teeth3.isHidden = true;
//        model.Teeth4.isHidden = true;
        model.Teeth5.isHidden = true;
        model.Teeth6.isHidden = true;
//        model.Teeth5.addBox(-0.8F, -1.9F, -3.9F, 1, 1, 1, 0.0F);
//        setRotateAngle(model.Teeth5, 0.0F, 0.0F, 0.0F); // was 0.2617994F (outward tilt)
//
//        model.Teeth6 = new ModelRenderer((ModelBase)model, 12, 50);
//        model.Teeth6.setRotationPoint(0.0F, 0.0F, 0.0F); //93
//        model.Teeth6.addBox(-0.2F, -1.9F, -3.9F, 1, 1, 1, 0.0F);
//        
//        setRotateAngle(model.Teeth6, 0.0F, 0.0F, 0.0F); 
//        setRotateAngle(model.Teeth5, 0.0F, 0.0F, 0.0F); 
        
    }

}
