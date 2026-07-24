package net.lopymine.mossyplugin.core.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import org.jetbrains.annotations.Nullable;

public class ModrinthFileApi {

	public static void download(String projectId, String minecraftVersion, @Nullable String loader, File directory) {
		String encodedMinecraftVersion = URLEncoder.encode("[\"%s\"]".formatted(minecraftVersion), StandardCharsets.UTF_8);
		String url = "https://api.modrinth.com/v2/project/%s/version?game_versions=%s".formatted(projectId, encodedMinecraftVersion);
		if (loader != null) {
			url += "&loaders=" + URLEncoder.encode("[\"%s\"]".formatted(loader), StandardCharsets.UTF_8);
		}

		JsonObject file = getPrimaryFile(url, projectId);
		if (file == null) {
			MossyPluginCore.LOGGER.log("No Modrinth file for \"%s\" (%s, %s)", projectId, loader == null ? "any" : loader, minecraftVersion);
			return;
		}

		String fileUrl = file.get("url").getAsString();
		String fileName = file.get("filename").getAsString();
		File target = new File(directory, fileName);

		try (InputStream stream = openStream(fileUrl)) {
			Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			MossyPluginCore.LOGGER.log("Failed to download \"%s\" from %s", projectId, fileUrl);
			e.printStackTrace(System.out);
			return;
		}

		MossyPluginCore.LOGGER.log("Downloaded %s", fileName);
	}

	private static @Nullable JsonObject getPrimaryFile(String url, String modId) {
		JsonArray versions;
		try (InputStream stream = openStream(url); JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			versions = new Gson().fromJson(reader, JsonArray.class);
		} catch (Exception e) {
			MossyPluginCore.LOGGER.log("Failed to query Modrinth for \"%s\"", modId);
			e.printStackTrace(System.out);
			return null;
		}

		if (versions == null || versions.isEmpty()) {
			return null;
		}

		JsonArray files = versions.get(0).getAsJsonObject().get("files").getAsJsonArray();
		JsonObject fallback = null;
		for (JsonElement element : files) {
			JsonObject file = element.getAsJsonObject();
			if (fallback == null) {
				fallback = file;
			}
			if (file.get("primary").getAsBoolean()) {
				return file;
			}
		}
		return fallback;
	}

	private static InputStream openStream(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		connection.setRequestMethod("GET");
		connection.setInstanceFollowRedirects(true);
		return connection.getInputStream();
	}

}
