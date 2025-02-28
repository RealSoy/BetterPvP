package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.fields.event.FieldsInteractableUseEvent;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.clans.fields.model.FieldsOre;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
public class MiningListener implements Listener, ConfigAccessor {

    private double xpMultiplier;
    private final Mining mining;

    @Inject
    public MiningListener(ProgressionAdapter adapter) {
        this.mining = adapter.getProgression().getInjector().getInstance(Mining.class);
    }

    @EventHandler
    public void onFieldsOreMine(FieldsInteractableUseEvent event) {
        final FieldsInteractable type = event.getType();
        if (!(type instanceof FieldsOre)) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getBlock().getBlock();
        mining.getMiningService().attemptMineOre(player, block, experience -> (long) (experience * xpMultiplier));
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.xpMultiplier = config.getOrSaveObject("fields.fieldsXpMultiplier", 5d, Double.class);
    }
}
