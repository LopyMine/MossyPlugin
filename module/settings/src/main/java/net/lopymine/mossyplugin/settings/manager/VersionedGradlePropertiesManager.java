package net.lopymine.mossyplugin.settings.manager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.settings.MossyPluginSettings;
import net.lopymine.mossyplugin.settings.api.*;
import net.lopymine.mossyplugin.settings.loader.*;
import net.lopymine.mossyplugin.settings.project.MossyProject;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

public class VersionedGradlePropertiesManager {

	public static void apply(@NotNull Settings settings, Properties gradleProperties, List<MossyProject> projects, List<String> additionalDependencies) {
		Path path = settings.getRootDir().toPath();

		for (MossyProject project : projects) {
			try {
				createGradleProperties(
						path,
						project.projectName(),
						project.loaderName(),
						project.minecraftVersion(),
						additionalDependencies,
						gradleProperties,
						(modId, ver) -> ModrinthDependenciesAPI.getVersion(modId, ver, project.loaderName()),
						project.loaderManager()
				);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create versioned gradle properties for " + project.projectName() + ", reason: " + e.getMessage(), e);
			}
		}
	}

	public static void createGradleProperties(
			Path rootPath,
			String projectName,
			String loader,
			String minecraft,
			List<String> additionalDependencies,
			Properties rootGradleProperties,
			BiFunction<String, String, String> dependResolver,
			LoaderManager loaderManager
	) throws IOException {
		File gradlePropertiesFile = getOrCreateGradlePropertiesFile(rootPath, projectName);
		if (gradlePropertiesFile == null) {
			return;
		}

		Properties gradleProperties = new Properties();
		try (InputStream stream = new FileInputStream(gradlePropertiesFile)) {
			gradleProperties.load(stream);
		}

		String fileText = Files.readString(gradlePropertiesFile.toPath(), StandardCharsets.UTF_8);
		boolean isEmpty = fileText.isBlank();

		List<String> missingDependencies = new ArrayList<>();
		for (String dependencyId : additionalDependencies) {
			String key = "dep." + dependencyId;
			String dependencyValue = rootGradleProperties.getProperty(key);
			if (!"[VERSIONED]".equals(dependencyValue)) {
				continue;
			}
			if (!gradleProperties.containsKey(key)) {
				missingDependencies.add(dependencyId);
			}
		}

		List<String> oldDependencies = new ArrayList<>();
		for (String key : gradleProperties.stringPropertyNames()) {
			if (!key.startsWith("dep.")) {
				continue;
			}
			String dependencyValue = rootGradleProperties.getProperty(key);
			if (dependencyValue != null && !"[VERSIONED]".equals(dependencyValue)) {
				oldDependencies.add(key);
				continue;
			}
			String dependencyId = MossyUtils.substringSince(key, ".");
			if (!additionalDependencies.contains(dependencyId)) {
				oldDependencies.add(key);
			}
		}

		boolean shouldUpdate = fileText.replace(" ", "").contains("=[UPDATE]");

		if (!isEmpty && !shouldUpdate && missingDependencies.isEmpty() && oldDependencies.isEmpty()) {
			return;
		}

		if (isEmpty) {
			StringBuilder builder = new StringBuilder();
			builder.append("# Versioned Properties\n");
			builder.append("# Tip: You can set any dependency value to \"[UPDATE]\"\n");
			builder.append("# and reload Gradle to update only it's value.\n\n");

			loaderManager.fillGPWithProperties(builder, minecraft);

			if (!additionalDependencies.isEmpty()) {
				builder.append("\n");
				builder.append("# Additional Dependencies Properties\n");
				for (String dependency : additionalDependencies) {
					fillModrinthDependency(builder, dependency, dependResolver.apply(dependency, minecraft), minecraft, loader);
				}
			}
			Files.writeString(gradlePropertiesFile.toPath(), builder.toString(), StandardCharsets.UTF_8);
			MossyPluginSettings.LOGGER.log("Successfully created gradle.properties for " + projectName);
			return;
		}

		if (shouldUpdate) {
			String text = fileText.replace(" ", "ㅤ").trim();
			for (String key : gradleProperties.stringPropertyNames()) {
				String dependencyValue = gradleProperties.getProperty(key);
				if (!"[UPDATE]".equals(dependencyValue)) {
					continue;
				}

				String dependencyId = MossyUtils.substringSince(key, ".");
				String updatedValue = key.startsWith("dep.") ?
						dependResolver.apply(dependencyId, minecraft)
						:
						key.startsWith("build.") ?
								loaderManager.getGPUpdatedProperty(dependencyId, minecraft)
								:
								null;
				if (updatedValue == null) {
					continue;
				}

				String oldLine = key + "=[UPDATE]";
				String updatedLine = "%s=%s".formatted(key, updatedValue);

				text = text.replace(oldLine, updatedLine);
			}
			String finalText = text.replace("ㅤ", " ");
			Files.writeString(gradlePropertiesFile.toPath(), finalText, StandardCharsets.UTF_8);
			MossyPluginSettings.LOGGER.log("Successfully updated gradle.properties for " + projectName);
		}

		if (!missingDependencies.isEmpty()) {
			String text = Files.readString(gradlePropertiesFile.toPath(), StandardCharsets.UTF_8);
			StringBuilder builder = new StringBuilder(text.endsWith("\n") ? text : text + "\n");
			for (String depend : missingDependencies) {
				fillModrinthDependency(builder, depend, dependResolver.apply(depend, minecraft), minecraft, loader);
			}
			Files.writeString(gradlePropertiesFile.toPath(), builder.toString(), StandardCharsets.UTF_8);
			MossyPluginSettings.LOGGER.log("Successfully added new depends " + missingDependencies + " to gradle.properties for " + projectName);
		}

		if (!oldDependencies.isEmpty()) {
			String text = Files.readString(gradlePropertiesFile.toPath(), StandardCharsets.UTF_8);
			List<String> removedDependencies = new ArrayList<>();
			for (String key : oldDependencies) {
				String dependencyId = MossyUtils.substringSince(key, ".");
				String dependencyVersion = gradleProperties.getProperty(key);
				StringBuilder builder = new StringBuilder();
				fillModrinthDependency(builder, dependencyId, dependencyVersion, minecraft, loader);
				text = text.replace(builder.toString(), "");
				removedDependencies.add(dependencyId);
			}
			Files.writeString(gradlePropertiesFile.toPath(), text, StandardCharsets.UTF_8);
			MossyPluginSettings.LOGGER.log("Successfully removed old depends " + removedDependencies + " from gradle.properties for " + projectName);
		}
	}

	private static void fillModrinthDependency(StringBuilder builder, String id, String version, String minecraft, String loader) {
		builder.append("# %s, check https://modrinth.com/mod/%s/versions?g=%s&l=%s\n".formatted(id, id, minecraft, loader));
		builder.append("dep.%s=%s\n".formatted(id, version));
	}

	private static File getOrCreateGradlePropertiesFile(Path path, String version) {
		try {
			Path folder = path.resolve("versions/" + version);
			File folderFile = folder.toFile();
			if (!folderFile.exists() && !folderFile.mkdirs()) {
				MossyPluginSettings.LOGGER.log("Failed to get or create folder for " + version);
				return null;
			}
			File gradlePropertiesFile = folder.resolve("gradle.properties").toFile();
			if (!gradlePropertiesFile.exists() && !gradlePropertiesFile.createNewFile()) {
				MossyPluginSettings.LOGGER.log("Failed to get or create gradle.properties for " + version);
				return null;
			}
			return gradlePropertiesFile;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
