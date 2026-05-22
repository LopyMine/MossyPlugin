package net.lopymine.mossyplugin.settings.api;

import com.google.gson.JsonElement;
import java.util.*;
import net.lopymine.mossyplugin.settings.MossyPluginSettings;

public class ForgeCommonDependenciesAPI {

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
			MossyPluginSettings.LOGGER.log("Failed to find parchment version!");
			e.printStackTrace(System.out);
			return "unknown";
		}
	}

}
