package moe.plushie.armourers_workshop.init.client;

import moe.plushie.armourers_workshop.api.client.model.IModelHolder;
import moe.plushie.armourers_workshop.api.math.IPoseStack;
import moe.plushie.armourers_workshop.api.skin.ISkinPartType;
import moe.plushie.armourers_workshop.core.armature.JointTransformModifier;
import moe.plushie.armourers_workshop.core.armature.thirdparty.EpicFlightContext;
import moe.plushie.armourers_workshop.core.armature.thirdparty.EpicFlightTransformProvider;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderContext;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderData;
import moe.plushie.armourers_workshop.core.client.skinrender.SkinRendererManager;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTypes;
import moe.plushie.armourers_workshop.init.ModConfig;
import moe.plushie.armourers_workshop.utils.ModelHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Collections;

@Environment(value = EnvType.CLIENT)
public class EpicFlightWardrobeHandler {

    private static final EpicFlightContext context = new EpicFlightContext();

    public static void onSetup() {
        //
        ModConfig.Client.enablePartSubdivide = true;
    }

    public static void onRenderLivingPre(LivingEntity entity, float partialTicks, int packedLight, IPoseStack poseStack, MultiBufferSource buffers, LivingEntityRenderer<?, ?> entityRenderer, boolean isFirstPersonRenderer, EpicFlightTransformProvider transformProvider) {
        IModelHolder<?> model = ModelHolder.ofNullable(entityRenderer.getModel());
        SkinRenderData renderData = SkinRenderData.of(entity);
        if (renderData == null) {
            return;
        }
        Collection<ISkinPartType> overrideParts = null;
        if (isFirstPersonRenderer) {
            overrideParts = Collections.singleton(SkinPartTypes.BIPPED_HEAD);
        }

        model.setExtraData(EpicFlightTransformProvider.KEY, transformProvider);

        context.overrideParts = overrideParts;
        context.overridePostStack = poseStack.copy();
        context.overrideTransformModifier = model.getExtraData(JointTransformModifier.EPICFIGHT);;

        renderData.epicFlightContext = context;

        SkinRendererManager.getInstance().willRender(entity, entityRenderer.getModel(), entityRenderer, renderData, () -> SkinRenderContext.alloc(renderData, packedLight, partialTicks, poseStack, buffers));
    }

    public static void onRenderLivingPost(LivingEntity entity, float partialTicks, int packedLight, IPoseStack poseStack, MultiBufferSource buffers, LivingEntityRenderer<?, ?> entityRenderer) {
        IModelHolder<?> model = ModelHolder.ofNullable(entityRenderer.getModel());
        SkinRenderData renderData = SkinRenderData.of(entity);
        if (renderData == null) {
            return;
        }
        model.setExtraData(EpicFlightTransformProvider.KEY, null);

        context.overrideParts = null;
        context.overridePostStack = null;
        context.overrideTransformModifier = null;

        renderData.epicFlightContext = null;

        SkinRendererManager.getInstance().didRender(entity, entityRenderer.getModel(), entityRenderer, renderData, () -> SkinRenderContext.alloc(renderData, packedLight, partialTicks, poseStack, buffers));
    }
}
