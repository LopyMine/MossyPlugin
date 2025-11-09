package net.lopymine.mossyplugin.settings.loader;

import java.io.FileWriter;
import java.util.List;
import net.lopymine.mossyplugin.settings.api.ForgeDependenciesAPI;

public class NeoForgeLoaderManager implements LoaderManager {

	private static final NeoForgeLoaderManager INSTANCE = new NeoForgeLoaderManager();

	public static NeoForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void fillGPWithProperties(StringBuilder builder, String minecraft) {
		builder.append("# NeoForge Properties, check https://neoforged.net/\n");
		for (String id : List.of("neoforge", "parchment")) {
			builder.append("build.%s=%s\n".formatted(id, this.getGPUpdatedProperty(id, minecraft)));
		}
	}

	@Override
	public String getGPUpdatedProperty(String id, String minecraft) {
		return switch (id) {
			case "neoforge" -> ForgeDependenciesAPI.getNeoForgeVersion(minecraft);
			case "parchment" -> ForgeDependenciesAPI.getParchmentVersion(minecraft);
			default -> "unknown";
		};
	}

	@Override
	public void fillAWWillExampleText(FileWriter writer, String minecraft) {

	}

	@Override
	public String getAWExtension() {
		return "cfg";
	}

}
