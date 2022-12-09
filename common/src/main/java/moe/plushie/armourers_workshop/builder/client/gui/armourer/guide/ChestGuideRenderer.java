package moe.plushie.armourers_workshop.builder.client.gui.armourer.guide;

import moe.plushie.armourers_workshop.api.client.guide.IGuideDataProvider;
import moe.plushie.armourers_workshop.api.math.IPoseStack;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderType;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTypes;
import moe.plushie.armourers_workshop.utils.ModelPartBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;

@Environment(value = EnvType.CLIENT)
public class ChestGuideRenderer extends AbstractGuideRenderer {

    protected final ModelPart body;
    protected final ModelPart leftArm;
    protected final ModelPart rightArm;
    protected final ModelPart jacket;
    protected final ModelPart leftSleeve;
    protected final ModelPart rightSleeve;

    public ChestGuideRenderer() {
        body = ModelPartBuilder.player().uv(16, 16).cube(-4, -12, -2, 8, 12, 4).build();
        leftArm = ModelPartBuilder.player().uv(32, 48).cube(-1, -12, -2, 4, 12, 4).build();
        rightArm = ModelPartBuilder.player().uv(40, 16).cube(-3, -12, -2, 4, 12, 4).build();
        leftSleeve = ModelPartBuilder.player().uv(48, 48).cube(-1, -12, -2, 4, 12, 4, 0.25f).build();
        rightSleeve = ModelPartBuilder.player().uv(40, 32).cube(-3, -12, -2, 4, 12, 4, 0.25f).build();
        jacket = ModelPartBuilder.player().uv(16, 32).cube(-4, -12, -2, 8, 12, 4, 0.25f).build();
    }

    @Override
    public void init(GuideRendererManager rendererManager) {
        rendererManager.register(SkinPartTypes.BIPED_CHEST, this::render);
        rendererManager.register(SkinPartTypes.BIPED_LEFT_ARM, this::renderLeftArm);
        rendererManager.register(SkinPartTypes.BIPED_RIGHT_ARM, this::renderRightArm);
    }

    public void render(IPoseStack matrixStack, IGuideDataProvider provider, int light, int overlay, MultiBufferSource buffers) {
        body.render(matrixStack.cast(), buffers.getBuffer(SkinRenderType.PLAYER_CUTOUT), light, overlay);
        if (provider.shouldRenderOverlay()) {
            jacket.render(matrixStack.cast(), buffers.getBuffer(SkinRenderType.PLAYER_CUTOUT_NO_CULL), light, overlay);
        }
    }

    public void renderLeftArm(IPoseStack matrixStack, IGuideDataProvider provider, int light, int overlay, MultiBufferSource buffers) {
        leftArm.render(matrixStack.cast(), buffers.getBuffer(SkinRenderType.PLAYER_CUTOUT), light, overlay);
        if (provider.shouldRenderOverlay()) {
            leftSleeve.render(matrixStack.cast(), buffers.getBuffer(SkinRenderType.PLAYER_CUTOUT_NO_CULL), light, overlay);
        }
    }

    public void renderRightArm(IPoseStack matrixStack, IGuideDataProvider provider, int light, int overlay, MultiBufferSource buffers) {
        rightArm.render(matrixStack.cast(), buffers.getBuffer(SkinRenderType.PLAYER_CUTOUT), light, overlay);
        if (provider.shouldRenderOverlay()) {
            rightSleeve.render(matrixStack.cast(), buffers.getBuffer(SkinRenderType.PLAYER_CUTOUT_NO_CULL), light, overlay);
        }
    }
}
