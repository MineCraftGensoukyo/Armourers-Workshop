package moe.plushie.armourers_workshop.init;

import moe.plushie.armourers_workshop.builder.network.*;
import moe.plushie.armourers_workshop.core.network.*;
import moe.plushie.armourers_workshop.library.network.*;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public enum ModPackets {

    UPDATE_CONTEXT(0x00, UpdateContextPacket.class, UpdateContextPacket::new),

    REQUEST_FILE(0x01, RequestSkinPacket.class, RequestSkinPacket::new),
    RESPONSE_FILE(0x02, ResponseSkinPacket.class, ResponseSkinPacket::new),

    OPEN_WARDROBE(0x03, OpenWardrobePacket.class, OpenWardrobePacket::new),
    UPDATE_WARDROBE(0x04, UpdateWardrobePacket.class, UpdateWardrobePacket::new),

    UPDATE_HOLOGRAM_PROJECTOR(0x05, UpdateHologramProjectorPacket.class, UpdateHologramProjectorPacket::new),
    UPDATE_COLOUR_MIXER(0x06, UpdateColorMixerPacket.class, UpdateColorMixerPacket::new),

    EXECUTE_COMMAND(0x07, ExecuteCommandPacket.class, ExecuteCommandPacket::new),

    UPLOAD_FILE(0x40, SaveSkinPacket.class, SaveSkinPacket::new),

    UPDATE_LIBRARY_FILE(0x41, UpdateLibraryFilePacket.class, UpdateLibraryFilePacket::new),
    UPDATE_LIBRARY_FILES(0x42, UpdateLibraryFilesPacket.class, UpdateLibraryFilesPacket::new),

    UPLOAD_SKIN_TO_GLOBAL(0x43, UploadSkinPacket.class, UploadSkinPacket::new),
    UPLOAD_SKIN_TO_GLOBAL_PRE(0x44, UploadSkinPrePacket.class, UploadSkinPrePacket::new),

    UPDATE_BLOCK_COLOR(0x80, UpdateBlockColorPacket.class, UpdateBlockColorPacket::new),

    UPDATE_COLOR_PICKER(0x81, UpdateColorPickerPacket.class, UpdateColorPickerPacket::new),
    UPDATE_PAINTING_TOOL(0x82, UpdatePaintingToolPacket.class, UpdatePaintingToolPacket::new),

    UNDO_ACTION(0x83, UndoActionPacket.class, UndoActionPacket::new),

    UPDATE_OUTFIT_MAKER(0x84, UpdateOutfitMakerPacket.class, UpdateOutfitMakerPacket::new),
    UPDATE_ARMOURER(0x85, UpdateArmourerPacket.class, UpdateArmourerPacket::new);

    ModPackets(int index, Class<? extends CustomPacket> packetClass, Function<FriendlyByteBuf, CustomPacket> factory) {
        CustomPacket.register(index, packetClass, factory);
    }

    public static void init() {
    }
}
