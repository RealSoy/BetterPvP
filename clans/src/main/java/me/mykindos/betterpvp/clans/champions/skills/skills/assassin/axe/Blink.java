package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Blink extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Location> loc = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> blinkTime = new WeakHashMap<>();

    @Inject
    public Blink(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory, Clans clans1) {
        super(clans, championsManager, configFactory);
    }


    @Override
    public String getName() {
        return "Blink";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to activate.",
                "",
                "Instantly teleport forwards 15 Blocks.",
                "Cannot be used while Slowed.",
                "",
                "Using again within 5 seconds De-Blinks,",
                "returning you to your original location.",
                "Cannot be used while Slowed.",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @EventHandler
    public void onCustomDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.SUFFOCATION) return;
        if (event.getDamager() == null) return;
        if (!(event.getDamagee() instanceof Player player)) return;

        if (blinkTime.containsKey(player)) {
            if (!UtilTime.elapsed(blinkTime.get(player), 500)) {
                deblink(player, true);
            }

        }
    }

    @UpdateEvent(delay = 100)
    public void onDetectGlass() {
        for (Player player : blinkTime.keySet()) {
            if (UtilTime.elapsed(blinkTime.get(player), 250)) continue;
            if (isInInvalidBlock(player)) {
                deblink(player, true);
            }

        }
    }

    private boolean isInInvalidBlock(Player player) {
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Location loc = new Location(player.getWorld(), Math.floor(player.getLocation().getX() + x),
                        player.getLocation().getY(), Math.floor(player.getLocation().getZ() + z));

                if (loc.getBlock().getType().name().contains("GLASS") || loc.getBlock().getType().name().contains("DOOR")) {
                    return true;

                }
            }
        }

        return false;
    }

    public void deblink(Player player, boolean force) {
        UtilServer.runTaskLater(clans, () -> {
            if (!championsManager.getCooldowns().isCooling(player, "Deblink") || force) {

                if (!force) {
                    UtilMessage.message(player, getClassType().getName(), "You used " + ChatColor.GREEN + "Deblink" + " " + getLevel(player) + ChatColor.GRAY + ".");
                } else {
                    UtilMessage.message(player, getClassType().getName(), "The target location was invalid, Blink cooldown has been reduced.");
                    championsManager.getCooldowns().removeCooldown(player, "Blink", true);
                    championsManager.getCooldowns().add(player, "Blink", 2, true);
                }

                Block lastSmoke = player.getLocation().getBlock();

                double curRange = 0.0D;
                Location target = this.loc.remove(player);

                boolean done = false;
                while (!done) {
                    Vector vec = UtilVelocity.getTrajectory(player.getLocation(),
                            new Location(player.getWorld(), target.getX(), target.getY(), target.getZ()));

                    Location newTarget = player.getLocation().add(vec.multiply(curRange));


                    curRange += 0.2D;


                    lastSmoke.getWorld().playEffect(lastSmoke.getLocation(), Effect.SMOKE, 4);
                    lastSmoke = newTarget.getBlock();

                    if (UtilMath.offset(newTarget, target) < 0.4D) {
                        done = true;
                    }
                    if (curRange > 24.0D) {
                        done = true;
                    }
                }

                player.teleport(target);
                player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0, 15);
            }
        }, 1);

    }

    @Override
    public boolean canUse(Player player) {


        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            UtilMessage.message(player, getClassType().getName(), "You cannot use " + getName() + " while Slowed.");
            return false;
        }

        if (championsManager.getEffects().hasEffect(player, EffectType.STUN)) {
            UtilMessage.message(player, getClassType().getName(), "You cannot use " + ChatColor.GREEN
                    + getName() + ChatColor.GRAY + " while stunned.");
            return false;
        }

        if ((loc.containsKey(player)) && (blinkTime.containsKey(player))
                && (!UtilTime.elapsed(blinkTime.get(player), 4000L))) {
            deblink(player, false);
            return false;
        }


        return true;
    }


    @Override
    public double getCooldown(int level) {

        return getSkillConfig().getCooldown() - ((level - 1));
    }


    @Override
    public void activate(Player player, int level) {

        // Run this later as teleporting during the InteractEvent was causing it to trigger twice
        UtilServer.runTaskLater(clans, () -> {
            Vector direction = player.getLocation().getDirection();
            Location targetLocation = player.getLocation().add(0, 1, 0);

            double maxDistance = 16;

            for (double currentDistance = 0; currentDistance < maxDistance; currentDistance += 1) {
                Location testLocation = targetLocation.clone().add(direction.clone());
                Block testBlock = testLocation.getBlock();
                if (!UtilBlock.isWall(testBlock)) {
                    targetLocation = testLocation;
                    player.getWorld().playEffect(targetLocation, Effect.SMOKE, 4);

                    if (!UtilPlayer.getNearbyPlayers(player, targetLocation, 0.5D).isEmpty()) {
                        break;
                    }
                } else {

                    break;
                }
            }

            blinkTime.put(player, System.currentTimeMillis());
            loc.put(player, player.getLocation());

            Location finalLocation = targetLocation.add(direction.clone().multiply(-1));
            player.leaveVehicle();
            player.teleport(finalLocation);

            championsManager.getCooldowns().add(player, "Deblink", 0.25, false);
            player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
        }, 1);


    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
