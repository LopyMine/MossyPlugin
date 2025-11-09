package net.lopymine.mossyplugin.settings.api;

import com.google.gson.JsonElement;
import java.util.*;

public class ForgeDependenciesAPI {

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

	public static String getParchmentVersion(String minecraft) {
		try {
			List<String> list = new ArrayList<>(
					JsonHelper.get("https://ldtteam.jfrog.io/artifactory/api/storage/parchmentmc-public/org/parchmentmc/data/parchment-%s/".formatted(minecraft))
						.getAsJsonObject()
						.get("children")
						.getAsJsonArray()
						.asList()
						.stream()
						.map(JsonElement::getAsJsonObject)
						.filter((e) -> e.get("folder").getAsBoolean())
						.map((e) -> e.get("uri").getAsString().substring(1))
						.filter((n) -> n.indexOf(".") != n.lastIndexOf(".") && !n.contains("-"))
						.toList()
			);
			Collections.sort(list);
			return list.get(list.size()-1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
