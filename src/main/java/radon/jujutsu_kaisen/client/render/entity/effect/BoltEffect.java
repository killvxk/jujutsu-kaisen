package radon.jujutsu_kaisen.client.render.entity.effect;


import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4f;

import java.util.*;

// https://github.com/mekanism/Mekanism/blob/1.20.x/src/main/java/mekanism/common/lib/effect/BoltEffect.java
public class BoltEffect {
    private final Random random = new Random();

    private final BoltRenderInfo renderInfo;

    private final Vec3 start;
    private final Vec3 end;

    private final int segments;

    private int count = 1;
    private float size = 0.1F;
    private int lifespan = 30;

    private SpawnFunction spawnFunction = SpawnFunction.delay(60);
    private FadeFunction fadeFunction = FadeFunction.fade(0.5F);

    public BoltEffect(Vec3 start, Vec3 end) {
        this(BoltRenderInfo.DEFAULT, start, end, (int) (Math.sqrt(start.distanceTo(end) * 100)));
    }

    public BoltEffect(BoltRenderInfo info, Vec3 start, Vec3 end, int segments) {
        this.renderInfo = info;
        this.start = start;
        this.end = end;
        this.segments = segments;
    }

    private static Vec3 findRandomOrthogonalVector(Vec3 vec, Random rand) {
        Vec3 newVec = new Vec3(-0.5 + rand.nextDouble(), -0.5 + rand.nextDouble(), -0.5 + rand.nextDouble());
        return vec.cross(newVec).normalize();
    }

    public BoltEffect count(int count) {
        this.count = count;
        return this;
    }

    public BoltEffect size(float size) {
        this.size = size;
        return this;
    }

    public BoltEffect spawn(SpawnFunction spawnFunction) {
        this.spawnFunction = spawnFunction;
        return this;
    }

    public BoltEffect fade(FadeFunction fadeFunction) {
        this.fadeFunction = fadeFunction;
        return this;
    }

    public BoltEffect lifespan(int lifespan) {
        this.lifespan = lifespan;
        return this;
    }

    public int getLifespan() {
        return this.lifespan;
    }

    public SpawnFunction getSpawnFunction() {
        return this.spawnFunction;
    }

    public FadeFunction getFadeFunction() {
        return this.fadeFunction;
    }

    public Vector4f getColor() {
        return this.renderInfo.color;
    }

    public List<BoltQuads> generate() {
        List<BoltQuads> quads = new ArrayList<>();
        Vec3 diff = this.end.subtract(this.start);
        float totalDistance = (float) diff.length();

        for (int i = 0; i < this.count; i++) {
            LinkedList<BoltInstructions> drawQueue = new LinkedList<>();
            drawQueue.add(new BoltInstructions(this.start, 0, new Vec3(0, 0, 0), null, false));

            while (!drawQueue.isEmpty()) {
                BoltInstructions data = drawQueue.poll();
                Vec3 perpendicularDist = data.perpendicularDist;
                float progress = data.progress + (1.0F / this.segments) * (1.0F - this.renderInfo.parallelNoise + this.random.nextFloat() * this.renderInfo.parallelNoise * 2);
                Vec3 segmentEnd;

                if (progress >= 1) {
                    segmentEnd = this.end;
                } else {
                    float segmentDiffScale = this.renderInfo.spreadFunction.getMaxSpread(progress);
                    float maxDiff = this.renderInfo.spreadFactor * segmentDiffScale * totalDistance * this.renderInfo.randomFunction.getRandom(this.random);
                    Vec3 randVec = findRandomOrthogonalVector(diff, this.random);
                    perpendicularDist = this.renderInfo.segmentSpreader.getSegmentAdd(perpendicularDist, randVec, maxDiff, segmentDiffScale, progress);
                    // New vector is original + current progress through segments + perpendicular change
                    segmentEnd = this.start.add(diff.scale(progress)).add(perpendicularDist);
                }
                float boltSize = this.size * (0.5F + (1 - progress) * 0.5F);
                Pair<BoltQuads, QuadCache> quadData = createQuads(data.cache, data.start, segmentEnd, boltSize);
                quads.add(quadData.getLeft());

                if (segmentEnd == this.end) {
                    break; // Break if we've reached the defined end point
                } else if (!data.isBranch) {
                    // Continue the bolt if this is the primary (non-branch) segment
                    drawQueue.add(new BoltInstructions(segmentEnd, progress, perpendicularDist, quadData.getRight(), false));
                } else if (this.random.nextFloat() < this.renderInfo.branchContinuationFactor) {
                    // Branch continuation
                    drawQueue.add(new BoltInstructions(segmentEnd, progress, perpendicularDist, quadData.getRight(), true));
                }

                while (this.random.nextFloat() < this.renderInfo.branchInitiationFactor * (1 - progress)) {
                    // Branch initiation (probability decreases as progress increases)
                    drawQueue.add(new BoltInstructions(segmentEnd, progress, perpendicularDist, quadData.getRight(), true));
                }
            }
        }
        return quads;
    }

