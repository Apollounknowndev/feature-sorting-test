package dev.worldgen.test.mixin;

import dev.worldgen.test.Exporter;
import dev.worldgen.test.FeatureSorter;
import dev.worldgen.test.TestMod;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    @Unique
    private static final RegistryKey<PlacedFeature> UNREGISTERED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.ofVanilla("unregistered"));

    @Unique
    private static boolean exported = false;

    @Shadow
    @Final
    private Supplier<List<PlacedFeatureIndexer.IndexedFeatures>> indexedFeaturesListSupplier;

    @Inject(method = "generateFeatures", at = @At("HEAD"))
    private void output(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci) {
        // Hack to prevent the feature list from being exported every time a chunk is generated
        if (exported) return;

        List<PlacedFeatureIndexer.IndexedFeatures> featureList = indexedFeaturesListSupplier.get();
        List<GenerationStep.Feature> steps = List.of(GenerationStep.Feature.values());
        Registry<PlacedFeature> registry = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);

        Exporter exporter = new Exporter("sorted_features");
        exporter.addLine("== Feature List ==");

        for (int i = 0; i < featureList.size(); i++) {
            PlacedFeatureIndexer.IndexedFeatures features = featureList.get(i);
            exporter.addLine("");
            exporter.addLine(String.format("- %s", steps.get(i).asString()));

            for (PlacedFeature feature : features.features()) {
                exporter.addLine("   - " + registry.getEntry(feature).getKey().orElse(UNREGISTERED).getValue());
            }
        }

        exporter.export();
        exported = true;
    }
}