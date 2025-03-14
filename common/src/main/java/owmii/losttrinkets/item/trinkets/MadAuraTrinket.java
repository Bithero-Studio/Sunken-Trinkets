package owmii.losttrinkets.item.trinkets;

import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import owmii.losttrinkets.api.LostTrinketsAPI;
import owmii.losttrinkets.api.trinket.Rarity;
import owmii.losttrinkets.api.trinket.Trinket;
import owmii.losttrinkets.api.trinket.Trinkets;
import owmii.losttrinkets.item.Itms;

public class MadAuraTrinket extends Trinket<MadAuraTrinket> {
    public MadAuraTrinket(Rarity rarity, Settings properties) {
        super(rarity, properties);
    }

    public static void onAttack(LivingEntity entity, DamageSource source, Consumer<Boolean> setCanceled) {
        Entity immediateSource = source.getSource();
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            Trinkets trinkets = LostTrinketsAPI.getTrinkets(player);
            if (immediateSource instanceof PersistentProjectileEntity) {
                if (trinkets.isActive(Itms.MAD_AURA)) {
                    setCanceled.accept(true);
                }
            }
        }
    }
}
