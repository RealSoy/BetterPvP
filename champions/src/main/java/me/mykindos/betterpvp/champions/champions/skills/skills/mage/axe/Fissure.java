package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.FissureBlock;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.FissureCast;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.FissurePath;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;


@Singleton
@BPvPListener
public class Fissure extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WorldBlockHandler blockHandler;
    private int fissureDistance;
    private int fissureDistanceIncreasePerLevel;
    private double fissureExpireDuration;
    private double damagePerBlock;
    private int effectDurationIncreasePerLevel;
    private double damagePerBlockIncreasePerLevel;
    private int effectDuration;
    private int slownessLevel;
    private List<String> forbiddenBlockTypes;

    private List<FissureCast> activeCasts = new ArrayList<>();

    @Inject
    public Fissure(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Fissure";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Fissure the earth infront of you,",
                "creating an impassable wall",
                "",
                "Players struck by wall will receive",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slownessLevel + 1) + "</effect> for <val>" + getSlowDuration(level) + "</val> seconds and take",
                "<val>" + (Math.round(getDamage(level) * 10) / 10.0) + "</val> damage for every block fissure",
                "has travelled",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int blocksTraveled, int level) {
        return (damagePerBlock + ((level - 1) * damagePerBlockIncreasePerLevel)) * blocksTraveled;
    }

    public double getDamage(int level) {
        return (damagePerBlock + ((level - 1) * damagePerBlockIncreasePerLevel));
    }

    public int getSlowDuration(int level) {
        return effectDuration + ((level - 1) * effectDurationIncreasePerLevel);
    }

    public int getFissureDistance(int level) {
        return fissureDistance + ((level - 1) * fissureDistanceIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isGrounded(player)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You can only use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    public void activate(Player player, int level) {
        activeCasts.add(createFissure(player, level));
    }


    public FissureCast createFissure(Player player, int level) {
        FissureCast fissureCast = new FissureCast(this, player, level, getFissureDistance(level));
        FissurePath fissurePath = new FissurePath();
        fissurePath.createPath(fissureCast);
        fissureCast.setFissurePath(fissurePath);

        if (!fissurePath.getFissureBlocks().isEmpty()) {
            for (FissureBlock fissureBlock : fissurePath.getFissureBlocks()) {
                Block block = fissureBlock.getBlock();

                player.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
                player.getWorld().playEffect(block.getLocation().add(0, 1, 0), Effect.STEP_SOUND, block.getType());

                for (LivingEntity entity : fissureBlock.getNearbyEntities()) {
                    if (entity.getLocation().getBlockY() == block.getY() + 1 || entity.getLocation().getBlock().equals(block)) {
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, getSlowDuration(level) * 20, slownessLevel));
                    }
                }
            }
        }

        return fissureCast;
    }

    @UpdateEvent
    public void processFissures() {
        activeCasts.removeIf(fissureCast -> {
            if (fissureCast.isFinished()) {
                return true;
            }

            fissureCast.process();
            return false;
        });
    }

    public void doBlockUpdate(FissureCast fissureCast, FissureBlock fissureBlock) {
        Block targetBlock = fissureBlock.getBlock();
        if (UtilBlock.airFoliage(targetBlock)) {
            Material materialToSet = fissureBlock.getMaterialToSet();
            blockHandler.addRestoreBlock(targetBlock, materialToSet, (long) (fissureExpireDuration * 1000));
            targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.STEP_SOUND, materialToSet);

            Location startLocation = fissureCast.getFissurePath().getStartLocation();
            Location blockLocation = fissureBlock.getBlock().getLocation();

            processCollision(fissureCast, fissureBlock, (int) UtilMath.offset2d(startLocation, blockLocation));
        }
    }

    private void processCollision(FissureCast fissureCast, FissureBlock fissureBlock, int distance) {

        for (LivingEntity livingEntity : fissureBlock.getNearbyEntities()) {
            if (fissureCast.getEntitiesHit().contains(livingEntity.getUniqueId())) continue;

            double damage = getDamage(distance, fissureCast.getLevel());
            UtilDamage.doCustomDamage(new CustomDamageEvent(livingEntity, fissureCast.getPlayer(), null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, "Fissure"));

            UtilMessage.simpleMessage(fissureCast.getPlayer(), getClassType().getName(), "You hit <alt2>" + livingEntity.getName() + "</alt2> with <alt>" + getName());
            UtilMessage.simpleMessage(livingEntity, getClassType().getName(), "<alt2>" + fissureCast.getPlayer().getName() + "</alt2> hit you with <alt>" + getName());

            fissureCast.getEntitiesHit().add(livingEntity.getUniqueId());
        }

    }

    public boolean isForbiddenBlockType(Block block) {
        if (block.getState() instanceof Container) {
            return true;
        }

        if (block.getBlockData() instanceof Openable || block.getBlockData() instanceof Directional) {
            return true;
        }

        if (!UtilBlock.solid(block)) {
            return true;
        }

        return forbiddenBlockTypes.stream().anyMatch(material -> block.getType().name().toLowerCase().contains(material.toLowerCase()));

    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        fissureDistance = getConfig("fissureDistance", 14, Integer.class);
        fissureDistanceIncreasePerLevel = getConfig("fissureDistanceIncreasePerLevel", 0, Integer.class);
        fissureExpireDuration = getConfig("fissureExpireDuration", 10.0, Double.class);
        damagePerBlock = getConfig("baseExtraDamagePerBlock", 0.6, Double.class);
        damagePerBlockIncreasePerLevel = getConfig("baseExtraDamagePerBlockIncreasePerLevel", 0.2, Double.class);
        effectDuration = getConfig("effectDuration", 1, Integer.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 1, Integer.class);
        slownessLevel = getConfig("slownessLevel", 1, Integer.class);

        var forbidden = List.of("TNT", "ENCHANTING_TABLE");
        forbiddenBlockTypes = getConfig("fissureForbiddenBlocks", forbidden, List.class);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
