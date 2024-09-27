package dev.worldgen.test;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.util.TopologicalSorts;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer.IndexedFeatures;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Functionally identical implementation of PlacedFeatureIndexer#collectIndexedFeatures / FeatureSorter#buildFeaturesPerStep without decompilation artifacts.
 */
public class FeatureSorter {
    private static boolean exported = false;

    public static List<IndexedFeatures> indexAndExportFeatures(List<RegistryEntry<Biome>> biomes, Function<RegistryEntry<Biome>, List<RegistryEntryList<PlacedFeature>>> biomesToPlacedFeaturesList, boolean listInvolvedBiomesOnFailure) {
        Object2IntMap<PlacedFeature> featuresToIndex = new Object2IntOpenHashMap<>();
        MutableInt mutableInt = new MutableInt(0);
        Comparator<IndexedFeature> comparator = Comparator.comparingInt(IndexedFeature::step).thenComparingInt(IndexedFeature::featureIndex);
        Map<IndexedFeature, Set<IndexedFeature>> featureToNextFeatures = new TreeMap<>(comparator);
        int steps = 0;

        for (RegistryEntry<Biome> biome : biomes) {
            List<RegistryEntryList<PlacedFeature>> biomePlacedFeatures = biomesToPlacedFeaturesList.apply(biome);
            steps = Math.max(steps, biomePlacedFeatures.size());

            ArrayList<IndexedFeature> biomeIndexedFeatures = new ArrayList<>();

            for(int i = 0; i < biomePlacedFeatures.size(); ++i) {
                for (RegistryEntry<PlacedFeature> placedFeature : biomePlacedFeatures.get(i)) {
                    biomeIndexedFeatures.add(new IndexedFeature(featuresToIndex.computeIfAbsent(placedFeature.value(), (feature -> mutableInt.getAndIncrement())), i, placedFeature));
                }
            }

            for(int i = 0; i < biomeIndexedFeatures.size(); ++i) {
                Set<IndexedFeature> set = featureToNextFeatures.computeIfAbsent(biomeIndexedFeatures.get(i), feature -> new TreeSet<>(comparator));
                if (i < biomeIndexedFeatures.size() - 1) {
                    set.add(biomeIndexedFeatures.get(i + 1));
                }
            }
        }

        Set<IndexedFeature> set2 = new TreeSet<>(comparator);
        Set<IndexedFeature> set3 = new TreeSet<>(comparator);
        ArrayList<IndexedFeature> list = new ArrayList<>();

        exportBiomes(biomes);
        exportFeaturesToNextFeatures(featureToNextFeatures);

        for (IndexedFeature indexedFeature : featureToNextFeatures.keySet()) {
            if (!set3.isEmpty()) {
                throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
            }

            if (!set2.contains(indexedFeature)) {
                // TopologicalSorts#sort returning true means a loop occurred
                if (TopologicalSorts.sort(featureToNextFeatures, set2, set3, list::add, indexedFeature)) {
                    if (!listInvolvedBiomesOnFailure) {
                        throw new IllegalStateException("Feature order cycle found");
                    }

                    List<RegistryEntry<Biome>> list3 = new ArrayList<>(biomes);

                    int k;
                    do {
                        k = list3.size();
                        ListIterator<RegistryEntry<Biome>> listIterator = list3.listIterator();

                        while (listIterator.hasNext()) {
                            RegistryEntry<Biome> object2 = listIterator.next();
                            listIterator.remove();

                            try {
                                indexAndExportFeatures(list3, biomesToPlacedFeaturesList, false);
                            } catch (IllegalStateException var18) {
                                continue;
                            }

                            listIterator.add(object2);
                        }
                    } while (k != list3.size());

                    throw new IllegalStateException("Feature order cycle found, involved sources: " + list3);
                }
            }
        }

        Collections.reverse(list);
        ImmutableList.Builder<IndexedFeatures> builder = ImmutableList.builder();

        for(int i = 0; i < steps; ++i) {
            int step = i;
            List<PlacedFeature> list4 = list.stream().filter((feature) -> feature.step() == step).map(IndexedFeature::feature).map(RegistryEntry::value).collect(Collectors.toList());
            builder.add(new IndexedFeatures(list4, Util.lastIdentityIndexGetter(list4)));
        }

        return builder.build();
    }

    private static void exportBiomes(List<RegistryEntry<Biome>> biomes) {
        if (!TestMod.exportSortedBiomes) return;

        if (exported) return;

        Exporter exporter = new Exporter("sorted_biomes");
        exporter.addLine("== Biome List ==");

        for (RegistryEntry<Biome> biome : biomes) {
            exporter.addLine("- "+biome.getKey().orElseThrow().getValue());
        }

        exporter.export();
    }

    private static void exportFeaturesToNextFeatures(Map<IndexedFeature, Set<IndexedFeature>> featureToNextFeatures) {
        if (!TestMod.exportFeaturesToNextFeatures) return;

        if (exported) return;

        Exporter exporter = new Exporter("features_to_next_features");
        exporter.addLine("== Features to Next Feature(s) ==");

        for (Map.Entry<IndexedFeature, Set<IndexedFeature>> entry : featureToNextFeatures.entrySet()) {
            exporter.addLine("");
            exporter.addLine(entry.getKey().id());

            if (entry.getValue().isEmpty()) exporter.addLine("- N/A");

            for (IndexedFeature nextFeature : entry.getValue()) {
                exporter.addLine("- "+nextFeature.id());
            }
        }

        exporter.export();

        exported = true;
    }


    // PlacedFeature changed to RegistryEntry<PlacedFeature> to preserve key for exporting
    public record IndexedFeature(int featureIndex, int step, RegistryEntry<PlacedFeature> feature) {
        private static final RegistryKey<PlacedFeature> INLINED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("inlined"));
        public String id() {
            return feature.getKey().orElse(INLINED).getValue().toString();
        }
    }
}
