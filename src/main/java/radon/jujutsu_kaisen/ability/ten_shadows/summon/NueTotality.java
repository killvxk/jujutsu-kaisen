package radon.jujutsu_kaisen.ability.ten_shadows.summon;


import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.Summon;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.entity.registry.JJKEntities;
import radon.jujutsu_kaisen.entity.ten_shadows.NueTotalityEntity;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.List;

public class NueTotality extends Summon<NueTotalityEntity> {
    public NueTotality() {
        super(NueTotalityEntity.class);
    }

    @Override
    public boolean isScalable(LivingEntity owner) {
        return false;
    }

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        if (target == null || target.isDeadOrDying()) return false;

        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return false;

        IAbilityData data = cap.getAbilityData();

        if (data.hasToggled(this)) {
            return owner.level().getGameTime() % 20 != 0 || HelperMethods.RANDOM.nextInt(20) != 0;
        }
        return owner.level().getGameTime() % 20 == 0 && HelperMethods.RANDOM.nextInt(20) == 0;
    }

    @Override
    public boolean isTotality() {
        return true;
    }

    @Override
    public List<EntityType<?>> getFusions() {
        return List.of(JJKEntities.NUE.get(), JJKEntities.GREAT_SERPENT.get());
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0.2F;
    }

    @Override
    public List<EntityType<?>> getTypes() {
        return List.of(JJKEntities.NUE_TOTALITY.get());
    }

    @Override
    protected NueTotalityEntity summon(LivingEntity owner) {
        return new NueTotalityEntity(owner);
    }

    @Override
    public boolean isTenShadows() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }


}
