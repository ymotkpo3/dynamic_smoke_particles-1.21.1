package niwer.dynamic_smoke_particles;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Compatibility class for ModMenu.
 * Provides a config screen factory that returns the config screen for the mod.
 * 
 * If no compatible config mod is loaded, returns null.
 * 
 * @author Niwer
 */
public class ModMenuCompat implements ModMenuApi {

    @Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return Engine::getConfigScreen;
	}
}
