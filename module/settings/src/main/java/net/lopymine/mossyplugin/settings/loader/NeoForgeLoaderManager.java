package net.lopymine.mossyplugin.settings.loader;

import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension;
import java.io.FileWriter;
import java.util.*;
import net.lopymine.mossyplugin.settings.api.*;

public class NeoForgeLoaderManager implements LoaderManager {

	private static final NeoForgeLoaderManager INSTANCE = new NeoForgeLoaderManager();

	public static NeoForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void fillGPWithProperties(StringBuilder builder, String minecraft, StonecutterSettingsExtension stonecutter) {
		builder.append("# NeoForge Properties, check https://neoforged.net/\n");

		List<String> list = new ArrayList<>();
		list.add("neoforge");
		if (!stonecutter.eval(minecraft, ">=26.1")) {
			list.add("parchment");
		}

		for (String id : list) {
			builder.append("build.%s=%s\n".formatted(id, this.getGPUpdatedProperty(id, minecraft, stonecutter)));
		}
	}

	@Override
	public String getGPUpdatedProperty(String id, String minecraft, StonecutterSettingsExtension stonecutter) {
		return switch (id) {
			case "neoforge" -> NeoForgeDependenciesAPI.getNeoForgeVersion(minecraft);
			case "parchment" -> {
				if (stonecutter.eval(minecraft, ">=26.1")) {
					yield  "unknown";
				}
				yield ForgeCommonDependenciesAPI.getParchmentVersion(minecraft);
			}
			default -> "unknown";
		};
	}

	@Override
	public void fillAWWillExampleText(FileWriter writer, String minecraft, StonecutterSettingsExtension stonecutter) {

	}

	@Override
	public String getAWExtension(String minecraft, StonecutterSettingsExtension stonecutter) {
		return "cfg";
	}

}
