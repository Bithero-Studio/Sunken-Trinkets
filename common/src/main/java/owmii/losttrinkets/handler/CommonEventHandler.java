package owmii.losttrinkets.handler;

import java.util.concurrent.atomic.AtomicReference;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.ExplosionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import owmii.losttrinkets.api.LostTrinketsAPI;
import owmii.losttrinkets.api.player.PlayerData;
import owmii.losttrinkets.api.trinket.Trinkets;
import owmii.losttrinkets.item.Itms;
import owmii.losttrinkets.item.trinkets.BigFootTrinket;
import owmii.losttrinkets.item.trinkets.BlazeHeartTrinket;
import owmii.losttrinkets.item.trinkets.DarkDaggerTrinket;
import owmii.losttrinkets.item.trinkets.DarkEggTrinket;
import owmii.losttrinkets.item.trinkets.DropSpindleTrinket;
import owmii.losttrinkets.item.trinkets.EmberTrinket;
import owmii.losttrinkets.item.trinkets.GoldenSwatterTrinket;
import owmii.losttrinkets.item.trinkets.MadAuraTrinket;
import owmii.losttrinkets.item.trinkets.MadPiggyTrinket;
import owmii.losttrinkets.item.trinkets.MirrorShardTrinket;
import owmii.losttrinkets.item.trinkets.OctopickTrinket;
import owmii.losttrinkets.item.trinkets.OctopusLegTrinket;
import owmii.losttrinkets.item.trinkets.RubyHeartTrinket;
import owmii.losttrinkets.item.trinkets.SerpentToothTrinket;
import owmii.losttrinkets.item.trinkets.SlingshotTrinket;
import owmii.losttrinkets.item.trinkets.StarfishTrinket;
import owmii.losttrinkets.item.trinkets.WitherNailTrinket;

public class CommonEventHandler {

    public static void register() {
        TickEvent.PLAYER_POST.register((player) -> {
            PlayerData data = LostTrinketsAPI.getData(player);
            if (data.unlockDelay > 0) {
                data.unlockDelay--;
            }
            Trinkets trinkets = LostTrinketsAPI.getTrinkets(player);
            trinkets.getTickable().forEach(trinket -> trinket.tick(player.world, player.getBlockPos(), player));

            if (player instanceof ServerPlayerEntity) {
                UnlockHandler.tickPlayerOnServer(player);
            }
        });
        TickEvent.SERVER_PRE.register((server) -> {
            RubyHeartTrinket.saveHealthTickStart(server);
        });
        // onLivingUpdate is handled by fabric:LivingEntityMixin.tick() forge:LivingUpdateEvent
        ExplosionEvent.PRE.register((world, explosion) -> {
            Entity entity = explosion.entity;
            if (entity instanceof CreeperEntity) {
                CreeperEntity creeper = ((CreeperEntity) entity);
                LivingEntity target = creeper.getTarget();
                if (target instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) target;
                    Trinkets trinkets = LostTrinketsAPI.getTrinkets(player);
                    if (trinkets.isActive(Itms.CREEPO)) {
                        creeper.playSpawnEffects();
                        return EventResult.interruptFalse();
                    }
                }
            }
            return EventResult.pass();
        });
        EntityEvent.ADD.register((entity, world) -> {
            AtomicReference<EventResult> result = new AtomicReference<>(EventResult.pass());
            OctopickTrinket.collectDrops(entity, (cancel) -> {
                result.set(cancel ? EventResult.interruptFalse() : EventResult.pass());
            });
            BigFootTrinket.addAvoidGoal(entity);
            return result.get();
        });
        // Forge's LivingAttackEvent
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (source == null) return EventResult.pass();