    private Pair<BoltQuads, QuadCache> createQuads(QuadCache cache, Vec3 startPos, Vec3 end, float size) {
        Vec3 diff = end.subtract(startPos);
        Vec3 rightAdd = diff.cross(new Vec3(0.5D, 0.5D, 0.5D)).normalize().scale(size);
        Vec3 backAdd = diff.cross(rightAdd).normalize().scale(size), rightAddSplit = rightAdd.scale(0.5F);

        Vec3 start = cache != null ? cache.prevEnd : startPos;
        Vec3 startRight = cache != null ? cache.prevEndRight : start.add(rightAdd);
        Vec3 startBack = cache != null ? cache.prevEndBack : start.add(rightAddSplit).add(backAdd);
        Vec3 endRight = end.add(rightAdd), endBack = end.add(rightAddSplit).add(backAdd);

        BoltQuads quads = new BoltQuads();
        quads.addQuad(start, end, endRight, startRight);
        quads.addQuad(startRight, endRight, end, start);

        quads.addQuad(startRight, endRight, endBack, startBack);
        quads.addQuad(startBack, endBack, endRight, startRight);

        return Pair.of(quads, new QuadCache(end, endRight, endBack));
    }

    public interface SpreadFunction {
        /**
         * A steady linear increase in perpendicular noise.
         */
        SpreadFunction LINEAR_ASCENT = (progress) -> progress;

        /**
         * A steady linear increase in perpendicular noise, followed by a steady decrease after the halfway point.
         */
        SpreadFunction LINEAR_ASCENT_DESCENT = (progress) -> (progress - Math.max(0, 2 * progress - 1)) / 0.5F;

        /**
         * Represents a unit sine wave from 0 to PI, scaled by progress.
         */
        SpreadFunction SINE = (progress) -> Mth.sin((float) (Math.PI * progress));

        float getMaxSpread(float progress);
    }

    public interface RandomFunction {
        RandomFunction UNIFORM = Random::nextFloat;
        RandomFunction GAUSSIAN = rand -> (float) rand.nextGaussian();

        float getRandom(Random rand);
    }

    public interface SegmentSpreader {
        /**
         * Don't remember where the last segment left off, just randomly move from the straight-line vector.
         */
        SegmentSpreader NO_MEMORY = (perpendicularDist, randVec, maxDiff, scale, progress) -> randVec.scale(maxDiff);

        /**
         * Move from where the previous segment ended by a certain memory factor. Higher memory will restrict perpendicular movement.
         */
        static SegmentSpreader memory(float memoryFactor) {
            return (perpendicularDist, randVec, maxDiff, spreadScale, progress) -> {
                float nextDiff = maxDiff * (1 - memoryFactor);
                Vec3 cur = randVec.scale(nextDiff);

                if (progress > 0.5F) {
                    // Begin to come back to the center after we pass halfway mark
                    cur = cur.add(perpendicularDist.scale(-1 * (1 - spreadScale)));
                }
                return perpendicularDist.add(cur);
            };
        }

        Vec3 getSegmentAdd(Vec3 perpendicularDist, Vec3 randVec, float maxDiff, float scale, float progress);
    }

    public interface SpawnFunction {
        /**
         * Allow for bolts to be spawned each update call without any delay.
         */
        SpawnFunction NO_DELAY = (rand) -> Pair.of(0.0F, 0.0F);

