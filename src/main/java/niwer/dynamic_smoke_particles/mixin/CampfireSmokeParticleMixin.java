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
// Mth no longer required
import net.minecraft.world.entity.Entity;
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