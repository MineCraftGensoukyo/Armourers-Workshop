package moe.plushie.armourers_workshop.core.skin.cube;

import moe.plushie.armourers_workshop.api.skin.ISkinCube;
import moe.plushie.armourers_workshop.api.skin.ISkinPaintType;
import moe.plushie.armourers_workshop.api.skin.ISkinPartType;
import moe.plushie.armourers_workshop.core.data.color.PaintColor;
import moe.plushie.armourers_workshop.core.skin.data.SkinUsedCounter;
import moe.plushie.armourers_workshop.core.skin.data.serialize.LegacyCubeHelper;
import moe.plushie.armourers_workshop.core.skin.exception.InvalidCubeTypeException;
import moe.plushie.armourers_workshop.core.skin.face.SkinCubeFace;
import moe.plushie.armourers_workshop.core.skin.painting.SkinPaintTypes;
import moe.plushie.armourers_workshop.utils.math.OpenVoxelShape;
import moe.plushie.armourers_workshop.utils.math.Rectangle3i;
import moe.plushie.armourers_workshop.utils.math.Vector3i;
import net.minecraft.core.Direction;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SkinCubeData {

    private int cubeCount = 0;
    private BufferSlice bufferSlice;
    private final SkinUsedCounter usedCounter = new SkinUsedCounter();
    private final ISkinPartType partType;

    public SkinCubeData(ISkinPartType partType) {
        this.partType = partType;
    }

    public int getCubeCount() {
        return cubeCount;
    }

    public void setCubeCount(int count) {
        bufferSlice = new BufferSlice(count);
        cubeCount = count;
        usedCounter.reset();
    }

    public ISkinPartType getPartType() {
        return partType;
    }

    public ISkinCube getCube(int index) {
        return SkinCubes.byId(bufferSlice.at(index).getId());
    }

    public void forEach(ICubeConsumer consumer) {
        for (int i = 0; i < cubeCount; ++i) {
            BufferSlice slice = bufferSlice.at(i);
            consumer.apply(i, slice.getX(), slice.getY(), slice.getZ());
        }
    }

    public byte getCubePosX(int index) {
        return bufferSlice.at(index).getX();
    }

    public byte getCubePosY(int index) {
        return bufferSlice.at(index).getY();
    }

    public byte getCubePosZ(int index) {
        return bufferSlice.at(index).getZ();
    }

    public SkinCubeFace getCubeFace(int index, Direction dir) {
        int side = dir.get3DDataValue();
        BufferSlice slice = bufferSlice.at(index);
        ISkinCube cube = SkinCubes.byId(slice.getId());
        ISkinPaintType paintType = SkinPaintTypes.byId(slice.getPaintType(side));

        int rgb = slice.getRGB(side);
        int alpha = 255;
        if (cube.isGlass()) {
            alpha = 127;
        }

        int x = slice.getX();
        int y = slice.getY();
        int z = slice.getZ();

        return new SkinCubeFace(x, y, z, PaintColor.of(rgb, paintType), alpha, dir, cube);
    }

    public OpenVoxelShape getRenderShape() {
        if (bufferSlice == null) {
            return OpenVoxelShape.empty();
        }
        OpenVoxelShape shape = OpenVoxelShape.empty();
        int count = cubeCount;
        if (count == 0) {
            return shape;
        }
        for (int i = 0; i < count; ++i) {
            BufferSlice slice = bufferSlice.at(i);
            shape.add(slice.getX(), slice.getY(), slice.getZ(), 1, 1, 1);
        }
        shape.optimize();
        return shape;
    }

    public Rectangle3i getBounds() {
        byte minX = 127;
        byte minY = 127;
        byte minZ = 127;
        byte maxX = -127;
        byte maxY = -127;
        byte maxZ = -127;
        for (int i = 0; i < cubeCount; ++i) {
            BufferSlice slice = bufferSlice.at(i);
            byte x = slice.getX();
            byte y = slice.getY();
            byte z = slice.getZ();
            if (minX > x) minX = x;
            if (minY > y) minY = y;
            if (minZ > z) minZ = z;
            if (maxX < x) maxX = x;
            if (maxY < y) maxY = y;
            if (maxZ < z) maxZ = z;
        }
        return new Rectangle3i(minX, minY, minZ, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }


    public void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeInt(cubeCount);
        stream.write(bufferSlice.getBuffers());
    }

    public void readFromStream(DataInputStream stream, int version, ISkinPartType skinPart) throws IOException, InvalidCubeTypeException {
        int size = stream.readInt();
        setCubeCount(size);
        if (version >= 10) {
            byte[] buffers = bufferSlice.getBuffers();
            stream.readFully(buffers, 0, size * bufferSlice.lineSize);
            for (int i = 0; i < size; i++) {
                BufferSlice slice = bufferSlice.at(i);
                if (version < 11) {
                    for (int side = 0; side < 6; side++) {
                        slice.setPaintType(side, (byte) 255);
                    }
                }
                usedCounter.addCube(slice.getId());
            }
            return;
        }
        // 1 - 9
        for (int i = 0; i < size; i++) {
            BufferSlice slice = bufferSlice.at(i);
            LegacyCubeHelper.loadLegacyCubeData(this, slice, stream, version, skinPart);
            for (int side = 0; side < 6; side++) {
                slice.setPaintType(side, (byte) 255);
            }
        }
    }

    public BufferSlice at(int index) {
        return bufferSlice.at(index);
    }

    public SkinUsedCounter getUsedCounter() {
        return usedCounter;
    }

    public interface ICubeConsumer {
        void apply(int i, int x, int y, int z);
    }

    public static class BufferSlice {

        final int lineSize = 4 + 4 * 6; // id/x/y/z + r/g/b/t * 6
        final byte[] buffers;

        int writerIndex = 0;
        int readerIndex = 0;

        public BufferSlice(int count) {
            this.buffers = new byte[count * lineSize];
        }

        public BufferSlice at(int index) {
            this.writerIndex = index * lineSize;
            this.readerIndex = index * lineSize;
            return this;
        }

        public byte getId() {
            return getByte(0);
        }

        public void setId(byte id) {
            setByte(0, id);
        }

        public byte getX() {
            return getByte(1);
        }

        public void setX(byte value) {
            setByte(1, value);
        }

        public byte getY() {
            return getByte(2);
        }

        public void setY(byte value) {
            setByte(2, value);
        }

        public byte getZ() {
            return getByte(3);
        }

        public void setZ(byte value) {
            setByte(3, value);
        }

        public void setR(int side, byte value) {
            setByte(4 + side * 4, value);
        }

        public byte getR(int side) {
            return getByte(4 + side * 4);
        }

        public void setG(int side, byte value) {
            setByte(5 + side * 4, value);
        }

        public byte getG(int side) {
            return getByte(5 + side * 4);
        }

        public void setB(int side, byte value) {
            setByte(6 + side * 4, value);
        }

        public byte getB(int side) {
            return getByte(6 + side * 4);
        }

        public void setPaintType(int side, byte value) {
            setByte(7 + side * 4, value);
        }

        public byte getPaintType(int side) {
            return getByte(7 + side * 4);
        }

        public int getRGB(int side) {
            int color = 0;
            color |= (getR(side) & 0xff) << 16;
            color |= (getG(side) & 0xff) << 8;
            color |= (getB(side) & 0xff);
            return color;
        }

        public void setRGB(int side, int rgb) {
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            setR(side, (byte) r);
            setG(side, (byte) g);
            setB(side, (byte) b);
        }

        public Vector3i getPos() {
            return new Vector3i(getX(), getY(), getZ());
        }

        public void setPos(Vector3i pos) {
            setX((byte) pos.getX());
            setY((byte) pos.getY());
            setZ((byte) pos.getZ());
        }

        public void setByte(int offset, byte value) {
            buffers[writerIndex + offset] = value;
        }

        public byte getByte(int offset) {
            return buffers[readerIndex + offset];
        }

        public byte[] getBuffers() {
            return buffers;
        }
    }
}
