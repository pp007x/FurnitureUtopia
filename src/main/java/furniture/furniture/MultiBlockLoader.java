package furniture.furniture;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlockLoader {

    private final Map<String, Structure> structures = new HashMap<>();
    public MultiBlockLoader(File dataFolder) {
        Yaml yaml = new Yaml();
        File file = new File(dataFolder, "structures.yml");
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Map<String, Map<String, Object>> loadedStructures = (Map<String, Map<String, Object>>) yaml.load(inputStream);

        for (Map.Entry<String, Map<String, Object>> entry : loadedStructures.entrySet()) {
            String structureName = entry.getKey();
            Map<String, Object> structureMap = entry.getValue();
            List<Map<String, Object>> blocksData = (List<Map<String, Object>>) structureMap.get("blocks");

            List<MultiBlockStructure> blocks = new ArrayList<>();
            for (Map<String, Object> blockData : blocksData) {
                Vector vector = new Vector(
                        (Integer) blockData.get("x"),
                        (Integer) blockData.get("y"),
                        (Integer) blockData.get("z")
                );
                try {
                    Material material = Material.valueOf((String) blockData.get("type"));
                    blocks.add(new MultiBlockStructure(vector, material));
                } catch (IllegalArgumentException e) {
                    System.out.println("Skipped block with unknown material: " + blockData.get("type"));
                }
            }
            structures.put(structureName, new Structure(structureName, blocks));
        }
    }


    public Map<String, Structure> getStructures() {
        return structures;
    }

    public static class MultiBlockStructure {
        private final Vector relativePosition;
        private final Material type;

        public MultiBlockStructure(Vector relativePosition, Material type) {
            this.relativePosition = relativePosition;
            this.type = type;
        }

        public Vector getRelativePosition() {
            return relativePosition;
        }

        public Material getType() {
            return type;
        }
    }

    public static class Structure {
        private final String name;
        private final List<MultiBlockStructure> blocks;

        public Structure(String name, List<MultiBlockStructure> blocks) {
            this.name = name;
            this.blocks = blocks;
        }

        public String getName() {
            return name;
        }

        public List<MultiBlockStructure> getBlocks() {
            return blocks;
        }
    }
}
