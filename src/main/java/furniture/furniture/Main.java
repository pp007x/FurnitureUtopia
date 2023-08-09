package furniture.furniture;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    private MultiBlockLoader multiBlockLoader;
    private final Map<String, List<PlacedMultiBlockStructure>> placedStructures = new HashMap<>();
    private final Set<UUID> builderModePlayers = new HashSet<>(); // Set to keep track of players in building mode
    private final Map<UUID, BossBar> builderModeBars = new HashMap<>();
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!builderModePlayers.contains(player.getUniqueId())) return; // Return if player is not in building mode

        Block brokenBlock = event.getBlock();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, List<PlacedMultiBlockStructure>> entry : placedStructures.entrySet()) {
            List<PlacedMultiBlockStructure> placedStructuresList = entry.getValue();
            for (PlacedMultiBlockStructure placedStructure : placedStructuresList) {
                for (MultiBlockLoader.MultiBlockStructure block : placedStructure.getBlocks()) {
                    Location structureBlockLocation = placedStructure.getBaseLocation().clone().add(block.getRelativePosition());
                    if (structureBlockLocation.getBlock().equals(brokenBlock)) {
                        toRemove.add(placedStructure.getBaseLocation().toString());
                        break;
                    }
                }
            }
        }

        // Added this to remove all blocks in the structure when a single block is broken
        for (String locationKey : toRemove) {
            removeStructure(locationKey);
        }
    }


    private void removeStructure(String locationKey) {
        for (Map.Entry<String, List<PlacedMultiBlockStructure>> entry : new HashMap<>(placedStructures).entrySet()) {
            List<PlacedMultiBlockStructure> structuresAtLocation = entry.getValue().stream()
                    .filter(s -> s.getBaseLocation().toString().equals(locationKey))
                    .collect(Collectors.toList());

            for (PlacedMultiBlockStructure placedStructure : structuresAtLocation) {
                for (MultiBlockLoader.MultiBlockStructure block : placedStructure.getBlocks()) {
                    Location structureBlockLocation = placedStructure.getBaseLocation().clone().add(block.getRelativePosition());
                    structureBlockLocation.getBlock().setType(Material.AIR);
                }
                entry.getValue().remove(placedStructure);
            }
        }
        placedStructures.values().removeIf(List::isEmpty);
    }

    @Override
    public void onEnable() {
        File structuresFile = new File(getDataFolder(), "structures.yml");
        if (!structuresFile.exists()) {
            saveResource("structures.yml", false);
        }

        multiBlockLoader = new MultiBlockLoader(getDataFolder());
        getServer().getPluginManager().registerEvents(this, this);

        GiveStructureCommand giveStructureCommand = new GiveStructureCommand();
        getCommand("giveStructure").setExecutor(giveStructureCommand);
        getCommand("giveStructure").setTabCompleter(giveStructureCommand);

        getCommand("removeStructure").setExecutor(new RemoveStructureCommand());

        // Registering the new command
        getCommand("buildmode").setExecutor(new BuildingModeCommand());

        // Removed the first BossBar bossBar declaration here
        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bossBar = Bukkit.createBossBar("Builder Mode", BarColor.BLUE, BarStyle.SOLID);
            bossBar.setVisible(false);
            builderModeBars.put(player.getUniqueId(), bossBar);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BossBar bossBar = Bukkit.createBossBar("Builder Mode", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(false);
        builderModeBars.put(player.getUniqueId(), bossBar);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!builderModePlayers.contains(player.getUniqueId())) return; // Return if player is not in building mode

        // Check if the interaction is a right-click action
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) {
                clickedBlock = player.getTargetBlockExact(5); // Adjust maxDistance as needed
                if (clickedBlock == null) return; // Return if the player is not looking at any block (might be looking at the sky or too far away)
            }

            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, List<PlacedMultiBlockStructure>> entry : placedStructures.entrySet()) {
                List<PlacedMultiBlockStructure> placedStructuresList = entry.getValue();
                for (PlacedMultiBlockStructure placedStructure : placedStructuresList) {
                    for (MultiBlockLoader.MultiBlockStructure block : placedStructure.getBlocks()) {
                        Location structureBlockLocation = placedStructure.getBaseLocation().clone().add(block.getRelativePosition());
                        if (structureBlockLocation.getBlock().equals(clickedBlock)) {
                            toRemove.add(placedStructure.getBaseLocation().toString());
                            break;
                        }
                    }
                }
            }

            // Added this to remove all blocks in the structure when a single block is clicked
            for (String locationKey : toRemove) {
                removeStructure(locationKey);
            }

            // Check if the player is holding a structure item
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                ItemMeta itemMeta = itemInHand.getItemMeta();
                if (itemMeta != null && itemMeta.hasDisplayName()) {
                    String structureName = itemMeta.getDisplayName();
                    if (multiBlockLoader.getStructures().containsKey(structureName)) {
                        List<MultiBlockLoader.MultiBlockStructure> structure = multiBlockLoader.getStructures().get(structureName).getBlocks();
                        placeStructure(clickedBlock, structure, structureName);
                    }
                }
            }
        }
    }



    private boolean checkSpace(Block baseBlock, List<MultiBlockLoader.MultiBlockStructure> structure) {
        for (MultiBlockLoader.MultiBlockStructure block : structure) {
            if (baseBlock.getRelative(block.getRelativePosition().getBlockX(), block.getRelativePosition().getBlockY(), block.getRelativePosition().getBlockZ()).getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }
    private boolean structureExists(Block baseBlock) {
        for (List<PlacedMultiBlockStructure> structures : placedStructures.values()) {
            for (PlacedMultiBlockStructure structure : structures) {
                if (structure.getBaseLocation().equals(baseBlock.getLocation())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void placeStructure(Block baseBlock, List<MultiBlockLoader.MultiBlockStructure> structure, String structureName) {
        for (MultiBlockLoader.MultiBlockStructure block : structure) {
            Material material = block.getType();
            if (!material.isLegacy()) {
                Block placedBlock = baseBlock.getRelative(block.getRelativePosition().getBlockX(), block.getRelativePosition().getBlockY(), block.getRelativePosition().getBlockZ());
                placedBlock.setType(material);
            } else {
                System.out.println("Skipped legacy block: " + material);
            }
        }

        PlacedMultiBlockStructure placedStructure = new PlacedMultiBlockStructure(structureName, baseBlock.getLocation(), structure);
        List<PlacedMultiBlockStructure> structuresAtLocation = placedStructures.getOrDefault(placedStructure.getBaseLocation().toString(), new ArrayList<>());
        structuresAtLocation.add(placedStructure);
        placedStructures.put(placedStructure.getBaseLocation().toString(), structuresAtLocation);
    }



    private class GiveStructureCommand implements CommandExecutor, TabCompleter {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage("Usage: /giveStructure <structureName>");
                return true;
            }

            String structureName = args[0];
            if (!multiBlockLoader.getStructures().containsKey(structureName)) {
                sender.sendMessage("Unknown structure: " + structureName);
                return true;
            }

            ItemStack structureItem = new ItemStack(Material.STICK, 1);
            ItemMeta meta = structureItem.getItemMeta();
            meta.setDisplayName(structureName);
            structureItem.setItemMeta(meta);

            ((Player) sender).getInventory().addItem(structureItem);
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                List<String> structureNames = new ArrayList<>(multiBlockLoader.getStructures().keySet());
                return StringUtil.copyPartialMatches(args[0], structureNames, new ArrayList<>());
            }
            return null;
        }
    }

    private class RemoveStructureCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage("Usage: /removeStructure <structureName>");
                return true;
            }

            String structureName = args[0];
            List<PlacedMultiBlockStructure> structures = placedStructures.get(structureName);
            if (structures == null || structures.isEmpty()) {
                sender.sendMessage("Unknown structure: " + structureName);
                return true;
            }

            for (PlacedMultiBlockStructure placedStructure : structures) {
                removeStructure(placedStructure.getBaseLocation().toString());
            }

            return true;
        }
    }
    private class BuildingModeCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;
            UUID playerUuid = player.getUniqueId();
            BossBar bossBar = builderModeBars.get(playerUuid);

            if (builderModePlayers.contains(playerUuid)) {
                builderModePlayers.remove(playerUuid);
                bossBar.setVisible(false); // Hide the BossBar
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (builderModePlayers.contains(onlinePlayer.getUniqueId())) {
                        bossBar = builderModeBars.get(onlinePlayer.getUniqueId());
                        bossBar.setVisible(true); // Show the BossBar for other players in build mode
                    }
                }
                sender.sendMessage("Builder mode disabled.");
            } else {
                builderModePlayers.add(playerUuid);
                bossBar.setVisible(true); // Show the BossBar
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (builderModePlayers.contains(onlinePlayer.getUniqueId())) {
                        bossBar = builderModeBars.get(onlinePlayer.getUniqueId());
                        bossBar.setVisible(true); // Show the BossBar for other players in build mode
                    }
                }
                sender.sendMessage("Builder mode enabled.");
            }

            return true;
        }
    }


}

