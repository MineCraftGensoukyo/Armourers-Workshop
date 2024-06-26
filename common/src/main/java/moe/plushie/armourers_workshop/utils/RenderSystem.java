package moe.plushie.armourers_workshop.utils;

import com.apple.library.coregraphics.CGRect;
import com.apple.library.uikit.UIColor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import moe.plushie.armourers_workshop.api.math.*;
import moe.plushie.armourers_workshop.compatibility.AbstractRenderSystem;
import moe.plushie.armourers_workshop.compatibility.AbstractShaderTesselator;
import moe.plushie.armourers_workshop.core.armature.ModelBinder;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderType;
import moe.plushie.armourers_workshop.core.client.other.SkinVertexBufferBuilder;
import moe.plushie.armourers_workshop.core.texture.PlayerTexture;
import moe.plushie.armourers_workshop.core.texture.PlayerTextureDescriptor;
import moe.plushie.armourers_workshop.core.texture.PlayerTextureLoader;
import moe.plushie.armourers_workshop.init.ModDebugger;
import moe.plushie.armourers_workshop.utils.math.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

@Environment(value = EnvType.CLIENT)
public final class RenderSystem extends AbstractRenderSystem {

    private static final IPoseStack extendedModelViewStack = MatrixUtils.modelViewStack();

    private static final Storage<IMatrix3f> extendedNormalMatrix = new Storage<>(Matrix3f.createScaleMatrix(1, 1, 1));
    private static final Storage<IMatrix4f> extendedTextureMatrix = new Storage<>(Matrix4f.createScaleMatrix(1, 1, 1));
    private static final Storage<IMatrix4f> extendedLightmapTextureMatrix = new Storage<>(Matrix4f.createScaleMatrix(1, 1, 1));
    private static final Storage<IMatrix4f> extendedModelViewMatrix = new Storage<>(Matrix4f.createScaleMatrix(1, 1, 1));

    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(3);

    private static final byte[][][] FACE_MARK_TEXTURES = {
            // 0, 1(w), 2(h), 3(d)
            {{1, 3}, {1, 0}, {0, 0}, {0, 3}},
            {{1, 3}, {1, 0}, {0, 0}, {0, 3}},

            {{1, 2}, {1, 0}, {0, 0}, {0, 2}},
            {{1, 2}, {1, 0}, {0, 0}, {0, 2}},

            {{3, 2}, {3, 0}, {0, 0}, {0, 2}},
            {{3, 2}, {3, 0}, {0, 0}, {0, 2}},
    };

