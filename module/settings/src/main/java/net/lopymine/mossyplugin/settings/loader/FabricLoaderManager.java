package net.lopymine.mossyplugin.settings.loader;

import java.io.*;
import java.util.List;
import net.lopymine.mossyplugin.settings.api.*;

public class FabricLoaderManager implements LoaderManager {

	private static final FabricLoaderManager INSTANCE = new FabricLoaderManager();

	public static FabricLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void fillGPWithProperties(StringBuilder builder, String minecraft) {
		builder.append("# Fabric Properties, check https://fabricmc.net/develop/\n");
		for (String id : List.of("yarn", "fabric_api")) {
			builder.append("build.%s=%s\n".formatted(id, this.getGPUpdatedProperty(id, minecraft)));
		}
	}

	@Override
	public String getGPUpdatedProperty(String id, String minecraft) {
		return switch (id) {
			case "yarn" -> FabricDependenciesAPI.getYarnVersion(minecraft);
			case "fabric_api" -> ModrinthDependenciesAPI.getVersion("fabric-api", minecraft, "fabric");
			default -> "unknown";
		};
	}

	@Override
	public void fillAWWillExampleText(FileWriter writer, String minecraft) throws IOException {
		writer.write("accessWidener v2 named\n");
		writer.write("# " + minecraft + " AW\n");
	}

	@Override
	public String getAWExtension() {
		return "accesswidener";
	}
}
