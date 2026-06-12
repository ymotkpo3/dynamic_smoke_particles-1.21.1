package niwer.dynamic_smoke_particles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import niwer.dynamic_smoke_particles.compatibility.ClothConfigCompat;
import niwer.dynamic_smoke_particles.platform.Services;

public class FabricEngine extends Engine implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		this.init(FabricLoader.getInstance().getConfigDir());
	}

	/**
	 * Gets the config screen for the mod. Returns null if no compatible config mod is loaded.
	 * 
	 * @param parent The parent screen.
	 * @return The config screen or null if no compatible config mod is loaded.
	 */
	public static Screen getConfigScreen(Screen parent) {
		if(Services.PLATFORM.isModLoaded(Constants.CLOTH_CONFIG_ID)) return ClothConfigCompat.getConfigScreen(parent, config());
		return null;
	}
}