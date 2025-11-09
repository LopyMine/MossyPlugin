package net.lopymine.mossyplugin.settings.loader;

import java.io.*;

public interface LoaderManager {

	static LoaderManager of(String loader) {
		if (loader.contains("forge")) {
			return NeoForgeLoaderManager.getInstance();
		} else if (loader.contains("fabric")) {
			return FabricLoaderManager.getInstance();
		} else {
			throw new RuntimeException("Unsupported loader \"%s\"!".formatted(loader));
		}
	}

	void fillGPWithProperties(StringBuilder builder, String minecraft);

	String getGPUpdatedProperty(String id, String minecraft);

	void fillAWWillExampleText(FileWriter writer, String minecraft) throws IOException;

	String getAWExtension();

}
