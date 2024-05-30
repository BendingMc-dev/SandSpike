package org.bmc;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempFallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SandSpike extends SandAbility implements AddonAbility {
    private long cooldown;
    private int range;
    private int sourceRange;
    private Block sourceBlock;
    private Material sourceBlockMaterial;
    private Location location;
    private boolean isProgressing;
    private int maxHeight;

    public SandSpike(Player player) {
        super(player);

        boolean canBend = this.bPlayer.canBend(this);

        if (!canBend) {
            System.out.println("Player can't bend SandSpike!");
            return;
        }

        this.setFields();

        System.out.println("Checking if source block can be selected...");

        if (this.prepare()) {
            this.start();
        }
    }

    public void setFields() {
        this.range = ConfigManager.getConfig().getInt(Config.RANGE.getPath());
        this.cooldown = ConfigManager.getConfig().getInt(Config.COOLDOWN.getPath());
        this.sourceRange = ConfigManager.getConfig().getInt(Config.SOURCE_RANGE.getPath());
        this.maxHeight = ConfigManager.getConfig().getInt(Config.MAX_HEIGHT.getPath());
        this.isProgressing = false;

        System.out.println("Setting fields:");
        System.out.println("\tRange: " + this.range);
        System.out.println("\tCooldown: " + this.cooldown);
        System.out.println("\tMax Height: " + this.maxHeight);
        System.out.println("\tSource Range: " + this.sourceRange);
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
//        Block block = BlockSource.getEarthSourceBlock(this.player, sourceRange, ClickType.SHIFT_DOWN);
        boolean sourceBlockIsBendable = block != null && this.isSandbendable(block);

        if (!sourceBlockIsBendable) {
            System.out.println("Unable to source block:");
            if (block == null) {
                System.out.println("\tSource block is null!");
            } else {
                System.out.println("\tSource is not sandbendable!");
            }
            return false;
        }

        System.out.println("Source block info:");
        System.out.println("\tLocation: " + block.getLocation().getX() + " | " + block.getLocation().getY() + " | " + block.getLocation().getZ());
        System.out.println("\tMaterial: " + block.getType().name());

        int sourceRangeSquared = this.sourceRange * this.sourceRange;
        boolean sourceBlockIsInRange = block.getLocation().distanceSquared(player.getLocation()) <= sourceRangeSquared;

        if (!sourceBlockIsInRange) {
            System.out.println("\tSource is not in range");
            return false;
        }

        this.sourceBlock = block;
        this.selectSource();

        return true;
    }

    public void selectSource() {
//        if (DensityShift.isPassiveSand(this.sourceBlock)) {
//            DensityShift.revertSand(this.sourceBlock);
//        }

        Material sourceBlockType = this.sourceBlock.getType();
        this.sourceBlockMaterial = sourceBlockType;

        System.out.println("Selected source:");
        System.out.println("\tLocation: " + sourceBlock.getLocation().getX() + " | " + sourceBlock.getLocation().getY() + " | " + sourceBlock.getLocation().getZ());
        System.out.println("\tMaterial: " + sourceBlockType.name());

        switch(sourceBlockType) {
            case SAND:
                this.sourceBlock.setType(Material.SANDSTONE);
                break;
            case RED_SAND:
                this.sourceBlock.setType(Material.RED_SANDSTONE);
                break;
            case STONE:
                this.sourceBlock.setType(Material.COBBLESTONE);
                break;
            default:
                this.sourceBlock.setType(Material.STONE);
                break;
        }

//        this.damage = applyMetalPowerFactor(this.damage, this.sourceBlock);

        this.location = this.sourceBlock.getLocation();
    }

    public void unselectSource() {
        this.sourceBlock.setType(this.sourceBlockMaterial);
    }

    public void setProgressing(boolean isProgressing) {
        this.isProgressing = isProgressing;
    }

    public boolean isProgressing() {
        return this.isProgressing;
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
        if (!this.isProgressing()) {
            return;
        }

        double distance = this.location.distance(this.sourceBlock.getLocation());
//        boolean locationIsAGap = (Math.ceil(this.location.getX()) % 2 == 0) && (Math.ceil(this.location.getZ()) % 2 == 0);

//        if (locationIsAGap) {
            int height = (int) Math.ceil((((double) this.maxHeight / this.range) * distance));
            createSpikeAtLocation(this.location.clone(), height);
//        }

        boolean abilityIsInRange = distance <= this.range;

        if (!abilityIsInRange) {
            System.out.println("Out of range (" + this.range + "), removing");
            this.remove();
            return;
        }

        Block originBlock = this.sourceBlock;
        Location destination = GeneralMethods.getTargetedLocation(this.player, this.range);
        Vector direction = new Vector(destination.getX() - originBlock.getX(), 0, destination.getZ() - originBlock.getZ());

        this.location.add(direction.normalize());
    }

    public List<Location> getLocationsAroundPoint(Location location) {
        return null;
    }

    public void createSpikeAtLocation(Location location, int height) {
        System.out.println("Creating spike of height: " + height);

        Vector fallingBlockVelocity = new Vector(0, 0.25, 0);
        boolean fallingBlockCanExpire = true;
        int amountBase = height >= 3 ? 1 : 0;
        int amountMiddle = height >= 4 ? height - 3 : 0;
        int amountFrustum = height >= 2 ? 1 : 0;
        int amountTop = 1; //height == 1 ? 0 : 1;

        // base
        for (int i = 0; i < amountBase; i++) {
            PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
            blockData.setThickness(PointedDripstone.Thickness.BASE);
            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), blockData, fallingBlockVelocity, this, fallingBlockCanExpire);

//            new TempFallingBlock(location.add(0, 1, 0), Material.POINTED_DRIPSTONE.createBlockData(), fallingBlockVelocity, this, fallingBlockCanExpire);
        }

        // middle
        for (int i = 0; i < amountMiddle; i++) {
            PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
            blockData.setThickness(PointedDripstone.Thickness.MIDDLE);
            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), blockData, fallingBlockVelocity, this, fallingBlockCanExpire);