        /**
         * Will re-spawn a bolt each time one expires.
         */
        SpawnFunction CONSECUTIVE = new SpawnFunction() {
            @Override
            public Pair<Float, Float> getSpawnDelayBounds(Random rand) {
                return Pair.of(0.0F, 0.0F);
            }

            @Override
            public boolean isConsecutive() {
                return true;
            }
        };

        /**
         * Spawn bolts with a specified constant delay.
         */
        static SpawnFunction delay(float delay) {
            return (rand) -> Pair.of(delay, delay);
        }

        /**
         * Spawns bolts with a specified delay and specified noise value, which will be randomly applied at either end of the delay bounds.
         */
        static SpawnFunction noise(float delay, float noise) {
            return (rand) -> Pair.of(delay - noise, delay + noise);
        }

        Pair<Float, Float> getSpawnDelayBounds(Random rand);

        default float getSpawnDelay(Random rand) {
            Pair<Float, Float> bounds = getSpawnDelayBounds(rand);
            return bounds.getLeft() + (bounds.getRight() - bounds.getLeft()) * rand.nextFloat();
        }

        default boolean isConsecutive() {
            return false;
        }
    }

    public interface FadeFunction {
        /**
         * No fade; render the bolts entirely throughout their lifespan.
         */
        FadeFunction NONE = (totalBolts, lifeScale) -> Pair.of(0, totalBolts);

        /**
         * Render bolts with a segment-by-segment 'fade' in and out, with a specified fade duration (applied to start and finish).
         */
        static FadeFunction fade(float fade) {
            return (totalBolts, lifeScale) -> {
                int start = lifeScale > (1 - fade) ? (int) (totalBolts * (lifeScale - (1 - fade)) / fade) : 0;
                int end = lifeScale < fade ? (int) (totalBolts * (lifeScale / fade)) : totalBolts;
                return Pair.of(start, end);
            };
        }

        Pair<Integer, Integer> getRenderBounds(int totalBolts, float lifeScale);
    }

    private record QuadCache(Vec3 prevEnd, Vec3 prevEndRight, Vec3 prevEndBack) {
    }

    protected static class BoltInstructions {
        private final Vec3 start;
        private final Vec3 perpendicularDist;
        private final QuadCache cache;
        private final float progress;
        private final boolean isBranch;

        private BoltInstructions(Vec3 start, float progress, Vec3 perpendicularDist, QuadCache cache, boolean isBranch) {
            this.start = start;
            this.perpendicularDist = perpendicularDist;
            this.progress = progress;
            this.cache = cache;
            this.isBranch = isBranch;
        }
    }

    public static class BoltQuads {
        private final List<Vec3> vecs = new ArrayList<>();

        protected void addQuad(Vec3... quadVecs) {
            vecs.addAll(Arrays.asList(quadVecs));
        }

        public List<Vec3> getVecs() {
            return vecs;
        }
    }

    public static class BoltRenderInfo {
        public static final BoltRenderInfo DEFAULT = new BoltRenderInfo();
        private final RandomFunction randomFunction = RandomFunction.GAUSSIAN;
        private final SpreadFunction spreadFunction = SpreadFunction.SINE;
        /**
         * How much variance is allowed in segment lengths (parallel to straight line).
         */
        private float parallelNoise = 0.1F;
        /**
         * How much variance is allowed perpendicular to the straight line vector. Scaled by distance and spread function.
         */
        private float spreadFactor = 0.1F;
        /**
         * The chance of creating an additional branch after a certain segment.
         */
        private float branchInitiationFactor = 0.0F;
        /**
         * The chance of a branch continuing (post-initiation).
         */
        private float branchContinuationFactor = 0.0F;
        private Vector4f color = new Vector4f(0.45F, 0.45F, 0.5F, 0.8F);
        private SegmentSpreader segmentSpreader = SegmentSpreader.NO_MEMORY;

        public BoltRenderInfo() {
        }

        public BoltRenderInfo(float parallelNoise, float spreadFactor, float branchInitiationFactor, float branchContinuationFactor, Vector4f color, float closeness) {
            this.parallelNoise = parallelNoise;
            this.spreadFactor = spreadFactor;
            this.branchInitiationFactor = branchInitiationFactor;
            this.branchContinuationFactor = branchContinuationFactor;
            this.color = color;
            this.segmentSpreader = SegmentSpreader.memory(closeness);
        }
    }
}