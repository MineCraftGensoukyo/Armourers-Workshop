package moe.plushie.armourers_workshop.init.platform.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class TransformationProviderImpl {

    public static BakedModel handleTransforms(PoseStack matrixStack, BakedModel bakedModel, ItemTransforms.TransformType transformType, boolean leftHandHackery) {
        bakedModel.getTransforms().getTransform(transformType).apply(leftHandHackery, matrixStack);
        return bakedModel;
    }

}