    private static final byte[][][] FACE_MARK_VERTEXES = new byte[][][]{
            {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, -1, 0}}, // -y
            {{1, 1, 1}, {1, 1, 0}, {0, 1, 0}, {0, 1, 1}, {0, 1, 0}},  // +y
            {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}, {0, 0, -1}}, // -z
            {{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}, {0, 0, 1}},  // +z
            {{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}, {-1, 0, 0}}, // -x
            {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 0}},  // +x
    };

    private static final LinkedList<CGRect> clipBounds = new LinkedList<>();


    public static void call(Runnable task) {
        if (isOnRenderThread()) {
            task.run();
        } else {
            recordRenderCall(task::run);
        }
    }


    public static void blit(IPoseStack poseStack, int x, int y, int u, int v, int width, int height) {
        Screen.blit(poseStack.cast(), x, y, 0, u, v, width, height, 256, 256);
    }

    public static void blit(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, ResourceLocation texture) {
        setShaderTexture(0, texture);
        Screen.blit(poseStack.cast(), x, y, 0, u, v, width, height, 256, 256);
    }

    public static void blit(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int texWidth, int texHeight, ResourceLocation texture) {
        setShaderTexture(0, texture);
        Screen.blit(poseStack.cast(), x, y, 0, u, v, width, height, texWidth, texHeight);
    }

    public static void tile(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int texWidth, int texHeight, int r0, int r1, int r2, int r3) {
        drawContinuousTexturedBox(poseStack, x, y, u, v, width, height, texWidth, texHeight, r0, r1, r2, r3, 0);
    }

    public static void tile(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int texWidth, int texHeight, int r0, int r1, int r2, int r3, ResourceLocation texture) {
        setShaderTexture(0, texture);
        drawContinuousTexturedBox(poseStack, x, y, u, v, width, height, texWidth, texHeight, r0, r1, r2, r3, 0);
    }

    public static void resize(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int targetWidth, int targetHeight) {
        resize(poseStack, x, y, u, v, width, height, targetWidth, targetHeight, 256, 256);
    }

    public static void resize(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int sourceWidth, int sourceHeight, int texWidth, int texHeight) {
        float f = 1.0f / texWidth;
        float f1 = 1.0f / texHeight;

        IMatrix4f mat = poseStack.lastPose();
        AbstractShaderTesselator tessellator = AbstractShaderTesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.begin(SkinRenderType.GUI_IMAGE);
        bufferbuilder.vertex(mat, x, y + height, 0).uv(u * f, (v + sourceHeight) * f1).endVertex();
        bufferbuilder.vertex(mat, x + width, y + height, 0).uv((u + sourceWidth) * f, (v + sourceHeight) * f1).endVertex();
        bufferbuilder.vertex(mat, x + width, y, 0).uv((u + sourceWidth) * f, v * f1).endVertex();
        bufferbuilder.vertex(mat, x, y, 0).uv(u * f, v * f1).endVertex();
        tessellator.end();
    }


    public static void resize(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int sourceWidth, int sourceHeight, ResourceLocation texture) {
        setShaderTexture(0, texture);
        resize(poseStack, x, y, u, v, width, height, sourceWidth, sourceHeight);
    }

    public static void resize(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int sourceWidth, int sourceHeight, int texWidth, int texHeight, ResourceLocation texture) {
        setShaderTexture(0, texture);
        resize(poseStack, x, y, u, v, width, height, sourceWidth, sourceHeight, texWidth, texHeight);
    }

    public static int getPixelColour(int x, int y) {
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        int sx = (int) (x * guiScale);
        int sy = (int) ((window.getGuiScaledHeight() - y) * guiScale);
        BUFFER.rewind();
        GL11.glReadPixels(sx, sy, 1, 1, GL11.GL_RGB, GL11.GL_FLOAT, BUFFER);
        GL11.glFinish();
        int r = Math.round(BUFFER.get() * 255);
        int g = Math.round(BUFFER.get() * 255);
        int b = Math.round(BUFFER.get() * 255);
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    public static void addClipRect(int x, int y, int width, int height) {
        addClipRect(new CGRect(x, y, width, height));
    }

    public static void addClipRect(CGRect rect) {
        if (!clipBounds.isEmpty()) {
            CGRect rect1 = clipBounds.getLast();
            rect = rect.intersection(rect1);
        }
        clipBounds.add(rect);
        applyScissor(rect);
    }

    public static void removeClipRect() {
        if (clipBounds.isEmpty()) {
            return;
        }
        clipBounds.removeLast();
        if (!clipBounds.isEmpty()) {
            applyScissor(clipBounds.getLast());
        } else {
            disableScissor();
        }
    }

    public static void applyScissor(CGRect rect) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        double sx = rect.getX() * scale;
        double sy = window.getHeight() - rect.getMaxY() * scale;
        double sw = rect.getWidth() * scale;
        double sh = rect.getHeight() * scale;
        enableScissor((int) sx, (int) sy, (int) sw, (int) sh);
    }

    public static boolean inScissorRect(CGRect rect1) {
        if (!clipBounds.isEmpty()) {
            CGRect rect = clipBounds.getLast();
            return rect.intersects(rect1);
        }
        return true;
    }

    public static boolean inScissorRect(int x, int y, int width, int height) {
        if (!clipBounds.isEmpty()) {
            CGRect rect = clipBounds.getLast();
            return rect.intersects(x, y, width, height);
        }
        return true;
    }

    public static void drawText(IPoseStack poseStack, Font font, FormattedText text, int x, int y, int width, int zLevel, int textColor) {
        drawText(poseStack, font, Collections.singleton(text), x, y, width, zLevel, false, 9, textColor);
    }

    public static void drawShadowText(IPoseStack poseStack, Iterable<FormattedText> lines, int x, int y, int width, int zLevel, Font font, int fontSize, int textColor) {
        drawText(poseStack, font, lines, x, y, width, zLevel, true, fontSize, textColor);
    }

    public static void drawText(IPoseStack poseStack, Font font, Iterable<FormattedText> lines, int x, int y, int width, int zLevel, boolean shadow, int fontSize, int textColor) {
        float f = fontSize / 9f;
        ArrayList<FormattedText> wrappedTextLines = new ArrayList<>();
        for (FormattedText line : lines) {
            wrappedTextLines.addAll(font.getSplitter().splitLines(line, (int) (width / f), Style.EMPTY));
        }
        poseStack.pushPose();
        poseStack.translate(x, y, zLevel);
        poseStack.scale(f, f, f);
        IMatrix4f mat = poseStack.lastPose();
        MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        int dx = 0, dy = 0;
        for (FormattedText line : wrappedTextLines) {
            int qx = font.drawInBatch2(Language.getInstance().getVisualOrder(line), dx, dy, textColor, shadow, mat, buffers, false, 0, 15728880);
            if (qx == dx) {
                dy += 7;
            } else {
                dy += 10;
            }
        }

        buffers.endBatch();
        poseStack.popPose();

        // drawing text causes the Alpha test to reset
        enableAlphaTest();
    }

    public static void drawPlayerHead(IPoseStack poseStack, int x, int y, int width, int height, PlayerTextureDescriptor descriptor) {
        ResourceLocation texture = DefaultPlayerSkin.getDefaultSkin();
        if (!descriptor.isEmpty()) {
            PlayerTexture texture1 = PlayerTextureLoader.getInstance().loadTexture(descriptor);
            if (texture1 != null) {
                texture = texture1.getLocation();
            }
        }
        setShaderTexture(0, texture);
        resize(poseStack, x, y, 8, 8, width, height, 8, 8, 64, 64);
        resize(poseStack, x - 1, y - 1, 40, 8, width + 2, height + 2, 8, 8, 64, 64);
    }

    private static void drawLine(IPoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, VertexConsumer builder) {
        float nx = 0, ny = 0, nz = 0;
        if (x0 != x1) {
            nx = 1;
        }
        if (y0 != y1) {
            ny = 1;
        }
        if (z0 != z1) {
            nz = 1;
        }
        IMatrix4f mat = poseStack.lastPose();
        IMatrix3f normal = poseStack.lastNormal();
        builder.vertex(mat, x0, y0, z0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(normal, nx, ny, nz).endVertex();
        builder.vertex(mat, x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(normal, nx, ny, nz).endVertex();
    }

    public static void drawBoundingBox(IPoseStack mat, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, VertexConsumer builder) {
        drawLine(mat, x1, y0, z1, x0, y0, z1, color, builder);
        drawLine(mat, x1, y0, z1, x1, y1, z1, color, builder);
        drawLine(mat, x1, y0, z1, x1, y0, z0, color, builder);
        drawLine(mat, x1, y1, z0, x0, y1, z0, color, builder);
        drawLine(mat, x1, y1, z0, x1, y0, z0, color, builder);
        drawLine(mat, x1, y1, z0, x1, y1, z1, color, builder);
        drawLine(mat, x0, y1, z1, x1, y1, z1, color, builder);
        drawLine(mat, x0, y1, z1, x0, y0, z1, color, builder);
        drawLine(mat, x0, y1, z1, x0, y1, z0, color, builder);
        drawLine(mat, x0, y0, z0, x1, y0, z0, color, builder);
        drawLine(mat, x0, y0, z0, x0, y1, z0, color, builder);
        drawLine(mat, x0, y0, z0, x0, y0, z1, color, builder);
    }

    public static void drawPoint(IPoseStack poseStack, @Nullable MultiBufferSource renderTypeBuffer) {
        drawPoint(poseStack, null, 2, renderTypeBuffer);
    }

    public static void drawPoint(IPoseStack poseStack, @Nullable Vector3f point, float size, @Nullable MultiBufferSource renderTypeBuffer) {
        drawPoint(poseStack, point, size, size, size, renderTypeBuffer);
    }

    public static void drawPoint(IPoseStack poseStack, @Nullable Vector3f point, float width, float height, float depth, @Nullable MultiBufferSource renderTypeBuffer) {
        if (renderTypeBuffer == null) {
            renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        }
        VertexConsumer builder = renderTypeBuffer.getBuffer(SkinRenderType.lines());
        float x0 = 0;
        float y0 = 0;
        float z0 = 0;
        if (point != null) {
            x0 = point.getX();
            y0 = point.getY();
            z0 = point.getZ();
        }
        drawLine(poseStack, x0 - width, y0, z0, x0 + width, y0, z0, UIColor.RED, builder); // x
        drawLine(poseStack, x0, y0 - height, z0, x0, y0 + height, z0, UIColor.GREEN, builder); // Y
        drawLine(poseStack, x0, y0, z0 - depth, x0, y0, z0 + depth, UIColor.BLUE, builder); // Z
    }

    public static void drawTargetBox(IPoseStack poseStack, float width, float height, float depth, MultiBufferSource buffers) {
        if (ModDebugger.targetBounds) {
            drawBoundingBox(poseStack, -width / 2, -height / 2, -depth / 2, width / 2, height / 2, depth / 2, UIColor.ORANGE, buffers);
            drawPoint(poseStack, null, width, height, depth, buffers);
        }
    }

    public static void drawBoundingBox(IPoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, MultiBufferSource buffers) {
        VertexConsumer builder = buffers.getBuffer(SkinRenderType.lines());
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, builder);
    }

    public static void drawBoundingBox(IPoseStack poseStack, CGRect rect, UIColor color) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        drawBoundingBox(poseStack, rect.x, rect.y, 0, rect.x + rect.width, rect.y + rect.height, 0, color, buffers);
        buffers.endBatch();
    }

