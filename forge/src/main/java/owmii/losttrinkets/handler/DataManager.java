package owmii.losttrinkets.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import owmii.losttrinkets.LostTrinkets;
import owmii.losttrinkets.api.LostTrinketsAPI;
import owmii.losttrinkets.api.player.PlayerData;
import owmii.losttrinkets.api.trinket.Trinket;
import owmii.losttrinkets.api.trinket.Trinkets;
import owmii.losttrinkets.forge.LostTrinketsForge;
import owmii.losttrinkets.network.packet.SyncDataPacket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DataManager implements ICapabilitySerializable<CompoundNBT> {
    private final PlayerData data = new PlayerData();
    private final LazyOptional<PlayerData> holder = LazyOptional.of(() -> this.data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == LostTrinketsForge.PLAYERDATA_CAP) {
            return this.holder.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return this.data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.data.deserializeNBT(nbt);
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(PlayerData.class, new Capability.IStorage<PlayerData>() {
            @Nullable
            @Override
            public INBT writeNBT(Capability<PlayerData> capability, PlayerData instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<PlayerData> capability, PlayerData instance, Direction side, INBT nbt) {

            }
        }, PlayerData::new);
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(new ResourceLocation(LostTrinkets.MOD_ID, "player_data"), new DataManager());
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        PlayerEntity oldPlayer = event.getOriginal();
        PlayerEntity player = event.getPlayer();
        PlayerData oldData = LostTrinketsAPI.getData(oldPlayer);
        PlayerData newData = LostTrinketsAPI.getData(player);
        newData.deserializeNBT(oldData.serializeNBT());

        Trinkets trinkets = LostTrinketsAPI.getTrinkets(player);
        trinkets.getActiveTrinkets().forEach(trinket -> {
            if (trinket instanceof Trinket) {
                ((Trinket) trinket).applyAttributes(player);
            }
        });
        if (!event.isWasDeath()) {
            // fix player health
            player.setHealth(oldPlayer.getHealth());
        }
    }

    @SubscribeEvent
    public static void update(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            PlayerData data = LostTrinketsAPI.getData(player);
            if (data.isSync()) {
                LostTrinketsForge.NET.toTrackingAndSelf(new SyncDataPacket(player), player);
                data.setSync(false);
            }
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Trinkets trinkets = LostTrinketsAPI.getTrinkets(event.getPlayer());
        trinkets.initSlots(LostTrinkets.config().startSlots);
        trinkets.removeDisabled(event.getPlayer());
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();
        PlayerData data = LostTrinketsAPI.getData(player);
        data.wasFlying = player.abilities.isFlying;
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void trackPlayer(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        // When a player starts tracking another player entity, sync the target to them
        if (target instanceof ServerPlayerEntity) {
            LostTrinketsForge.NET.toClient(new SyncDataPacket((ServerPlayerEntity) target), event.getPlayer());
        }
    }

    static void sync(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            LostTrinketsForge.NET.toClient(new SyncDataPacket(player), player);
        }
    }
}
