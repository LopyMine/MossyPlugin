package net.lopymine.mossyplugin.settings.loader;

import java.io.FileWriter;
import java.util.List;
import net.lopymine.mossyplugin.settings.api.*;

public class ForgeLoaderManager implements LoaderManager {

	private static final ForgeLoaderManager INSTANCE = new ForgeLoaderManager();

	public static ForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void fillGPWithProperties(StringBuilder builder, String minecraft) {
		builder.append("# Forge Properties, check https://files.minecraftforge.net/net/minecraftforge/forge/index_%s.html\n".formatted(minecraft));
		for (String id : List.of("forge", "parchment")) {
			builder.append("build.%s=%s\n".formatted(id, this.getGPUpdatedProperty(id, minecraft)));
		}
	}

	@Override
	public String getGPUpdatedProperty(String id, String minecraft) {
		return switch (id) {
			case "forge" -> ForgeDependenciesAPI.getForgeVersion(minecraft);
			case "parchment" -> ForgeCommonDependenciesAPI.getParchmentVersion(minecraft);
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
