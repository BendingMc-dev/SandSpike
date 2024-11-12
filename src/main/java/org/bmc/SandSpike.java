package org.bmc;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SandSpike extends SandAbility implements AddonAbility {
    private long cooldown;
    private int range, sourceRange, maxHeight;
    private Block sourceBlock;
    private Material sourceBlockMaterial;
    private Location location;
    private boolean isProgressing;

    private Permission perm;
    private SandSpikeListener listener;

    public SandSpike(Player player) {
        super(player);

        // Initialize configuration values
        this.cooldown = ConfigManager.getConfig().getInt("ExtraAbilities.Bera.SandSpike.Cooldown");
        this.range = ConfigManager.getConfig().getInt("ExtraAbilities.Bera.SandSpike.Range");
        this.sourceRange = ConfigManager.getConfig().getInt("ExtraAbilities.Bera.SandSpike.SourceRange");
        this.maxHeight = ConfigManager.getConfig().getInt("ExtraAbilities.Bera.SandSpike.MaxHeight");

        if (!bPlayer.canBend(this)) {
            System.out.println("Player can't bend SandSpike!");
            return;
        }

        if (prepare()) {
            start();
        }
    }

    public void removeNonProgressingInstances(Player player) {
        Collection<SandSpike> sandSpikeInstances = getAbilities(player, SandSpike.class);
        for (SandSpike sandSpike : sandSpikeInstances) {
            if (!sandSpike.isProgressing()) {
                sandSpike.remove();
            }
        }
    }

    public boolean prepare() {
        removeNonProgressingInstances(player);
        Block block = getEarthSourceBlock(sourceRange);

        if (block == null || !isEarthbendable(block)) {
            System.out.println("Unable to source block: " + (block == null ? "null" : "not sandbendable"));
            return false;
        }

        if (block.getLocation().distanceSquared(player.getLocation()) > sourceRange * sourceRange) {
            System.out.println("Source block is not in range");
            return false;
        }

        sourceBlock = block;
        selectSource();
        return true;
    }

    public void selectSource() {
        sourceBlockMaterial = sourceBlock.getType();

        if (sourceBlockMaterial == Material.SAND) {
            sourceBlock.setType(Material.SANDSTONE);
        } else if (sourceBlockMaterial == Material.RED_SAND) {
            sourceBlock.setType(Material.RED_SANDSTONE);
        } else if (sourceBlockMaterial == Material.STONE) {
            sourceBlock.setType(Material.COBBLESTONE);
        } else {
            sourceBlock.setType(Material.STONE);
        }

        location = sourceBlock.getLocation();
    }

    public void unselectSource() {
        sourceBlock.setType(sourceBlockMaterial);
    }

    public void setProgressing(boolean isProgressing) {
        this.isProgressing = isProgressing;
    }

    public boolean isProgressing() {
        return isProgressing;
    }

    public static void shootSpikes(Player player) {
        List<SandSpike> sandSpikeInstances = new ArrayList<>(getAbilities(player, SandSpike.class));
        for (SandSpike sandSpike : sandSpikeInstances) {
            sandSpike.setProgressing(true);
            sandSpike.unselectSource();
        }
    }

    @Override
    public void progress() {
        if (!isProgressing()) {
            return;
        }

        double distance = location.distance(sourceBlock.getLocation());

        if (distance > range) {
            System.out.println("Out of range (" + range + "), removing");
            remove();
            return;
        }

        int height = (int) Math.ceil((double) maxHeight / range * distance);
        createSpikeAtLocation(location.clone(), height);

        Location destination = GeneralMethods.getTargetedLocation(player, range);
        Vector direction = new Vector(destination.getX() - sourceBlock.getX(), 0, destination.getZ() - sourceBlock.getZ());
        location.add(direction.normalize());
    }

    public void createSpikeAtLocation(Location location, int height) {
        Vector velocity = new Vector(0, 0.25, 0);
        boolean canExpire = true;

        int amountBase = height >= 3 ? 1 : 0;
        int amountMiddle = height >= 4 ? height - 3 : 0;
        int amountFrustum = height >= 2 ? 1 : 0;
        int amountTop = 1;

        // Base
        for (int i = 0; i < amountBase; i++) {
            PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
            blockData.setThickness(PointedDripstone.Thickness.BASE);
            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), blockData, velocity, this, canExpire);
        }

        // Middle
        for (int i = 0; i < amountMiddle; i++) {
            PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
            blockData.setThickness(PointedDripstone.Thickness.MIDDLE);
            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), blockData, velocity, this, canExpire);
        }

        // Frustum
        for (int i = 0; i < amountFrustum; i++) {
            PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
            blockData.setThickness(PointedDripstone.Thickness.FRUSTUM);
            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), blockData, velocity, this, canExpire);
        }

        // Top
        PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
        blockData.setThickness(PointedDripstone.Thickness.TIP);
        new TempFallingBlock(location.add(0, amountTop, 0), blockData, velocity, this, canExpire);
    }


    @Override
    public void remove() {
        unselectSource();
        super.remove();
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "SandSpike";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void load() {
        listener = new SandSpikeListener();
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
        perm = new Permission("bending.ability.SandSpike", PermissionDefault.OP);
        ProjectKorra.plugin.getServer().getPluginManager().addPermission(perm);

        ConfigManager.getConfig().addDefault("ExtraAbilities.Bera.SandSpike.Range", 15);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Bera.SandSpike.Cooldown", 5000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Bera.SandSpike.SourceRange", 10);
        ConfigManager.getConfig().addDefault("ExtraAbilities.Bera.SandSpike.MaxHeight", 3);
        ConfigManager.defaultConfig.save();

        ProjectKorra.plugin.getLogger().info(getName() + " " + getVersion() + " by " + getAuthor() + " has been successfully enabled.");
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(listener);
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(perm);
    }

    @Override
    public String getInstructions() {
        return "Sneak at an earthbendable block to select a source, then Left-click to fire the spikes, hold sneak to control";
    }

    @Override
    public String getDescription() {
        return "Solidify the sands to create a swift blinding attack.";
    }

    @Override
    public String getAuthor() {
        return "BeraTR & ShadowTP";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
