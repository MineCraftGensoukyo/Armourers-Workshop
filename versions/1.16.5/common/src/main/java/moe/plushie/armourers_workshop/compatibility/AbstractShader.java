package moe.plushie.armourers_workshop.compatibility;

import com.mojang.blaze3d.vertex.VertexFormat;
import moe.plushie.armourers_workshop.api.math.IPoseStack;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderObject;
import moe.plushie.armourers_workshop.core.client.other.VertexIndexBuffer;
import moe.plushie.armourers_workshop.core.client.shader.Shader;
import moe.plushie.armourers_workshop.core.client.shader.ShaderVertexGroup;
import moe.plushie.armourers_workshop.core.client.shader.ShaderVertexObject;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;

@Environment(value = EnvType.CLIENT)
public class AbstractShader extends Shader {

    private int lastMaxVertexCount = 0;

    @Override
    public void begin() {
        super.begin();
        RenderSystem.enableRescaleNormal();
    }

    @Override
    public void end() {
        RenderSystem.disableRescaleNormal();
        SkinRenderObject.unbind();
        super.end();
    }

    @Override
    public void apply(ShaderVertexGroup group, Runnable action) {
        lastMaxVertexCount = group.maxVertexCount;
        group.getType().setupRenderState();
        action.run();
        group.getType().clearRenderState();
    }

    @Override
    public void render(ShaderVertexObject object) {
        VertexFormat vertexFormat = object.getFormat();
        AbstractLightBufferObject lightBuffer = null;

        if (!object.isGrowing()) {
            lightBuffer = AbstractLightBufferObject.getLightBuffer(object.getLightmap());
            lightBuffer.ensureCapacity(lastMaxVertexCount);
            lightBuffer.bind();
            lightBuffer.getFormat().setupBufferState(0L);
        }

        object.getVertexBuffer().bind();
        vertexFormat.setupBufferState(object.getVertexOffset());
        setupPolygonState(object);

        IPoseStack poseStack = object.getPoseStack();
        IPoseStack modelViewStack = RenderSystem.getExtendedModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.multiply(poseStack.lastPose());
        RenderSystem.drawArrays(object.getType().mode(), 0, object.getVertexCount());
        modelViewStack.popPose();

        cleanPolygonState(object);
        vertexFormat.clearBufferState();

        if (lightBuffer != null) {
            lightBuffer.getFormat().clearBufferState();
        }
    }

    @Override
    protected void draw(RenderType renderType, VertexIndexBuffer.IndexType indexType, int count, int indices) {
        throw new AssertionError();
    }
}
