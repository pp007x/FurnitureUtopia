package furniture.furniture;

import org.bukkit.Location;

import java.util.List;

public class PlacedMultiBlockStructure {

    private final String name;
    private final Location baseLocation;
    private final List<MultiBlockLoader.MultiBlockStructure> blocks;

    public PlacedMultiBlockStructure(String name, Location baseLocation, List<MultiBlockLoader.MultiBlockStructure> blocks) {
        this.name = name;
        this.baseLocation = baseLocation;
        this.blocks = blocks;
    }

    public String getName() {
        return name;
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public List<MultiBlockLoader.MultiBlockStructure> getBlocks() {
        return blocks;
    }
}
