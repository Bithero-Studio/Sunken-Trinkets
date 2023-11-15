package owmii.losttrinkets.item.trinkets;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import owmii.losttrinkets.api.LostTrinketsAPI;
import owmii.losttrinkets.api.trinket.Rarity;
import owmii.losttrinkets.api.trinket.Trinket;
import owmii.losttrinkets.api.trinket.Trinkets;
import owmii.losttrinkets.item.Itms;

import java.util.Iterator;

public class MagicalHerbsTrinket extends Trinket<MagicalHerbsTrinket> {
    public MagicalHerbsTrinket(Rarity rarity, Properties properties) {
        super(rarity, properties);
    }

    public static void onPotion(LivingEntity entity, Effect effect, Runnable denyResult) {
        if (entity instanceof PlayerEntity) {
            Trinkets trinkets = LostTrinketsAPI.getTrinkets((PlayerEntity) entity);
            if (trinkets.isActive(Itms.MAGICAL_HERBS)) {
                if (effect.getEffectType().equals(EffectType.HARMFUL) ||
                        effect.equals(Effects.BAD_OMEN)) {
                    denyResult.run();
                }
            }
        }
    }

    @Override
    public void onActivated(World world, BlockPos pos, PlayerEntity player) {
        if (world.isRemote) return;
        Iterator<EffectInstance> iterator = player.getActivePotionMap().values().iterator();
        while (iterator.hasNext()) {
            EffectInstance effect = iterator.next();
            if (effect.getPotion().getEffectType().equals(EffectType.HARMFUL) ||
                    effect.getPotion().equals(Effects.BAD_OMEN)) {
                player.onFinishedPotionEffect(effect);
                iterator.remove();
            }
        }
    }
}