            AtomicReference<EventResult> result = new AtomicReference<>(EventResult.pass());
            if (BlazeHeartTrinket.isImmuneToFire(entity, source)) {
                result.set(EventResult.interruptFalse());
            }
            MadAuraTrinket.onAttack(entity, source, (cancel) -> {
                result.set(cancel ? EventResult.interruptFalse() : EventResult.pass());
            });
            OctopusLegTrinket.onAttack(entity, source);
            return result.get();
        });
        // saveHealthHurt is handed by fabric:PlayerEntityMixin.applyDamage() / forge:LivingHurtEvent
        // onHurt is handled by fabric:LivingEntityMixin.applyDamage() / fabric:PlayerEntityMixin.applyDamage() / forge:LivingHurtEvent
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (source == null) return EventResult.pass();
            AtomicReference<EventResult> result = new AtomicReference<>(EventResult.pass());
            RubyHeartTrinket.onDeath(source, entity, (cancel) -> {
                result.set(cancel ? EventResult.interruptFalse() : EventResult.pass());
            });
            return result.get();
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            UnlockHandler.kill(source, entity);
            return EventResult.pass();
        });
        // onDrops is handled by fabric:LivingEntityMixin.drop() / forge:LivingDropsEvent
        // onPotion is handled by fabric:LivingEntityMixin.canHaveStatusEffect() / forge:PotionApplicableEvent
        // onCriticalHit is handled by fabric:PlayerEntityMixin.modifyCriticalHitFlag() / forge:CriticalHitEvent
        // onLooting is handled by fabric:EnchantmentHelperMixin.getLooting() / forge:LootingLevelEvent
        // onUseFinish fabric:LivingEntityMixin.consumeItem() / forge:LivingEntityUseItemEvent.Finish
        // onBreakSpeed is handled by fabric:PlayerEntityMixin.getBlockBreakingSpeed() / forge:PlayerEvent.BreakSpeed
        BlockEvent.BREAK.register((world, pos, state, player, xp) -> {
            AtomicReference<EventResult> result = new AtomicReference<>(EventResult.pass());
            OctopickTrinket.onBreak(player, pos, state, (cancel) -> {
                result.set(cancel ? EventResult.interruptFalse() : EventResult.pass());
            });
            return result.get();
        });
        // onEnderTeleport is handled by fabric:EndermanEntityMixin.teleportTo() / fabric:ShulkerEntityMixin.tryTeleport() / forge:EntityTeleportEvent.EnderEntity
        // setTarget is handled by fabric:MobEntityMixin.setTarget() / forge:LivingSetAttackTargetEvent
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            DataManager.clone(oldPlayer, newPlayer, !wonGame);
        });
        PlayerEvent.PLAYER_JOIN.register((player) -> DataManager.loggedIn(player));
        PlayerEvent.PLAYER_QUIT.register((player) -> DataManager.loggedOut(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> DataManager.sync(player));
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> DataManager.sync(player));
        // trackPlayer is handled by fabric:EntityTrackingEvents.START_TRACKING / forge:PlayerEvent.StartTracking

        LifecycleEvent.SERVER_BEFORE_START.register((server) -> {
            UnlockManager.refresh();
        });
    }

    public static float onHurt(DamageSource source, LivingEntity entityLiving, float amount) {
        if (source == null) {
            return amount;
        }

        DarkDaggerTrinket.onHurt(source, amount, entityLiving);
        DarkEggTrinket.onHurt(entityLiving, source);
        DropSpindleTrinket.onHurt(source);
        EmberTrinket.onHurt(entityLiving, source);
        GoldenSwatterTrinket.onHurt(entityLiving, source);
        MadPiggyTrinket.onHurt(entityLiving, source);
        MirrorShardTrinket.onHurt(entityLiving, source, amount);
        SerpentToothTrinket.onHurt(source, entityLiving);
        StarfishTrinket.onHurt(source, amount);
        SlingshotTrinket.onHurt(source, entityLiving);
        WitherNailTrinket.onHurt(source, entityLiving);

        if (source.getAttacker() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) source.getAttacker();
            Trinkets trinkets = LostTrinketsAPI.getTrinkets(player);
            if (trinkets.isActive(Itms.SILVER_NAIL)) {
                amount *= 1.1F;
            }
            if (trinkets.isActive(Itms.GLORY_SHARDS)) {
                amount *= 1.2F;
            }
        }

        return amount;
    }

}
