package niwer.dynamic_smoke_particles.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import niwer.dynamic_smoke_particles.platform.services.IPlatformHelper;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }
}