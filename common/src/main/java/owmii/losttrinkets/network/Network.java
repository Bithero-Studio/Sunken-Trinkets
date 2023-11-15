package owmii.losttrinkets.network;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import io.netty.buffer.Unpooled;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import owmii.losttrinkets.EnvHandler;
import owmii.losttrinkets.LostTrinkets;
import owmii.losttrinkets.network.packet.*;

public final class Network {
    private static final ResourceLocation PACKET_ID = new ResourceLocation(LostTrinkets.MOD_ID, "packet");
    private static int nextId = 0;
    private static List<Constructor<? extends IPacket>> decoders = new ArrayList<>();
    private static IdentityHashMap<Class<?>, Integer> packetIds = new IdentityHashMap<>();

    public static <T extends IPacket> void register(Class<T> packetClass) {
        Constructor<T> constructor = null;
        try {
            constructor = packetClass.getConstructor(PacketBuffer.class);
            decoders.add(constructor);
            packetIds.put(packetClass, nextId);
            nextId++;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to register packet", e);
        }
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PACKET_ID, (buf, ctx) -> {
            int packetId = buf.readVarInt();

            try {
                IPacket packet = decoders.get(packetId).newInstance(buf);
                ctx.queue(() -> {
                    packet.handle(ctx.getPlayer());
                });
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to construct packet of type " + packetId, e);
            }
        });

        register(SyncDataPacket.class);
        register(SetActivePacket.class);
        register(SetInactivePacket.class);
        register(UnlockSlotPacket.class);
        register(TrinketUnlockedPacket.class);
        register(SyncFlyPacket.class);
        register(MagnetoPacket.class);
    }

    private static PacketBuffer encodePacket(IPacket packet) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(packetIds.get(packet.getClass()));
        packet.encode(buf);
        return buf;
    }

    public static void toServer(IPacket msg) {
        NetworkManager.sendToServer(PACKET_ID, encodePacket(msg));
    }

    public static void toClient(IPacket msg, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            NetworkManager.sendToPlayer((ServerPlayerEntity) player, PACKET_ID, encodePacket(msg));
        }
    }

    public static void toTrackingAndSelf(IPacket msg, Entity entity) {
        Collection<ServerPlayerEntity> trackingPlayers = EnvHandler.INSTANCE.getTrackingPlayers(entity);
        PacketBuffer buf = encodePacket(msg);
        if (!trackingPlayers.isEmpty()) {
            NetworkManager.sendToPlayers(trackingPlayers, PACKET_ID, buf);
        }
        if (entity instanceof ServerPlayerEntity) {
            NetworkManager.sendToPlayer((ServerPlayerEntity) entity, PACKET_ID, buf);
        }
    }

}
