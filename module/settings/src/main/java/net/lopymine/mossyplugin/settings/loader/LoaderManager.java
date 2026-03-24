package net.lopymine.mossyplugin.settings.loader;

import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension;
import java.io.*;

public interface LoaderManager {

	static LoaderManager of(String loader) {
		if (loader.equals("forge")) {
			return ForgeLoaderManager.getInstance();
		} else if (loader.equals("neoforge")) {
			return NeoForgeLoaderManager.getInstance();
		} else if (loader.contains("fabric")) {
			return FabricLoaderManager.getInstance();
		} else {
			throw new RuntimeException("Unsupported loader \"%s\"!".formatted(loader));
		}
	}

	void fillGPWithProperties(StringBuilder builder, String minecraft);

	String getGPUpdatedProperty(String id, String minecraft);

	void fillAWWillExampleText(FileWriter writer, String minecraft, StonecutterSettingsExtension stonecutter) throws IOException;

	String getAWExtension(String minecraft, StonecutterSettingsExtension stonecutter);

}
