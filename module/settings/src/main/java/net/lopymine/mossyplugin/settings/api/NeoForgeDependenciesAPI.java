package net.lopymine.mossyplugin.settings.api;

public class NeoForgeDependenciesAPI {

	public static String getNeoForgeVersion(String minecraft) {
		// 1.21
		// 1.21.10
		String[] split = minecraft.split("\\.");
		if (split.length == 1) {
			throw new RuntimeException("Unsupported Minecraft Version \"%s\"".formatted(minecraft));
		}
		String major = split[1];
		String minor = split.length == 2 ? "0" : split[2];
		try {
			return JsonHelper.get("https://maven.neoforged.net/api/maven/latest/version/releases/net/neoforged/neoforge?filter=%s.%s.".formatted(major, minor))
					.getAsJsonObject()
					.get("version")
					.getAsString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
