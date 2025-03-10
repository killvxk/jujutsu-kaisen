package radon.jujutsu_kaisen.world.gen.processor;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.block.CurseSpawnerBlock;
import radon.jujutsu_kaisen.block.JJKBlocks;
import radon.jujutsu_kaisen.data.mission.Mission;
import radon.jujutsu_kaisen.data.mission.MissionGrade;
import radon.jujutsu_kaisen.data.mission.MissionType;
import radon.jujutsu_kaisen.data.mission.level.IMissionLevelData;
import radon.jujutsu_kaisen.data.registry.JJKAttachmentTypes;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.List;
import java.util.Set;

public class MissionProcessor extends StructureProcessor {
    public static final MapCodec<MissionProcessor> CODEC = MapCodec.unit(MissionProcessor::new);

    @Override
    protected @NotNull StructureProcessorType<?> getType() {
        return JJKProcessors.MISSION_PROCESSOR.get();
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo process(@NotNull LevelReader pLevel, @NotNull BlockPos pOffset, @NotNull BlockPos pPos, StructureTemplate.@NotNull StructureBlockInfo pBlockInfo, StructureTemplate.@NotNull StructureBlockInfo pRelativeBlockInfo, @NotNull StructurePlaceSettings pSettings, @Nullable StructureTemplate template) {
        IMissionLevelData data = ((ServerLevelAccessor) pLevel).getLevel().getData(JJKAttachmentTypes.MISSION_LEVEL);

        if (pRelativeBlockInfo.state().getBlock() instanceof CurseSpawnerBlock) {
            RandomSource random = RandomSource.create(Mth.getSeed(pPos));

            if (!data.isRegistered(pPos)) data.register(HelperMethods.randomEnum(MissionType.class, random),
                    HelperMethods.randomEnum(MissionGrade.class, Set.of(MissionGrade.S), random), pPos);

            Mission mission = data.getMission(pPos);

            if (mission == null) return pRelativeBlockInfo;

            mission.addSpawn(pRelativeBlockInfo.pos());

            CompoundTag nbt = pRelativeBlockInfo.nbt() == null ? new CompoundTag() : pRelativeBlockInfo.nbt();
            nbt.put("pos", NbtUtils.writeBlockPos(pPos));
            pRelativeBlockInfo.state().setValue(CurseSpawnerBlock.IS_BOSS, pRelativeBlockInfo.state().is(JJKBlocks.CURSE_BOSS_SPAWNER));

            return new StructureTemplate.StructureBlockInfo(pRelativeBlockInfo.pos(), pRelativeBlockInfo.state(), nbt);
        }
        return pRelativeBlockInfo;
    }

    @Override
    public @NotNull List<StructureTemplate.StructureBlockInfo> finalizeProcessing(@NotNull ServerLevelAccessor pServerLevel, @NotNull BlockPos pOffset, @NotNull BlockPos pPos, @NotNull List<StructureTemplate.StructureBlockInfo> pOriginalBlockInfos, @NotNull List<StructureTemplate.StructureBlockInfo> pProcessedBlockInfos, @NotNull StructurePlaceSettings pSettings) {
        IMissionLevelData data = pServerLevel.getLevel().getData(JJKAttachmentTypes.MISSION_LEVEL);

        RandomSource random = RandomSource.create(Mth.getSeed(pPos));

        if (!data.isRegistered(pPos)) data.register(HelperMethods.randomEnum(MissionType.class, random),
                HelperMethods.randomEnum(MissionGrade.class, Set.of(MissionGrade.S), random), pPos);

        Mission mission = data.getMission(pPos);

        if (mission == null) return pProcessedBlockInfos;

        mission.setInitialized(true);

        for (StructureTemplate.StructureBlockInfo info : pProcessedBlockInfos) {
            if (!(info.state().getBlock() instanceof CurseSpawnerBlock)) continue;

            pServerLevel.scheduleTick(info.pos(), info.state().getBlock(), 0);
        }
        return pProcessedBlockInfos;
    }
}
