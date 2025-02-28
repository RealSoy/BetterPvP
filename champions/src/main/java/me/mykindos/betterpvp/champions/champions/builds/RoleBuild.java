package me.mykindos.betterpvp.champions.champions.builds;

import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;

@Data
public class RoleBuild {

    private final String uuid;
    private final Role role;
    private final int id;

    private boolean active;
    private BuildSkill swordSkill;
    private BuildSkill axeSkill;
    private BuildSkill passiveA, passiveB, global;
    private BuildSkill bow;
    private int points = 12;

    public void addPoint() {
        points++;
    }

    public void takePoint() {
        points--;
    }

    public void takePoints(int points) {
        this.points -= points;
    }

    public ArrayList<Skill> getActiveSkills() {
        ArrayList<Skill> skills = new ArrayList<>();
        if (swordSkill != null) {
            skills.add(swordSkill.getSkill());
        }
        if (axeSkill != null) {
            skills.add(axeSkill.getSkill());
        }
        if (getBow() != null) {
            skills.add(getBow().getSkill());
        }
        if (getPassiveA() != null) {
            skills.add(getPassiveA().getSkill());
        }
        if (getPassiveB() != null) {
            skills.add(getPassiveB().getSkill());
        }
        if (getGlobal() != null) {
            skills.add(getGlobal().getSkill());
        }
        return skills;
    }

    public BuildSkill getBuildSkill(SkillType type) {
        return switch (type) {
            case SWORD -> getSwordSkill();
            case AXE -> getAxeSkill();
            case PASSIVE_A -> getPassiveA();
            case BOW -> getBow();
            case GLOBAL -> getGlobal();
            case PASSIVE_B -> getPassiveB();
        };

    }


    public void setSkill(SkillType type, BuildSkill skill) {
        switch (type) {
            case SWORD -> setSwordSkill(skill);
            case AXE -> setAxeSkill(skill);
            case PASSIVE_A -> setPassiveA(skill);
            case BOW -> setBow(skill);
            case GLOBAL -> setGlobal(skill);
            case PASSIVE_B -> setPassiveB(skill);
        }
    }

    public void setSkill(SkillType type, Skill skill, int level) {
        setSkill(type, new BuildSkill(skill, level));
    }

    public void deleteBuild() {
        swordSkill = null;
        axeSkill = null;
        passiveA = null;
        passiveB = null;
        global = null;
        bow = null;
        points = 12;

    }

    /**
     * @return The component representation of a build
     */
    public Component getBuildComponent() {
        String sword = getSwordSkill() == null ? "" : getSwordSkill().getString();
        String axe = getAxeSkill() == null ? "" : getAxeSkill().getString();
        String bow = getBow() == null ? "" : getBow().getString();
        String passivea = getPassiveA() == null ? "" : getPassiveA().getString();
        String passiveb = getPassiveB() == null ? "" : getPassiveB().getString();
        String global = getGlobal() == null ? "" : getGlobal().getString();

        Component component =  Component.text("Sword: ", NamedTextColor.GREEN).append(Component.text(sword, NamedTextColor.WHITE)).appendNewline()
                .append(Component.text("Axe: ", NamedTextColor.GREEN).append(Component.text(axe, NamedTextColor.WHITE))).appendNewline()
                .append(Component.text("Bow: ", NamedTextColor.GREEN).append(Component.text(bow, NamedTextColor.WHITE))).appendNewline()
                .append(Component.text("Passive A: ", NamedTextColor.GREEN).append(Component.text(passivea, NamedTextColor.WHITE))).appendNewline()
                .append(Component.text("Passive B: ", NamedTextColor.GREEN).append(Component.text(passiveb, NamedTextColor.WHITE))).appendNewline()
                .append(Component.text("Global: ", NamedTextColor.GREEN).append(Component.text(global, NamedTextColor.WHITE)));
        return component;
    }

}
