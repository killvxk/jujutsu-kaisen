package radon.jujutsu_kaisen.event;


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.VeilHandler;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.AbilityHandler;
import radon.jujutsu_kaisen.ability.AbilityTriggerEvent;
import radon.jujutsu_kaisen.ability.event.LivingInsideDomainEvent;
import radon.jujutsu_kaisen.ability.misc.Barrage;
import radon.jujutsu_kaisen.ability.registry.JJKAbilities;
import radon.jujutsu_kaisen.chant.ServerChantHandler;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.chant.IChantData;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.entity.ISorcerer;
import radon.jujutsu_kaisen.entity.sorcerer.HeianSukunaEntity;
import radon.jujutsu_kaisen.util.EntityUtil;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;

public class MobEventHandler {
    @EventBusSubscriber(modid = JujutsuKaisen.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onLivingInsideDomain(LivingInsideDomainEvent event) {
            LivingEntity victim = event.getEntity();

            if (victim instanceof ISorcerer && victim instanceof Mob mob && mob.canAttack(event.getAttacker()))
                mob.setTarget(event.getAttacker());
        }

        @SubscribeEvent
        public static void onMobSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            if (event.getSpawnType() == MobSpawnType.SPAWN_EGG) return;

            Mob mob = event.getEntity();

            if (!VeilHandler.canSpawn(level, mob, event.getX(), event.getY(), event.getZ())) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLivingHurtEvent(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            if (!(victim instanceof ISorcerer)) return;

            IJujutsuCapability cap = victim.getCapability(JujutsuCapabilityHandler.INSTANCE);

            if (cap == null) return;

            IAbilityData data = cap.getAbilityData();

            if (!data.hasToggled(JJKAbilities.CURSED_ENERGY_FLOW.get())) {
                AbilityHandler.trigger(victim, JJKAbilities.CURSED_ENERGY_FLOW.get());
            }

            int chance = 4 - victim.level().getDifficulty().getId();

            if (!data.isChanneling(JJKAbilities.CURSED_ENERGY_SHIELD.get())) {
                if (HelperMethods.RANDOM.nextInt(chance) == 0) {
                    AbilityHandler.trigger(victim, JJKAbilities.CURSED_ENERGY_SHIELD.get());
                }
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            DamageSource source = event.getSource();

            if (!(source.getEntity() instanceof LivingEntity attacker)) return;

            if (victim == attacker) return;

            // Checks to prevent tamed creatures from attacking their owners and owners from attacking their tames
            if (attacker instanceof TamableAnimal tamable1 && attacker instanceof ISorcerer) {
                LivingEntity owner1 = EntityUtil.getOwner(tamable1);

                if (owner1 == victim) {
                    event.setCanceled(true);
                } else if (victim instanceof TamableAnimal tamable2 && victim instanceof ISorcerer) {
                    LivingEntity owner2 = EntityUtil.getOwner(tamable2);

                    // Prevent tames with the same owner from attacking each other
                    if (owner1 == owner2) {
                        event.setCanceled(true);
                    }
                }
            } else if (victim instanceof TamableAnimal tamable && victim instanceof ISorcerer) {
                LivingEntity owner = EntityUtil.getOwner(tamable);

                if (owner == attacker) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onAbilityTrigger(AbilityTriggerEvent.Pre event) {
            Ability ability = event.getAbility();

            LivingEntity owner = event.getEntity();

            IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

            if (cap == null) return;

            ISorcererData sorcererData = cap.getSorcererData();
            IChantData chantData = cap.getChantData();

            // Sukuna has multiple arms
            if (owner instanceof HeianSukunaEntity entity && ability == JJKAbilities.BARRAGE.get()) {
                entity.setBarrage(Barrage.DURATION * 2);
            }

            // Making mobs use chants
            /*if (owner.level() instanceof ServerLevel level) {
                if (owner instanceof Mob) {
                    List<String> chants = new ArrayList<>(chantData.getFirstChants(ability));

                    if (!chants.isEmpty() && (ability == JJKAbilities.WORLD_SLASH.get() ||
                            HelperMethods.RANDOM.nextInt(Math.max(1, (int) (50 * sorcererData.getMaximumOutput()))) == 0)) {
                        for (int i = 0; i < HelperMethods.RANDOM.nextInt(chants.size()); i++) {
                            ServerChantHandler.onChant(owner, chants.get(i));

                            for (ServerPlayer player : level.players()) {
                                if (player.distanceTo(owner) > owner.getAttributeValue(Attributes.FOLLOW_RANGE))
                                    continue;

                                player.sendSystemMessage(Component.literal(String.format("<%s> %s", owner.getName().getString(), chants.get(i))));
                            }
                        }
                    }
                }
            }*/
        }
    }
}