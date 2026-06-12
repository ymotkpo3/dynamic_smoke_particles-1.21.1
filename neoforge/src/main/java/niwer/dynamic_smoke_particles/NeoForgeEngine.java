package niwer.dynamic_smoke_particles;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import niwer.dynamic_smoke_particles.compatibility.ClothConfigCompat;
import niwer.dynamic_smoke_particles.platform.Services;

@Mod(Constants.MOD_ID)
public class NeoForgeEngine extends Engine {

    public NeoForgeEngine(IEventBus eventBus) {
        this.init(FMLLoader.getCurrent().getGameDir());
        if(Services.PLATFORM.isModLoaded(Constants.CLOTH_CONFIG_ID)) registerConfigScreen();
    }
    
    private void registerConfigScreen() {
        ModList.get().getModContainerById(Constants.MOD_ID).ifPresent(container -> {
            container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> ClothConfigCompat.getConfigScreen(parent, config()));
        });
    }
}