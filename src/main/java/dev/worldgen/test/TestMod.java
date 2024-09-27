package dev.worldgen.test;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMod implements ModInitializer {
	public static final String MOD_ID = "test";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Whether the sorted list of biomes inputted into the placed feature sorter should be exported.
	 * Running multiple times should show a consistent order, with `mushroom_fields` first, then ocean biomes, then land biomes.
	 * This list ordering will differ when the BiomeSource is modified, but the base vanilla source is constant.
	 */
	public boolean exportSortedBiomes = true;

	/**
	 * Whether the sorted list of placed features should be exported.
	 */
	public boolean exportSortedFeatures = true;
	public boolean exportFeaturesToNextFeatures = true;

	@Override
	public void onInitialize() {

	}
}