//    public static void drawAllEdges(IPoseStack matrix, VoxelShape shape, UIColor color) {
//        MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
//        VertexConsumer builder = buffer.getBuffer(RenderType.lines());
//        Matrix4f mat = matrix.last().pose();
//        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
//            builder.vertex(mat, (float) x0, (float) y0, (float) z0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
//            builder.vertex(mat, (float) x1, (float) y1, (float) z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
//        });
//    }

    public static void drawBoundingBox(IPoseStack poseStack, Rectangle3f rec, UIColor color, MultiBufferSource renderTypeBuffer) {
        float x0 = rec.getMinX();
        float y0 = rec.getMinY();
        float z0 = rec.getMinZ();
        float x1 = rec.getMaxX();
        float y1 = rec.getMaxY();
        float z1 = rec.getMaxZ();
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, renderTypeBuffer);
    }

    public static void drawBoundingBox(IPoseStack poseStack, Rectangle3i rec, UIColor color, MultiBufferSource renderTypeBuffer) {
        int x0 = rec.getMinX();
        int y0 = rec.getMinY();
        int z0 = rec.getMinZ();
        int x1 = rec.getMaxX();
        int y1 = rec.getMaxY();
        int z1 = rec.getMaxZ();
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, renderTypeBuffer);
    }

    public static void drawBoundingBox(IPoseStack poseStack, AABB rec, UIColor color, MultiBufferSource renderTypeBuffer) {
        float x0 = (float) rec.minX;
        float y0 = (float) rec.minY;
        float z0 = (float) rec.minZ;
        float x1 = (float) rec.maxX;
        float y1 = (float) rec.maxY;
        float z1 = (float) rec.maxZ;
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, renderTypeBuffer);
    }

    public static void drawArmatureBox(IPoseStack poseStack, ITransformf[] transforms) {
        if (transforms == null) {
            return;
        }
        float f1 = 1 / 16.f;
        poseStack.pushPose();
        poseStack.scale(f1, f1, f1);
        MultiBufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        ModelBinder.BIPPED_BOXES.forEach((joint, rect) -> {
            ITransformf transform = transforms[joint.getId()];
            if (transform == null) {
                return;
            }
            poseStack.pushPose();

            transform.apply(poseStack);

//			poseStack.translate(box.o.getX(), box.o.getY(), box.o.getZ());
            RenderSystem.drawBoundingBox(poseStack, rect, ColorUtils.getPaletteColor(joint.getId()), buffers);
            RenderSystem.drawPoint(poseStack, Vector3f.ZERO, 4, 4, 4, buffers);
            poseStack.popPose();
        });
        poseStack.popPose();
    }

    public static void drawCube(IPoseStack poseStack, IRectangle3i rect, float r, float g, float b, float a, MultiBufferSource buffers) {
        float x = rect.getMinX();
        float y = rect.getMinY();
        float z = rect.getMinZ();
        float w = rect.getWidth();
        float h = rect.getHeight();
        float d = rect.getDepth();
        drawCube(poseStack, x, y, z, w, h, d, r, g, b, a, buffers);
    }

    public static void drawCube(IPoseStack poseStack, IRectangle3f rect, float r, float g, float b, float a, MultiBufferSource buffers) {
        float x = rect.getMinX();
        float y = rect.getMinY();
        float z = rect.getMinZ();
        float w = rect.getWidth();
        float h = rect.getHeight();
        float d = rect.getDepth();
        drawCube(poseStack, x, y, z, w, h, d, r, g, b, a, buffers);
    }

    public static void drawCube(IPoseStack poseStack, float x, float y, float z, float w, float h, float d, float r, float g, float b, float a, MultiBufferSource buffers) {
        if (w == 0 || h == 0 || d == 0) {
            return;
        }
        IMatrix4f mat = poseStack.lastPose();
        SkinVertexBufferBuilder builder1 = SkinVertexBufferBuilder.getBuffer(buffers);
        VertexConsumer builder = builder1.getBuffer(SkinRenderType.IMAGE_GUIDE);
        for (Direction dir : Direction.values()) {
            drawFace(mat, dir, x, y, z, w, h, d, 0, 0, r, g, b, a, builder);
        }
    }

    public static void drawFace(IMatrix4f mat, Direction dir, float x, float y, float z, float w, float h, float d, float u, float v, float r, float g, float b, float a, VertexConsumer builder) {
        byte[][] vertexes = FACE_MARK_VERTEXES[dir.get3DDataValue()];
        byte[][] textures = FACE_MARK_TEXTURES[dir.get3DDataValue()];
        float[] values = {0, w, h, d};
        for (int i = 0; i < 4; ++i) {
            builder.vertex(mat, x + vertexes[i][0] * w, y + vertexes[i][1] * h, z + vertexes[i][2] * d)
                    .color(r, g, b, a)
                    .uv(u + values[textures[i][0]], v + values[textures[i][1]])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xf000f0)
                    .endVertex();
        }
    }

    /**
     * Draws a textured box of any size (smallest size is borderSize * 2 square) based on a fixed size textured box with continuous borders
     * and filler. It is assumed that the desired texture ResourceLocation object has been bound using
     * Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation).
     *
     * @param poseStack   the gui matrix stack
     * @param x             x axis offset
     * @param y             y axis offset
     * @param u             bound resource location image x offset
     * @param v             bound resource location image y offset
     * @param width         the desired box width
     * @param height        the desired box height
     * @param textureWidth  the width of the box texture in the resource location image
     * @param textureHeight the height of the box texture in the resource location image
     * @param borderSize    the size of the box's borders
     * @param zLevel        the zLevel to draw at
     */
    public static void drawContinuousTexturedBox(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight,
                                                 int borderSize, float zLevel) {
        drawContinuousTexturedBox(poseStack, x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize, zLevel);
    }

    /**
     * Draws a textured box of any size (smallest size is borderSize * 2 square) based on a fixed size textured box with continuous borders
     * and filler. The provided ResourceLocation object will be bound using
     * Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation).
     *
     * @param poseStack   the gui matrix stack
     * @param res           the ResourceLocation object that contains the desired image
     * @param x             x axis offset
     * @param y             y axis offset
     * @param u             bound resource location image x offset
     * @param v             bound resource location image y offset
     * @param width         the desired box width
     * @param height        the desired box height
     * @param textureWidth  the width of the box texture in the resource location image
     * @param textureHeight the height of the box texture in the resource location image
     * @param borderSize    the size of the box's borders
     * @param zLevel        the zLevel to draw at
     */
    public static void drawContinuousTexturedBox(IPoseStack poseStack, ResourceLocation res, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight,
                                                 int borderSize, float zLevel) {
        drawContinuousTexturedBox(poseStack, res, x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize, zLevel);
    }

    /**
     * Draws a textured box of any size (smallest size is borderSize * 2 square) based on a fixed size textured box with continuous borders
     * and filler. The provided ResourceLocation object will be bound using
     * Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation).
     *
     * @param poseStack   the gui matrix stack
     * @param res           the ResourceLocation object that contains the desired image
     * @param x             x axis offset
     * @param y             y axis offset
     * @param u             bound resource location image x offset
     * @param v             bound resource location image y offset
     * @param width         the desired box width
     * @param height        the desired box height
     * @param textureWidth  the width of the box texture in the resource location image
     * @param textureHeight the height of the box texture in the resource location image
     * @param topBorder     the size of the box's top border
     * @param bottomBorder  the size of the box's bottom border
     * @param leftBorder    the size of the box's left border
     * @param rightBorder   the size of the box's right border
     * @param zLevel        the zLevel to draw at
     */
    public static void drawContinuousTexturedBox(IPoseStack poseStack, ResourceLocation res, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight,
                                                 int topBorder, int bottomBorder, int leftBorder, int rightBorder, float zLevel) {
        setShaderTexture(0, res);
        drawContinuousTexturedBox(poseStack, x, y, u, v, width, height, textureWidth, textureHeight, topBorder, bottomBorder, leftBorder, rightBorder, zLevel);
    }

    /**
     * Draws a textured box of any size (smallest size is borderSize * 2 square) based on a fixed size textured box with continuous borders
     * and filler. It is assumed that the desired texture ResourceLocation object has been bound using
     * Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation).
     *
     * @param poseStack   the gui matrix stack
     * @param x             x axis offset
     * @param y             y axis offset
     * @param u             bound resource location image x offset
     * @param v             bound resource location image y offset
     * @param width         the desired box width
     * @param height        the desired box height
     * @param textureWidth  the width of the box texture in the resource location image
     * @param textureHeight the height of the box texture in the resource location image
     * @param topBorder     the size of the box's top border
     * @param bottomBorder  the size of the box's bottom border
     * @param leftBorder    the size of the box's left border
     * @param rightBorder   the size of the box's right border
     * @param zLevel        the zLevel to draw at
     */
    public static void drawContinuousTexturedBox(IPoseStack poseStack, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight,
                                                 int topBorder, int bottomBorder, int leftBorder, int rightBorder, float zLevel) {
        setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        enableBlend();
        defaultBlendFunc();

        int fillerWidth = textureWidth - leftBorder - rightBorder;
        int fillerHeight = textureHeight - topBorder - bottomBorder;
        int canvasWidth = width - leftBorder - rightBorder;
        int canvasHeight = height - topBorder - bottomBorder;
        int xPasses = canvasWidth / fillerWidth;
        int remainderWidth = canvasWidth % fillerWidth;
        int yPasses = canvasHeight / fillerHeight;
        int remainderHeight = canvasHeight % fillerHeight;

        IMatrix4f mat = poseStack.lastPose();
        AbstractShaderTesselator tesselator = AbstractShaderTesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(SkinRenderType.GUI_IMAGE);

        // Draw Border
        // Top Left
        _drawTexturedModalRect(mat, x, y, u, v, leftBorder, topBorder, zLevel, bufferBuilder);
        // Top Right
        _drawTexturedModalRect(mat, x + leftBorder + canvasWidth, y, u + leftBorder + fillerWidth, v, rightBorder, topBorder, zLevel, bufferBuilder);
        // Bottom Left
        _drawTexturedModalRect(mat, x, y + topBorder + canvasHeight, u, v + topBorder + fillerHeight, leftBorder, bottomBorder, zLevel, bufferBuilder);
        // Bottom Right
        _drawTexturedModalRect(mat, x + leftBorder + canvasWidth, y + topBorder + canvasHeight, u + leftBorder + fillerWidth, v + topBorder + fillerHeight, rightBorder, bottomBorder, zLevel, bufferBuilder);

        for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
            // Top Border
            _drawTexturedModalRect(mat, x + leftBorder + (i * fillerWidth), y, u + leftBorder, v, (i == xPasses ? remainderWidth : fillerWidth), topBorder, zLevel, bufferBuilder);
            // Bottom Border
            _drawTexturedModalRect(mat, x + leftBorder + (i * fillerWidth), y + topBorder + canvasHeight, u + leftBorder, v + topBorder + fillerHeight, (i == xPasses ? remainderWidth : fillerWidth), bottomBorder, zLevel, bufferBuilder);

            // Throw in some filler for good measure
            for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++)
                _drawTexturedModalRect(mat, x + leftBorder + (i * fillerWidth), y + topBorder + (j * fillerHeight), u + leftBorder, v + topBorder, (i == xPasses ? remainderWidth : fillerWidth), (j == yPasses ? remainderHeight : fillerHeight), zLevel, bufferBuilder);
        }

        // Side Borders
        for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            // Left Border
            _drawTexturedModalRect(mat, x, y + topBorder + (j * fillerHeight), u, v + topBorder, leftBorder, (j == yPasses ? remainderHeight : fillerHeight), zLevel, bufferBuilder);
            // Right Border
            _drawTexturedModalRect(mat, x + leftBorder + canvasWidth, y + topBorder + (j * fillerHeight), u + leftBorder + fillerWidth, v + topBorder, rightBorder, (j == yPasses ? remainderHeight : fillerHeight), zLevel, bufferBuilder);
        }

        tesselator.end();
    }

    private static void _drawTexturedModalRect(IMatrix4f mat, int x, int y, int u, int v, int width, int height, float zLevel, BufferBuilder bufferBuilder) {
        final float uScale = 1f / 0x100;
        final float vScale = 1f / 0x100;
        bufferBuilder.vertex(mat, x, y + height, zLevel).uv(u * uScale, ((v + height) * vScale)).endVertex();
        bufferBuilder.vertex(mat, x + width, y + height, zLevel).uv((u + width) * uScale, ((v + height) * vScale)).endVertex();
        bufferBuilder.vertex(mat, x + width, y, zLevel).uv((u + width) * uScale, (v * vScale)).endVertex();
        bufferBuilder.vertex(mat, x, y, zLevel).uv(u * uScale, (v * vScale)).endVertex();
    }

