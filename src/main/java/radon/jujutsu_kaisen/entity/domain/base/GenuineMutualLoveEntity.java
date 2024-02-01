package radon.jujutsu_kaisen.entity.domain.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.cursed_technique.JJKCursedTechniques;
import radon.jujutsu_kaisen.capability.data.sorcerer.cursed_technique.base.ICursedTechnique;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.MimicryKatanaEntity;

import java.util.*;

public class GenuineMutualLoveEntity extends ClosedDomainExpansionEntity {
    public GenuineMutualLoveEntity(EntityType<?> pType, Level pLevel) {
        super(pType, pLevel);
    }

    public GenuineMutualLoveEntity(LivingEntity owner, DomainExpansion ability, int radius) {
        super(JJKEntities.GENUINE_MUTUAL_LOVE.get(), owner, ability, radius);
    }

    @Override
    public void tick() {
        super.tick();

        int radius = this.getRadius();

        if (this.getTime() == radius * 2) {
            List<BlockPos> floor = this.getFloor();

            LivingEntity owner = this.getOwner();

            if (owner == null || floor.isEmpty()) return;

            ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

            Set<ICursedTechnique> copied = cap.getCopied();

            if (copied.isEmpty()) return;

            int total = floor.size() / 8;
            int share = total / copied.size();

            List<ICursedTechnique> all = new ArrayList<>();

            for (ICursedTechnique technique : copied) {
                all.addAll(Collections.nCopies(share, technique));
            }

            Iterator<ICursedTechnique> iter = all.iterator();

            while (iter.hasNext() && !floor.isEmpty()) {
                ICursedTechnique technique = iter.next();

                BlockPos pos = floor.get(this.random.nextInt(floor.size()));
                this.level().addFreshEntity(new MimicryKatanaEntity(this, technique, pos.getCenter().add(0.0D, 0.5D, 0.0D)));

                floor.remove(pos);
                iter.remove();
            }
        }
    }
}