package moe.plushie.armourers_workshop.builder.other;

import moe.plushie.armourers_workshop.api.math.IRectangle3i;
import moe.plushie.armourers_workshop.api.math.ITexturePos;
import moe.plushie.armourers_workshop.api.math.IVector3i;
import moe.plushie.armourers_workshop.api.painting.IPaintColor;
import moe.plushie.armourers_workshop.api.painting.IPaintable;
import moe.plushie.armourers_workshop.api.skin.*;
import moe.plushie.armourers_workshop.builder.block.SkinCubeBlock;
import moe.plushie.armourers_workshop.core.data.OptionalDirection;
import moe.plushie.armourers_workshop.core.data.color.PaintColor;
import moe.plushie.armourers_workshop.core.skin.Skin;
import moe.plushie.armourers_workshop.core.skin.SkinTypes;
import moe.plushie.armourers_workshop.core.skin.cube.SkinCubeData;
import moe.plushie.armourers_workshop.core.skin.cube.SkinCubes;
import moe.plushie.armourers_workshop.core.skin.data.SkinMarker;
import moe.plushie.armourers_workshop.core.skin.data.serialize.SkinSerializer;
import moe.plushie.armourers_workshop.core.skin.exception.SkinSaveException;
import moe.plushie.armourers_workshop.core.skin.painting.SkinPaintTypes;
import moe.plushie.armourers_workshop.core.skin.part.SkinPart;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTypes;
import moe.plushie.armourers_workshop.core.skin.property.SkinProperties;
import moe.plushie.armourers_workshop.core.skin.property.SkinProperty;
import moe.plushie.armourers_workshop.init.ModBlocks;
import moe.plushie.armourers_workshop.utils.math.Rectangle3i;
import moe.plushie.armourers_workshop.utils.math.Vector3i;
import moe.plushie.armourers_workshop.utils.texture.SkinPaintData;
import moe.plushie.armourers_workshop.utils.texture.SkyBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Helper class for converting back and forth from
 * in world blocks to skin classes.
 * <p>
 * Note: Minecraft models are inside out, blocks are
 * flipped when loading and saving.
 *
 * @author RiskyKen
 */
public final class WorldUtils {

    /**
     * Converts blocks in the world into a skin class.
     *
     * @param level     The world.
     * @param transform the armourer transform.
     * @param skinProps The skin properties for this skin.
     * @param skinType  The type of skin to save.
     * @param paintData Paint data for this skin.
     */
    public static Skin saveSkinFromWorld(Level level, CubeTransform transform, SkinProperties skinProps, ISkinType skinType, SkinPaintData paintData) throws SkinSaveException {

        ArrayList<SkinPart> parts = new ArrayList<>();

        if (skinType == SkinTypes.BLOCK) {
            ISkinPartType partType = SkinPartTypes.BLOCK;
            if (skinProps.get(SkinProperty.BLOCK_MULTIBLOCK)) {
                partType = SkinPartTypes.BLOCK_MULTI;
            }
            SkinPart skinPart = saveArmourPart(level, transform, partType, true);
            if (skinPart != null) {
                parts.add(skinPart);
            }
        } else {
            for (ISkinPartType partType : skinType.getParts()) {
                SkinPart skinPart = saveArmourPart(level, transform, partType, true);
                if (skinPart != null) {
                    parts.add(skinPart);
                }
            }
        }
        // TODO: support v2 texture
        // because old skin not support v2 texture format,
        // so downgrade v2 to v1 texture format.
        if (paintData != null) {
            SkinPaintData resolvedPaintData = SkinPaintData.v1();
            resolvedPaintData.copyFrom(paintData);
            paintData = resolvedPaintData;
        }

        Skin skin = SkinSerializer.makeSkin(skinType, skinProps, paintData, parts);

        // check if there are any blocks in the build guides.
        if (skin.getParts().size() == 0 && skin.getPaintData() == null) {
            throw new SkinSaveException("Nothing to save.", SkinSaveException.SkinSaveExceptionType.NO_DATA);
        }

        // check if the skin has all needed parts.
        for (ISkinPartType partType : skinType.getParts()) {
            if (partType.isPartRequired()) {
                boolean havePart = false;
                for (ISkinPart part : skin.getParts()) {
                    if (partType == part.getType()) {
                        havePart = true;
                        break;
                    }
                }
                if (!havePart) {
                    throw new SkinSaveException("Skin is missing part " + partType.getRegistryName(), SkinSaveException.SkinSaveExceptionType.MISSING_PARTS);
                }
            }
        }

        // check if the skin is not a seat and a bed.
        if (skinProps.get(SkinProperty.BLOCK_BED) && skinProps.get(SkinProperty.BLOCK_SEAT)) {
            throw new SkinSaveException("Skin can not be a bed and a seat.", SkinSaveException.SkinSaveExceptionType.BED_AND_SEAT);
        }

        // check if multi-block is valid.
        if (skinType == SkinTypes.BLOCK && skinProps.get(SkinProperty.BLOCK_MULTIBLOCK)) {
            SkinPart testPart = saveArmourPart(level, transform, SkinPartTypes.BLOCK, true);
            if (testPart == null) {
                throw new SkinSaveException("Multiblock has no blocks in the yellow area.", SkinSaveException.SkinSaveExceptionType.INVALID_MULTIBLOCK);
            }
        }

        return skin;
    }

