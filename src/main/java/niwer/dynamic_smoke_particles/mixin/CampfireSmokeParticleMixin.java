package niwer.dynamic_smoke_particles.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
// Mth no longer required
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import niwer.dynamic_smoke_particles.Engine;

@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin extends SingleQuadParticle {

	@Unique private static final List<VoxelShape> EMPTY_LIST = List.of();
	@Unique private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = 10000.0D; // 100^2
	@Unique private static final double COLLISION_HORIZONTAL_BOOST = 1.02D;
	@Unique private static final double COLLISION_HORIZONTAL_PUSH = 0.01D;
	@Unique private static final double MAX_HORIZONTAL_SPEED = 0.05D;
	@Unique private static final double EPS_MIN_MOVEMENT = 1.0E-7D;
	@Unique private static final double EPS_MOTION = 1.0E-5D;
	@Unique private static final double[] ESCAPE_DIRECTION_X = { 1.0D, -1.0D, 0.0D, 0.0D, 0.7071067811865476D, 0.7071067811865476D, -0.7071067811865476D, -0.7071067811865476D };
	@Unique private static final double[] ESCAPE_DIRECTION_Z = { 0.0D, 0.0D, 1.0D, -1.0D, 0.7071067811865476D, -0.7071067811865476D, 0.7071067811865476D, -0.7071067811865476D };
	@Unique private static final int ESCAPE_DIRECTION_STEPS = 4;
	@Unique private static final int ESCAPE_DIRECTION_VERTICAL_SAMPLES = 4;
	@Unique private boolean preferredEscapeDirectionReady;
	@Unique private double preferredEscapeDirectionX;
	@Unique private double preferredEscapeDirectionZ;
	@Unique private boolean stoppedByCollision;

	private CampfireSmokeParticleMixin(final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final boolean isSignalFire, final TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
	}

	@Inject(at = @At("HEAD"), method = "tick", cancellable = true)
	private void onTick(CallbackInfo info) {
		if (!Engine.config().isEnabled()) return; // If the mod is disabled, skip the custom tick logic and use the original behavior

		/* If the mod isn't disabled, cancel the Minecraft code and reset the state */
		info.cancel();
		this.stoppedByCollision = false;

		/* Handle previous positions */
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ < this.lifetime && this.alpha > 0.0F) {
			final var RND = this.random;

			/* Small random horizontal jitter */
			this.xd += (RND.nextBoolean() ? 1.0D : -1.0D) * (RND.nextFloat() * 0.0002D); // 1/5000 = 0.0002
			this.zd += (RND.nextBoolean() ? 1.0D : -1.0D) * (RND.nextFloat() * 0.0002D);
			this.yd -= this.gravity;

			final double PREV_Y = this.y;
			final double REQUESTED_Y_MOTION = this.yd;
			this.move(this.xd, this.yd, this.zd);

			if (REQUESTED_Y_MOTION != 0.0D && Math.abs(this.y - PREV_Y) < EPS_MIN_MOVEMENT) {
				switch (Engine.config().performanceProfile()) {
					case SIMPLE -> this.applySimpleVerticalStallResponse(RND);
					case COMPLEX -> this.applyComplexVerticalStallResponse(RND);
				}
			}

			if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) this.alpha -= 0.015F;
		} else this.remove();
	}

	@Unique
	public void move(double xa, double ya, double za) {
		if (this.stoppedByCollision) return;

		double originalXa = xa;
		double originalYa = ya;
		double originalZa = za;

		if (this.hasPhysics && (xa != 0.0D || ya != 0.0D || za != 0.0D) && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			final var AABB = this.getBoundingBox();
			Vec3 movement = Entity.collideBoundingBox(null, new Vec3(xa, ya, za), AABB, this.level, EMPTY_LIST);
			xa = movement.x;
			ya = movement.y;
			za = movement.z;
		}

		if (xa != 0.0D || ya != 0.0D || za != 0.0D) {
			this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
			this.setLocationFromBoundingbox();
		}

		if (Math.abs(originalYa) >= EPS_MOTION && Math.abs(ya) < EPS_MOTION) this.stoppedByCollision = true;

		this.onGround = originalYa != ya && originalYa < 0.0D;
		if (originalXa != xa) this.xd = 0.0D;
		if (originalZa != za) this.zd = 0.0D;
	}

	@Unique
	private void applySimpleVerticalStallResponse(RandomSource RND) {
		this.xd *= COLLISION_HORIZONTAL_BOOST;
		this.zd *= COLLISION_HORIZONTAL_BOOST;

		final double PUSH_X = this.xd == 0.0D ? (RND.nextBoolean() ? COLLISION_HORIZONTAL_PUSH : -COLLISION_HORIZONTAL_PUSH) : Math.copySign(COLLISION_HORIZONTAL_PUSH, this.xd);
		final double PUSH_Z = this.zd == 0.0D ? (RND.nextBoolean() ? COLLISION_HORIZONTAL_PUSH : -COLLISION_HORIZONTAL_PUSH) : Math.copySign(COLLISION_HORIZONTAL_PUSH, this.zd);
		this.xd += PUSH_X;
		this.zd += PUSH_Z;

		final double HORIZONTAL_SPEED_SQUARED = this.xd * this.xd + this.zd * this.zd;
		final double MAX_HORIZONTAL_SPEED_SQUARED = MAX_HORIZONTAL_SPEED * MAX_HORIZONTAL_SPEED;
		if (HORIZONTAL_SPEED_SQUARED > MAX_HORIZONTAL_SPEED_SQUARED) {
			final double SCALE = MAX_HORIZONTAL_SPEED / Math.sqrt(HORIZONTAL_SPEED_SQUARED);
			this.xd *= SCALE;
			this.zd *= SCALE;
		}

		this.moveHorizontally(PUSH_X, PUSH_Z);
	}

	@Unique
	private void applyComplexVerticalStallResponse(RandomSource RND) {
		this.ensurePreferredEscapeDirection();

		final double ESCAPE_X = this.preferredEscapeDirectionReady ? this.preferredEscapeDirectionX : (this.xd == 0.0D ? (RND.nextBoolean() ? 1.0D : -1.0D) : Math.copySign(1.0D, this.xd));
		final double ESCAPE_Z = this.preferredEscapeDirectionReady ? this.preferredEscapeDirectionZ : (this.zd == 0.0D ? (RND.nextBoolean() ? 1.0D : -1.0D) : Math.copySign(1.0D, this.zd));
		final double PUSH_X = ESCAPE_X * COLLISION_HORIZONTAL_PUSH;
		final double PUSH_Z = ESCAPE_Z * COLLISION_HORIZONTAL_PUSH;

		if (this.preferredEscapeDirectionReady) {
			final double HORIZONTAL_SPEED = Math.max(Math.sqrt(this.xd * this.xd + this.zd * this.zd), COLLISION_HORIZONTAL_PUSH);
			this.xd = ESCAPE_X * HORIZONTAL_SPEED * COLLISION_HORIZONTAL_BOOST;
			this.zd = ESCAPE_Z * HORIZONTAL_SPEED * COLLISION_HORIZONTAL_BOOST;
		} else {
			this.xd *= COLLISION_HORIZONTAL_BOOST;
			this.zd *= COLLISION_HORIZONTAL_BOOST;
			this.xd += PUSH_X;
			this.zd += PUSH_Z;
		}

		final double HORIZONTAL_SPEED_SQUARED = this.xd * this.xd + this.zd * this.zd;
		final double MAX_HORIZONTAL_SPEED_SQUARED = MAX_HORIZONTAL_SPEED * MAX_HORIZONTAL_SPEED;
		if (HORIZONTAL_SPEED_SQUARED > MAX_HORIZONTAL_SPEED_SQUARED) {
			final double SCALE = MAX_HORIZONTAL_SPEED / Math.sqrt(HORIZONTAL_SPEED_SQUARED);
			this.xd *= SCALE;
			this.zd *= SCALE;
		}

		this.moveHorizontally(PUSH_X, PUSH_Z);
	}

	@Unique
	private void ensurePreferredEscapeDirection() {
		if (this.preferredEscapeDirectionReady) return;

		double bestScore = Double.NEGATIVE_INFINITY;
		double bestX = 0.0D;
		double bestZ = 0.0D;

		for (int index = 0; index < ESCAPE_DIRECTION_X.length; index++) {
			double candidateX = ESCAPE_DIRECTION_X[index];
			double candidateZ = ESCAPE_DIRECTION_Z[index];
			double score = this.scoreEscapeDirection(candidateX, candidateZ);

			if (score > bestScore) {
				bestScore = score;
				bestX = candidateX;
				bestZ = candidateZ;
			}
		}

		if (bestScore > Double.NEGATIVE_INFINITY) {
			this.preferredEscapeDirectionX = bestX;
			this.preferredEscapeDirectionZ = bestZ;
			this.preferredEscapeDirectionReady = true;
		}
	}

	@Unique
	private double scoreEscapeDirection(double directionX, double directionZ) {
		double score = 0.0D;
		final BlockPos.MutableBlockPos CHECK_POS = new BlockPos.MutableBlockPos();
		final int BASE_Y = (int) Math.floor(this.y);

		for (int step = 1; step <= ESCAPE_DIRECTION_STEPS; step++) {
			final int SAMPLE_X = (int) Math.floor(this.x + directionX * step);
			final int SAMPLE_Z = (int) Math.floor(this.z + directionZ * step);

			for (int vertical = 0; vertical < ESCAPE_DIRECTION_VERTICAL_SAMPLES; vertical++) {
				final int SAMPLE_Y = BASE_Y + vertical;
				CHECK_POS.set(SAMPLE_X, SAMPLE_Y, SAMPLE_Z);
				
				final var BLOCK_STATE = this.level.getBlockState(CHECK_POS);
				if(BLOCK_STATE.getBlock() instanceof TrapDoorBlock) {
					boolean isOpen = BLOCK_STATE.getValue(TrapDoorBlock.OPEN);

					score += isOpen ? 3.0D : 0.0D; // Open trapdoors are excellent escape points, as they don't have a collision box and can be easily escaped through, while closed trapdoors don't provide any escape benefit
				} else if(BLOCK_STATE.getBlock() instanceof SlabBlock) {
					score += 2.5D; // Slabs are good escape points, as they don't have a full collision box and can be easily escaped through
				} else if(BLOCK_STATE.getBlock() instanceof StairBlock) {
					score += 2.0D; // Stairs are very good escape points, as they don't have a full collision box and can be easily escaped through
				} else if (BLOCK_STATE.getCollisionShape(this.level, CHECK_POS).isEmpty()) {
					score += vertical == 0 ? 1.5D : 1.0D;
				} else if(!BLOCK_STATE.isCollisionShapeFullBlock(level, CHECK_POS)) {
					score += vertical == 0 ? 1.0D : 0.5D; // Blocks with low collision boxes (like bottom slabs or carpet) are somewhat good escape points, as they can be escaped through with a bit of vertical movement
				}
				else {
					score -= 3.0D;
					return score;
				}
			}

			score += 0.25D;
		}

		return score;
	}

	@Unique
	public void moveHorizontally(double xa, double za) {
		double originalXa = xa;
		double originalZa = za;

		if (this.hasPhysics && (xa != 0.0D || za != 0.0D) && xa * xa + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			final var AABB = this.getBoundingBox();
			Vec3 movement = Entity.collideBoundingBox(null, new Vec3(xa, 0.0D, za), AABB, this.level, EMPTY_LIST);
			xa = movement.x;
			za = movement.z;
		}

		if (xa != 0.0D || za != 0.0D) {
			this.setBoundingBox(this.getBoundingBox().move(xa, 0.0D, za));
			this.setLocationFromBoundingbox();
		}

		if (originalXa != xa) this.xd = 0.0D;
		if (originalZa != za) this.zd = 0.0D;
	}
}