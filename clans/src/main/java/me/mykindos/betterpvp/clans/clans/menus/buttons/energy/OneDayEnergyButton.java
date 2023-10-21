package me.mykindos.betterpvp.clans.clans.menus.buttons.energy;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;

public class OneDayEnergyButton extends BuyEnergyButton {

    private final Clan clan;

    public OneDayEnergyButton(int slot, Clan clan) {
        super(slot, clan);
        this.clan = clan;
        double energyPerDay = clan.getEnergyRatio() * 24;

        name = Component.text("Buy 1 Day of Energy", NamedTextColor.GREEN);

        lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("Buy <green>%s <gray>energy for <yellow>$%s",
                NumberFormat.getInstance().format(energyPerDay), NumberFormat.getInstance().format(energyPerDay * 5.0)));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<gray>Left-click to buy 1 day of energy"));

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(new ItemStack(Material.EMERALD), name, lore)).clone();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            buyEnergy(player, (int) clan.getEnergyRatio() * 24, (int) (clan.getEnergyRatio() * 24 * 5.0));
        }
    }
}