    private static SkinPart saveArmourPart(Level level, CubeTransform transform, ISkinPartType skinPart, boolean markerCheck) throws SkinSaveException {

        int cubeCount = getNumberOfCubesInPart(level, transform, skinPart);
        if (cubeCount < 1) {
            return null;
        }
        SkinCubeData cubeData = new SkinCubeData(skinPart);
        cubeData.setCubeCount(cubeCount);

        ArrayList<SkinMarker> markerBlocks = new ArrayList<>();

        IRectangle3i buildSpace = skinPart.getBuildingSpace();
        IVector3i offset = skinPart.getOffset();

        int i = 0;
        for (int ix = 0; ix < buildSpace.getWidth(); ix++) {
            for (int iy = 0; iy < buildSpace.getHeight(); iy++) {
                for (int iz = 0; iz < buildSpace.getDepth(); iz++) {
                    BlockPos target = transform.mul(
                            ix + -offset.getX() + buildSpace.getX(),
                            iy + -offset.getY(),
                            iz + offset.getZ() + buildSpace.getZ());

//                    BlockPos origin = new BlockPos(-ix + -buildSpace.getX(), -iy + -buildSpace.getY(), -iz + -buildSpace.getZ());

                    int xOrigin = -ix + -buildSpace.getX();
                    int yOrigin = -iy + -buildSpace.getY();
                    int zOrigin = -iz + -buildSpace.getZ();

                    BlockState targetState = level.getBlockState(target);
                    if (targetState.getBlock() instanceof SkinCubeBlock) {
                        saveArmourBlockToList(level, transform, target,
                                xOrigin - 1,
                                yOrigin - 1,
                                -zOrigin,
                                cubeData.at(i), markerBlocks);
                        i++;
                    }
                }
            }
        }

        if (markerCheck) {
            if (skinPart.getMinimumMarkersNeeded() > markerBlocks.size()) {
                throw new SkinSaveException("Missing marker for part " + skinPart.getRegistryName(), SkinSaveException.SkinSaveExceptionType.MARKER_ERROR);
            }

            if (markerBlocks.size() > skinPart.getMaximumMarkersNeeded()) {
                throw new SkinSaveException("Too many markers for part " + skinPart.getRegistryName(), SkinSaveException.SkinSaveExceptionType.MARKER_ERROR);
            }
        }

        return new SkinPart(skinPart, markerBlocks, cubeData);
    }

