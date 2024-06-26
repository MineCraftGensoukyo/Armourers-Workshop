package moe.plushie.armourers_workshop.compatibility;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.api.math.IMatrix3f;
import moe.plushie.armourers_workshop.api.math.IMatrix4f;
import moe.plushie.armourers_workshop.api.math.IPoseStack;
import moe.plushie.armourers_workshop.api.math.IQuaternionf;
import moe.plushie.armourers_workshop.compatibility.ext.AbstractMatrixUtilsExt_V1618;
import moe.plushie.armourers_workshop.utils.MatrixUtils;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import moe.plushie.armourers_workshop.utils.math.Matrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value = EnvType.CLIENT)
public class AbstractMatrixUtils extends AbstractMatrixUtilsExt_V1618 {

    public static IPoseStack modelViewStack() {
        IPoseStack emptyStack = MatrixUtils.stack();
        return new IPoseStack() {

            @Override
            public void pushPose() {
                RenderSystem.pushMatrix();
            }

            @Override
            public void popPose() {
                RenderSystem.popMatrix();
            }

            @Override
            public void translate(float x, float y, float z) {
                RenderSystem.translated(x, y, z);
            }

            @Override
            public void scale(float x, float y, float z) {
                RenderSystem.scalef(x, y, z);
            }

            @Override
            public void multiply(IMatrix4f matrix) {
                RenderSystem.mulMatrix(matrix);
            }

            @Override
            public void multiply(IPoseStack poseStack) {
                // no support
            }

            @Override
            public void rotate(IQuaternionf quaternion) {
                RenderSystem.mulMatrix(new Matrix4f(quaternion));
            }

            @Override
            public IMatrix4f lastPose() {
                return emptyStack.lastPose();
            }

            @Override
            public IMatrix3f lastNormal() {
                return emptyStack.lastNormal();
            }

            @Override
            public IPoseStack copy() {
                return emptyStack;
            }

            @Override
            public PoseStack cast() {
                return emptyStack.cast();
            }
        };
    }
}
