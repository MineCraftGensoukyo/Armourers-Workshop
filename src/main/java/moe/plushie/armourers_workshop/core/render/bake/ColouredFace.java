package moe.plushie.armourers_workshop.core.render.bake;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import moe.plushie.armourers_workshop.core.api.ISkinCube;
import moe.plushie.armourers_workshop.core.api.ISkinPaintType;
import moe.plushie.armourers_workshop.core.api.ISkinPartType;
import moe.plushie.armourers_workshop.core.render.SkinModelRenderer;
import moe.plushie.armourers_workshop.core.skin.data.SkinPalette;
import moe.plushie.armourers_workshop.core.utils.ColorDescriptor;
import moe.plushie.armourers_workshop.core.utils.PaintColor;
import moe.plushie.armourers_workshop.core.skin.painting.SkinPaintTypes;
import moe.plushie.armourers_workshop.core.AWCore;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColouredFace {

    private static final PaintColor RAINBOW_TARGET = PaintColor.of(0xff7f7f7f, SkinPaintTypes.RAINBOW);

    public final int x;
    public final int y;
    public final int z;

    public final int alpha;

    private final Direction direction;
    private final PaintColor color;
    private final ISkinCube cube;

    public ColouredFace(int x, int y, int z, PaintColor color, int alpha, Direction direction, ISkinCube cube) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.cube = cube;
        this.color = color;
        this.direction = direction;
        this.alpha = alpha;
    }

    public PaintColor dye(PaintColor source, PaintColor destination, PaintColor average) {
        if (destination.getPaintType() == SkinPaintTypes.NONE) {
            return PaintColor.CLEAR;
        }
        if (average == null) {
            return source;
        }
        int src = (source.getRed() + source.getGreen() + source.getBlue()) / 3;
        int avg = (average.getRed() + average.getGreen() + average.getBlue()) / 3;
        int r = MathHelper.clamp(destination.getRed() + src - avg, 0, 255);
        int g = MathHelper.clamp(destination.getGreen() + src - avg, 0, 255);
        int b = MathHelper.clamp(destination.getBlue() + src - avg, 0, 255);
        return PaintColor.of(0xff000000 | r << 16 | g << 8 | b, destination.getPaintType());
    }

    public PaintColor resolve(PaintColor paintColor, SkinPalette palette, ColorDescriptor descriptor, ISkinPartType partType, int count) {
        ISkinPaintType paintType = paintColor.getPaintType();
        if (paintType == SkinPaintTypes.NONE) {
            return PaintColor.CLEAR;
        }
        if (paintType == SkinPaintTypes.RAINBOW) {
            return dye(paintColor, RAINBOW_TARGET, descriptor.getAverageColor(paintType));
        }
        if (paintType == SkinPaintTypes.TEXTURE) {
            BakedEntityTexture texture = AWCore.bakery.getEntityTexture(palette.getTexture());
            if (texture != null) {
                PaintColor paintColor1 = texture.getColor(x, y, z, direction, partType);
                if (paintColor1 != null) {
                    return paintColor1;
                }
            }
            return paintColor;
        }
        if (paintType.getDyeType() != null && count < 2) {
            PaintColor paintColor1 = palette.getResolvedColor(paintType);
            if (paintColor1 == null) {
                return paintColor;
            }
            paintColor = dye(paintColor, paintColor1, descriptor.getAverageColor(paintType));
            return resolve(paintColor, palette, descriptor, partType, count + 1);
        }
        return paintColor;
    }

    public void render(BakedSkinPart part, SkinPalette palette, MatrixStack matrixStack, IVertexBuilder builder) {
        PaintColor resolvedColor = resolve(color, palette, part.getColorInfo(), part.getType(), 0);
        if (resolvedColor.getPaintType() == SkinPaintTypes.NONE) {
            return;
        }
        SkinModelRenderer.renderFace(x, y, z, resolvedColor, alpha, direction, matrixStack, builder);
    }

    public ISkinCube getCube() {
        return cube;
    }

    public PaintColor getColor() {
        return color;
    }

    public Direction getDirection() {
        return direction;
    }

    public ISkinPaintType getPaintType() {
        return color.getPaintType();
    }
}