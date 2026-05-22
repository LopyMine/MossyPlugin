package net.lopymine.mossyplugin.settings.api;

import net.lopymine.mossyplugin.settings.MossyPluginSettings;

public class ForgeDependenciesAPI {

	public static String getForgeVersion(String minecraft) {
		try {
			return JsonHelper.get("https://maven.minecraftforge.net/api/maven/latest/version/releases/net/minecraftforge/forge?filter=%s-".formatted(minecraft))
					.getAsJsonObject()
					.get("version")
					.getAsString();
		} catch (Exception e) {
			MossyPluginSettings.LOGGER.log("Failed to find forge version!");
			e.printStackTrace(System.out);
			return "unknown";
		}
	}

}
