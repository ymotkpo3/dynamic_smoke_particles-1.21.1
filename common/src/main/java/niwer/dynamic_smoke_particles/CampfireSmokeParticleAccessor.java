package niwer.dynamic_smoke_particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;

public interface CampfireSmokeParticleAccessor {

    ClientLevel level();
    
    boolean hasPhysics();

    AABB getAABB();

    void setAABB(AABB boundingBox);

    void setLocationFromAABB();

    double x();

    double y();

    double z();

    double xd();

    void setXd(double value);

    double yd();

    void setYd(double value);

    double zd();

    void setZd(double value);

    boolean isStoppedByCollision();

    void setStoppedByCollision(boolean value);

    boolean isPreferredEscapeDirectionReady();

    void setPreferredEscapeDirectionReady(boolean value);

    double getPreferredEscapeDirectionX();

    void setPreferredEscapeDirectionX(double value);

    double getPreferredEscapeDirectionZ();
    
    void setPreferredEscapeDirectionZ(double value);
}
