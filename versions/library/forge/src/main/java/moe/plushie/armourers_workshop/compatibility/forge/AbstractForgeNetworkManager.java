package moe.plushie.armourers_workshop.compatibility.forge;

import moe.plushie.armourers_workshop.api.annotation.Available;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

@Available("[1.18, )")
public abstract class AbstractForgeNetworkManager extends NetworkEvent {

    public AbstractForgeNetworkManager() {
        super(() -> null);
    }

    public static void register(ResourceLocation channelName, String channelVersion, Object dispatcher) {
        EventNetworkChannel channel = NetworkRegistry.ChannelBuilder.named(channelName).networkProtocolVersion(() -> channelVersion).clientAcceptedVersions(cv -> true).serverAcceptedVersions(channelVersion::equals).eventNetworkChannel();
        channel.registerObject(dispatcher);
    }

    public static PacketDistributor.PacketTarget allPlayers() {
        return PacketDistributor.ALL.noArg();
    }

    public static PacketDistributor.PacketTarget trackingEntityAndSelf(Supplier<Entity> supplier) {
        return PacketDistributor.TRACKING_ENTITY_AND_SELF.with(supplier);
    }

    public enum Direction {
        PLAY_TO_SERVER(NetworkDirection.PLAY_TO_SERVER), PLAY_TO_CLIENT(NetworkDirection.PLAY_TO_CLIENT);

        private final NetworkDirection networkDirection;

        Direction(NetworkDirection networkDirection) {
            this.networkDirection = networkDirection;
        }

        public <T extends Packet<?>> ICustomPacket<T> buildPacket(Pair<FriendlyByteBuf, Integer> packetData, ResourceLocation channelName) {
            return networkDirection.buildPacket(packetData, channelName);
        }
    }
}