//    public static void disableLighting() {
//        net.minecraft.client.GameSettings
//        lightX = OpenGlHelper.lastBrightnessX;
//        lightY = OpenGlHelper.lastBrightnessY;
//        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
//    }

//    public static void enableLighting() {
//        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
//    }

//    public static void setLightingForBlock(Level world, BlockPos pos) {
//        int i = world.getCombinedLight(pos, 0);
//        int j = i % 65536;
//        int k = i / 65536;
//        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
//    }

    public static void setShaderColor(UIColor color) {
        setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public static void setShaderColor(float f, float g, float h) {
        setShaderColor(f, g, h, 1.0f);
    }

    public static class Storage<T> {

        private T value;
        private T backup;

        public Storage(T value) {
            this.value = value;
            this.backup = value;
        }

        public void save() {
            backup = value;
        }

        public void load() {
            value = backup;
        }

        public void set(T value) {
            if (!isOnRenderThread()) {
                recordRenderCall(() -> this.value = value);
            } else {
                this.value = value;
            }
        }

        public T get() {
            assertOnRenderThread();
            return value;
        }
    }

    public static IMatrix3f getExtendedNormalMatrix() {
        return extendedNormalMatrix.get();
    }

    public static void setExtendedNormalMatrix(IMatrix3f value) {
        extendedNormalMatrix.set(value);
    }

    public static IMatrix4f getExtendedTextureMatrix() {
        return extendedTextureMatrix.get();
    }

    public static void setExtendedTextureMatrix(IMatrix4f value) {
        extendedTextureMatrix.set(value);
    }

    public static IMatrix4f getExtendedLightmapTextureMatrix() {
        return extendedLightmapTextureMatrix.get();
    }

    public static void setExtendedLightmapTextureMatrix(IMatrix4f value) {
        extendedLightmapTextureMatrix.set(value);
    }

    public static IMatrix4f getExtendedModelViewMatrix() {
        return extendedModelViewMatrix.get();
    }

    public static void setExtendedModelViewMatrix(IMatrix4f value) {
        extendedModelViewMatrix.set(value);
    }

    public static void backupExtendedMatrix() {
        extendedTextureMatrix.save();
        extendedNormalMatrix.save();
        extendedLightmapTextureMatrix.save();
        extendedModelViewMatrix.save();
    }

    public static void restoreExtendedMatrix() {
        extendedTextureMatrix.load();
        extendedNormalMatrix.load();
        extendedLightmapTextureMatrix.load();
        extendedModelViewMatrix.load();
    }

    public static IPoseStack getExtendedModelViewStack() {
        return extendedModelViewStack;
    }
}