    private static void saveArmourBlockToList(Level level, CubeTransform transform, BlockPos pos, int ix, int iy, int iz, SkinCubeData.BufferSlice slice, ArrayList<SkinMarker> markerBlocks) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof IPaintable)) {
            return;
        }
        BlockState blockState = tileEntity.getBlockState();
        IPaintable target = (IPaintable) tileEntity;
        ISkinCube cube = SkinCubes.byBlock(blockState.getBlock());

        OptionalDirection marker = SkinCubeBlock.getMarker(blockState);

        slice.setId((byte) cube.getId());
        slice.setX((byte) ix);
        slice.setY((byte) iy);
        slice.setZ((byte) iz);

        for (Direction dir : Direction.values()) {
            IPaintColor color = target.getColor(dir);
            Direction resolvedDir = transform.invRotate(dir);
            slice.setRGB(resolvedDir.ordinal(), color.getRGB());
            slice.setPaintType(resolvedDir.ordinal(), (byte) color.getPaintType().getId());
        }
        if (marker != OptionalDirection.NONE) {
            OptionalDirection resolvedMarker = OptionalDirection.of(transform.invRotate(marker.getDirection()));
            markerBlocks.add(new SkinMarker((byte) ix, (byte) iy, (byte) iz, (byte) resolvedMarker.ordinal()));
        }
    }

    /**
     * Converts a skin class into blocks in the world.
     *
     * @param applier   The world applier.
     * @param transform The armourer transform.
     * @param skin      The skin to load.
     */
    public static void loadSkinIntoWorld(CubeApplier applier, CubeTransform transform, Skin skin) {
        for (SkinPart part : skin.getParts()) {
            loadSkinPartIntoWorld(applier, transform, part, false);
        }
    }

    private static void loadSkinPartIntoWorld(CubeApplier applier, CubeTransform transform, SkinPart partData, boolean mirror) {
        ISkinPartType skinPart = partData.getType();
        IRectangle3i buildSpace = skinPart.getBuildingSpace();
        IVector3i offset = skinPart.getOffset();
        SkinCubeData cubeData = partData.getCubeData();

        for (int i = 0; i < cubeData.getCubeCount(); i++) {
            SkinCubeData.BufferSlice slice = cubeData.at(i);
            Vector3i cubePos = slice.getPos();
            ISkinCube blockData = SkinCubes.byId(slice.getId());
            OptionalDirection markerFacing = OptionalDirection.NONE;
            for (ISkinMarker marker : partData.getMarkers()) {
                if (cubePos.equals(marker.getPosition())) {
                    Direction resolvedMarker = getResolvedDirection(marker.getDirection(), mirror);
                    markerFacing = OptionalDirection.of(transform.rotate(resolvedMarker));
                    break;
                }
            }
            BlockPos origin = new BlockPos(-offset.getX(), -offset.getY() + -buildSpace.getY(), offset.getZ());
            loadSkinBlockIntoWorld(applier, transform, origin, blockData, cubePos, markerFacing, slice, mirror);
        }
    }

    private static void loadSkinBlockIntoWorld(CubeApplier applier, CubeTransform transform, BlockPos origin, ISkinCube blockData, Vector3i cubePos, OptionalDirection markerFacing, SkinCubeData.BufferSlice slice, boolean mirror) {
        int shiftX = -cubePos.getX() - 1;
        int shiftY = cubePos.getY() + 1;
        int shiftZ = cubePos.getZ();
        if (mirror) {
            shiftX = cubePos.getX();
        }

        BlockPos target = transform.mul(shiftX + origin.getX(), origin.getY() - shiftY, shiftZ + origin.getZ());
        CubeWrapper wrapper = applier.wrap(target);

        if (wrapper.is(ModBlocks.BOUNDING_BOX.get())) {
            wrapper.setBlockState(Blocks.AIR.defaultBlockState(), (CompoundTag) null);
        }

        Block targetBlock = blockData.getBlock();
        BlockState targetState = SkinCubeBlock.setMarker(targetBlock.defaultBlockState(), markerFacing);

        HashMap<Direction, IPaintColor> colors = new HashMap<>();
        for (Direction dir : Direction.values()) {
            int rgb = slice.getRGB(dir.ordinal());
            int type = slice.getPaintType(dir.ordinal());
            Direction resolvedDir = getResolvedDirection(dir, mirror);
            colors.put(transform.rotate(resolvedDir), PaintColor.of(rgb, SkinPaintTypes.byId(type)));
        }

        wrapper.setBlockState(targetState, colors);
    }

    public static void copyPaintData(SkinPaintData paintData, SkyBox srcBox, SkyBox destBox, boolean isMirrorX) {
        int srcX = srcBox.getBounds().getX();
        int srcY = srcBox.getBounds().getY();
        int srcZ = srcBox.getBounds().getZ();
        int destX = destBox.getBounds().getX();
        int destY = destBox.getBounds().getY();
        int destZ = destBox.getBounds().getZ();
        int destWidth = destBox.getBounds().getWidth();
        HashMap<ITexturePos, Integer> colors = new HashMap<>();
        srcBox.forEach((texture, x, y, z, dir) -> {
            int ix = x - srcX;
            int iy = y - srcY;
            int iz = z - srcZ;
            if (isMirrorX) {
                ix = destWidth - ix - 1;
                dir = getResolvedDirection(dir, true);
            }
            ITexturePos newTexture = destBox.get(ix + destX, iy + destY, iz + destZ, dir);
            if (newTexture == null) {
                return;
            }
            int color = paintData.getColor(texture);
            if (PaintColor.isOpaque(color)) {
                // a special case is to use the mirror to swap the part texture,
                // we will copy the color to the map and then applying it when read finish.
                colors.put(newTexture, color);
            }
        });
        colors.forEach(paintData::setColor);
    }

    public static void clearPaintData(SkinPaintData paintData, SkyBox srcBox) {
        srcBox.forEach((texturePos, x, y, z, dir) -> paintData.setColor(texturePos, 0));
    }

    public static void replaceCubes(CubeApplier applier, CubeTransform transform, ISkinType skinType, SkinProperties skinProps, CubeReplacingEvent event) {
        for (ISkinPartType skinPart : skinType.getParts()) {
            for (Vector3i offset : getResolvedBuildingSpace2(skinPart)) {
                replaceCube(applier, transform.mul(offset), event);
            }
        }
    }

    public static void replaceCube(CubeApplier applier, BlockPos pos, CubeReplacingEvent event) {
        CubeWrapper wrapper = applier.wrap(pos);
        if (event.accept(wrapper)) {
            event.apply(wrapper);
        }
    }

    public static void copyCubes(CubeApplier applier, CubeTransform transform, ISkinType skinType, SkinProperties skinProps, ISkinPartType srcPart, ISkinPartType destPart, boolean mirror) throws SkinSaveException {
        SkinPart skinPart = saveArmourPart(applier.getLevel(), transform, srcPart, false);
        if (skinPart != null) {
            skinPart.setSkinPart(destPart);
            loadSkinPartIntoWorld(applier, transform, skinPart, mirror);
        }
    }

    public static int clearMarkers(CubeApplier applier, CubeTransform transform, ISkinType skinType, SkinProperties skinProps, ISkinPartType partType) {
        int blockCount = 0;
        for (ISkinPartType skinPart : skinType.getParts()) {
            if (partType != SkinPartTypes.UNKNOWN) {
                if (partType != skinPart) {
                    continue;
                }
            }
            if (skinType == SkinTypes.BLOCK) {
                boolean multiblock = skinProps.get(SkinProperty.BLOCK_MULTIBLOCK);
                if (skinPart == SkinPartTypes.BLOCK && !multiblock) {
                    blockCount += clearMarkersForSkinPart(applier, transform, skinPart);
                }
                if (skinPart == SkinPartTypes.BLOCK_MULTI && multiblock) {
                    blockCount += clearMarkersForSkinPart(applier, transform, skinPart);
                }
            } else {
                blockCount += clearMarkersForSkinPart(applier, transform, skinPart);
            }
        }
        return blockCount;
    }

    private static int clearMarkersForSkinPart(CubeApplier applier, CubeTransform transform, ISkinPartType skinPart) {
        int blockCount = 0;
        for (Vector3i offset : getResolvedBuildingSpace2(skinPart)) {
            BlockPos target = transform.mul(offset);
            CubeWrapper wrapper = applier.wrap(target);
            BlockState targetState = wrapper.getBlockState();
            if (targetState.hasProperty(SkinCubeBlock.MARKER) && SkinCubeBlock.getMarker(targetState) != OptionalDirection.NONE) {
                wrapper.setBlockState(SkinCubeBlock.setMarker(targetState, OptionalDirection.NONE));
                blockCount++;
            }
        }
        return blockCount;
    }

    public static int clearCubes(CubeApplier applier, CubeTransform transform, ISkinType skinType, SkinProperties skinProps, ISkinPartType partType) {
        int blockCount = 0;
        for (ISkinPartType skinPart : skinType.getParts()) {
            if (partType != SkinPartTypes.UNKNOWN) {
                if (partType != skinPart) {
                    continue;
                }
            }
            if (skinType == SkinTypes.BLOCK) {
                boolean multiblock = skinProps.get(SkinProperty.BLOCK_MULTIBLOCK);
                if (skinPart == SkinPartTypes.BLOCK && !multiblock) {
                    blockCount += clearEquipmentCubesForSkinPart(applier, transform, skinPart);
                }
                if (skinPart == SkinPartTypes.BLOCK_MULTI && multiblock) {
                    blockCount += clearEquipmentCubesForSkinPart(applier, transform, skinPart);
                }
            } else {
                blockCount += clearEquipmentCubesForSkinPart(applier, transform, skinPart);
            }
        }
        return blockCount;
    }

    private static int clearEquipmentCubesForSkinPart(CubeApplier applier, CubeTransform transform, ISkinPartType skinPart) {
        int blockCount = 0;
        for (Vector3i offset : getResolvedBuildingSpace2(skinPart)) {
            BlockPos target = transform.mul(offset);
            CubeWrapper wrapper = applier.wrap(target);
            if (wrapper.is(SkinCubeBlock.class)) {
                wrapper.setBlockState(Blocks.AIR.defaultBlockState(), (CompoundTag) null);
                blockCount++;
            }
        }
        return blockCount;
    }

    public static Rectangle3i getResolvedBuildingSpace(ISkinPartType skinPart) {
        IVector3i origin = skinPart.getOffset();
        IRectangle3i buildSpace = skinPart.getBuildingSpace();
        int dx = -origin.getX() + buildSpace.getX();
        int dy = -origin.getY();
        int dz = origin.getZ() + buildSpace.getZ();
        return new Rectangle3i(dx, dy, dz, buildSpace.getWidth(), buildSpace.getHeight(), buildSpace.getDepth());
    }

    private static Iterable<Vector3i> getResolvedBuildingSpace2(ISkinPartType skinPart) {
        return getResolvedBuildingSpace(skinPart).enumerateZYX();
    }

    private static int getNumberOfCubesInPart(Level level, CubeTransform transform, ISkinPartType skinPart) {
        int cubeCount = 0;
        for (Vector3i offset : getResolvedBuildingSpace2(skinPart)) {
            BlockState blockState = level.getBlockState(transform.mul(offset));
            if (blockState.getBlock() instanceof SkinCubeBlock) {
                cubeCount++;
            }
        }
        return cubeCount;
    }

    private static Direction getResolvedDirection(Direction dir, boolean mirror) {
        // we're just mirroring the x-axis when if it needs.
        if (mirror && dir.getAxis() == Direction.Axis.X) {
            return dir.getOpposite();
        }
        return dir;
    }
}

