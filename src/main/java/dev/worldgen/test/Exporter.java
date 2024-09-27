package dev.worldgen.test;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for exporting files.
 */
public class Exporter {
    private final Path path;
    private final List<String> lines = new ArrayList<>();

    public Exporter(String string) {
        this.path = FabricLoader.getInstance().getGameDir().resolve(String.format("exports/%s.txt", string));
        this.path.getParent().toFile().mkdirs();
    }

    public void addLine(String line) {
        lines.add(line);
    }

    public void export() {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            TestMod.LOGGER.error(String.format("Couldn't export file %s: %s", path.toString(), e));
        }
    }
}
