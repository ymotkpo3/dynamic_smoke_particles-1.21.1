package niwer.dynamic_smoke_particles;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod(Constants.MOD_ID)
public class NeoForgeEngine extends Engine {

    public NeoForgeEngine(IEventBus eventBus) {
        this.init(FMLLoader.getCurrent().getGameDir());
    }
}