//            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), Material.POINTED_DRIPSTONE.createBlockData(), fallingBlockVelocity, this, fallingBlockCanExpire);
        }

        // frustum
        for (int i = 0; i < amountFrustum; i++) {
            PointedDripstone blockData = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
            blockData.setThickness(PointedDripstone.Thickness.FRUSTUM);
            TempFallingBlock tempFallingBlock = new TempFallingBlock(location.add(0, 1, 0), blockData, fallingBlockVelocity, this, fallingBlockCanExpire);
        }

        // top
//        Location topLocation = location.add(0, 1, 0);
        new TempFallingBlock(location.add(0, amountTop, 0), Material.POINTED_DRIPSTONE.createBlockData(), fallingBlockVelocity, this, fallingBlockCanExpire);
    }

    // todo: test for distance -> if distance only ever increases, good, otherwise, look into implementing something like that
    // todo: region protection

    @Override
    public void remove() {
        this.sourceBlock.setType(this.sourceBlockMaterial);
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
        return this.cooldown;
    }

    @Override
    public String getName() {
        return "SandSpike";
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public void load() {
        ConfigManager.getConfig().addDefault(Config.RANGE.getPath(), 20);
        ConfigManager.getConfig().addDefault(Config.COOLDOWN.getPath(), 5000);
        ConfigManager.getConfig().addDefault(Config.SOURCE_RANGE.getPath(), 10);
        ConfigManager.getConfig().addDefault(Config.MAX_HEIGHT.getPath(), 6);
        ConfigManager.defaultConfig.save();

        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new AbilityListener(), ProjectKorra.plugin);
        ProjectKorra.plugin.getServer().getPluginManager().addPermission(new Permission("bending.ability.sandspike"));

        ProjectKorra.plugin.getLogger().info(this.getName() + " " + this.getVersion() + " by " + this.getAuthor() + " has been successfully enabled.");
    }

    @Override
    public void stop() {

    }

    @Override
    public String getAuthor() {
        return "Bera";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
