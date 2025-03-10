package radon.jujutsu_kaisen.client.render.entity.effect;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.entity.effect.BlackFlashEntity;
import radon.jujutsu_kaisen.util.HelperMethods;

public class BlackFlashRenderer extends EntityRenderer<BlackFlashEntity> {
    private static final double RANGE = 3.0D;
    private static final double OFFSET = 3.0D;

    private final BoltRenderer renderer;

    public BlackFlashRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.renderer = new BoltRenderer();
    }

    @Override
    public void render(@NotNull BlackFlashEntity pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.0F, pEntity.getBbHeight() / 2, 0.0F);

        pPoseStack.translate(-pEntity.getX(), -pEntity.getY(), -pEntity.getZ());

        Vec3 start = new Vec3(pEntity.getX(), pEntity.getY(), pEntity.getZ());
        Vec3 end = start.add(pEntity.getStart().subtract(start).scale(RANGE))
                .add(
                        OFFSET * (HelperMethods.RANDOM.nextDouble() - 0.5D),
                        OFFSET * (HelperMethods.RANDOM.nextDouble() - 0.5D),
                        OFFSET * (HelperMethods.RANDOM.nextDouble() - 0.5D)
                );
        BoltEffect.BoltRenderInfo info = new BoltEffect.BoltRenderInfo(0.0F, 0.075F, 0.0F, 0.0F,
                new Vector4f(ParticleColors.BLACK_FLASH.x, ParticleColors.BLACK_FLASH.y, ParticleColors.BLACK_FLASH.z, 0.8F), 1.8F);
        BoltEffect bolt = new BoltEffect(info, start, end, (int) (Math.sqrt(start.distanceTo(end))) * 10)
                .size(0.1F)
                .lifespan(1)
                .fade(BoltEffect.FadeFunction.NONE)
                .spawn(BoltEffect.SpawnFunction.CONSECUTIVE);
        this.renderer.update(null, bolt, pPartialTick);
        this.renderer.render(pPartialTick, pPoseStack, pBuffer);

        pPoseStack.popPose();
    }

    @Override
    protected int getBlockLightLevel(@NotNull BlackFlashEntity pEntity, @NotNull BlockPos pPos) {
        return 15;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BlackFlashEntity pEntity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
