package net.lopymine.mossyplugin.settings.api;

import net.lopymine.mossyplugin.settings.MossyPluginSettings;

public class NeoForgeDependenciesAPI {

	public static String getNeoForgeVersion(String minecraft) {
		String major;
		String minor;
		String[] split = minecraft.split("\\.");
		if (split.length == 1) {
			MossyPluginSettings.LOGGER.log("Unsupported Minecraft Version \"%s\"".formatted(minecraft));
			return "unknown";
		}

		if (minecraft.startsWith("1.")) {
			major = split[1];
			minor = split.length == 2 ? "0" : split[2];
		} else {
			major = split[0];
			minor = split[1];
		}
		try {
			return JsonHelper.get("https://maven.neoforged.net/api/maven/latest/version/releases/net/neoforged/neoforge?filter=%s.%s.".formatted(major, minor))
					.getAsJsonObject()
					.get("version")
					.getAsString();
		} catch (Exception e) {
			MossyPluginSettings.LOGGER.log("Failed to get neoforge version!");
			e.printStackTrace(System.out);
			return "unknown";
		}
	}

}
