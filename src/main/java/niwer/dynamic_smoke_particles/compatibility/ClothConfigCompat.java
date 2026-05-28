package niwer.dynamic_smoke_particles.compatibility;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import niwer.dynamic_smoke_particles.config.Config;
import niwer.dynamic_smoke_particles.config.PerformanceProfile;

public class ClothConfigCompat {

    private ClothConfigCompat() {}

    public static Screen getConfigScreen(Screen parent, Config config) {
        final ConfigBuilder BUILDER = ConfigBuilder.create().setParentScreen(parent)
            .setTitle(Component.literal("Configuration for Dynamic Smoke Particles"))
            .setSavingRunnable(() -> config.saveConfig())
        ;
        final ConfigEntryBuilder ENTRY_BUILDER = BUILDER.entryBuilder();

        /* Global Category */
        {
            final ConfigCategory GLOBAL = BUILDER.getOrCreateCategory(Component.literal("Global Settings"));
            GLOBAL.addEntry(
                ENTRY_BUILDER.startBooleanToggle(Component.literal("Enabled"), config.isEnabled())
                    .setDefaultValue(true)
                    .setSaveConsumer(config::setEnabled)
                    .build()
            );

            GLOBAL.addEntry(
                ENTRY_BUILDER.startEnumSelector(Component.literal("Performance Profile"), PerformanceProfile.class, config.performanceProfile())
                    .setSaveConsumer(config::setPerformanceProfile)
                    .build()
            );
        }

        return BUILDER.build();
    }
}
