package radon.jujutsu_kaisen.entity.ai.goal;


import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.entity.sorcerer.HeianSukunaEntity;
import radon.jujutsu_kaisen.entity.sorcerer.SukunaEntity;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class NearestAttackableCurseGoal extends TargetGoal {
    private static final int DEFAULT_RANDOM_INTERVAL = 10;
    protected final int randomInterval;

    @Nullable
    protected LivingEntity target;

    protected TargetingConditions targetConditions;

    public NearestAttackableCurseGoal(Mob pMob, boolean pMustSee) {
        this(pMob, DEFAULT_RANDOM_INTERVAL, pMustSee, false, null);
    }

    public NearestAttackableCurseGoal(Mob pMob, boolean pMustSee, Predicate<LivingEntity> pTargetPredicate) {
        this(pMob, DEFAULT_RANDOM_INTERVAL, pMustSee, false, pTargetPredicate);
    }

    public NearestAttackableCurseGoal(Mob pMob, boolean pMustSee, boolean pMustReach) {
        this(pMob, DEFAULT_RANDOM_INTERVAL, pMustSee, pMustReach, null);
    }

    public NearestAttackableCurseGoal(Mob pMob, int pRandomInterval, boolean pMustSee, boolean pMustReach, @Nullable Predicate<LivingEntity> pTargetPredicate) {
        super(pMob, pMustSee, pMustReach);

        this.randomInterval = reducedTickDelay(pRandomInterval);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditions = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(pTargetPredicate);
    }

    public boolean canUse() {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        } else {
            this.findTarget();
            return this.target != null;
        }
    }

    protected AABB getTargetSearchArea(double pTargetDistance) {
        return this.mob.getBoundingBox().inflate(pTargetDistance, 4.0D, pTargetDistance);
    }

    protected void findTarget() {
        this.target = this.mob.level().getNearestEntity(this.mob.level().getEntitiesOfClass(LivingEntity.class, this.getTargetSearchArea(this.getFollowDistance()), entity -> {
            if (!(entity instanceof TamableAnimal tamable) || !tamable.isTame()) {
                IJujutsuCapability cap = entity.getCapability(JujutsuCapabilityHandler.INSTANCE);

                if (cap == null) return false;

                ISorcererData data = cap.getSorcererData();
                return entity instanceof SukunaEntity || entity instanceof HeianSukunaEntity || data.getType() == JujutsuType.CURSE;
            }
            return false;
        }), this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
    }

    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    public void setTarget(@Nullable LivingEntity pTarget) {
        this.target = pTarget;
    }
}