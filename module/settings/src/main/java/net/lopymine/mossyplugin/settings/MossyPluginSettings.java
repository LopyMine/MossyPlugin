package net.lopymine.mossyplugin.settings;

import java.io.*;
import java.util.*;
import lombok.Getter;
import net.lopymine.mossyplugin.common.*;
import net.lopymine.mossyplugin.settings.loader.LoaderManager;
import net.lopymine.mossyplugin.settings.manager.*;
import net.lopymine.mossyplugin.settings.project.MossyProject;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

@Getter
public class MossyPluginSettings implements Plugin<Settings> {

	public static final String PLUGIN_VERSION = "1.1.51-beta";

	public static final MossyLogger LOGGER = new MossyLogger("Settings");

	@Override
	public void apply(@NotNull Settings settings) {
		Properties gradleProperties = getGradleProperties(settings.getRootDir());
		settings.getRootProject().setName(MossyUtils.getProperty(gradleProperties, "data.mod_name"));
		LOGGER.setup(settings.getRootProject().getName());
		LOGGER.log("Running MossyPlugin " + PLUGIN_VERSION);

		settings.getPlugins().apply("org.gradle.toolchains.foojay-resolver-convention");

		List<String> additionalDependencies = getAdditionalDependencies(gradleProperties);
		if (additionalDependencies.isEmpty()) {
			LOGGER.log("No additional dependencies!");
		} else {
			LOGGER.log("Found additional dependencies: [%s]".formatted(String.join(", ", additionalDependencies)));
		}

		List<String> loaders = getLoaders(gradleProperties);
		Map<String, List<String>> loadersAndVersions = getLoadersAndVersions(gradleProperties, loaders);

		loadersAndVersions.forEach((loader, versions) -> {
			LOGGER.logModule(loader, "Found MC versions: [%s]".formatted(String.join(", ", versions)));
		});

		StonecutterManager.apply(settings, loadersAndVersions);
		List<MossyProject> projects = getMossyProjects(loadersAndVersions);

		AccessWidenerManager.apply(settings, projects);
		VersionedGradlePropertiesManager.apply(settings, gradleProperties, projects, additionalDependencies);
	}

	public static List<String> getAdditionalDependencies(Properties properties) {
		List<String> additionalDepends = new ArrayList<>();

		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			String key = entry.getKey().toString();
			if (key.startsWith("dep.")) {
				int i = key.indexOf(".") + 1;
				String modId = key.substring(i);
				additionalDepends.add(modId);
			}
		}

		return additionalDepends;
	}

	public static Map<String, List<String>> getLoadersAndVersions(Properties gradleProperties, List<String> loaders) {
		HashMap<String, List<String>> map = new HashMap<>();
		for (String loader : loaders) {
			List<String> versions = Arrays.stream(MossyUtils.getProperty(gradleProperties, "%s.multi_versions".formatted(loader)).split(" ")).toList();
			map.put(loader, versions);
		}
		return map;
	}

	public static List<MossyProject> getMossyProjects(Map<String, List<String>> loaderAndVersions) {
		List<MossyProject> projects = new ArrayList<>();

		loaderAndVersions.forEach((loader, versions) -> {
			LoaderManager loaderManager = LoaderManager.of(loader);
			for (String version : versions) {
				projects.add(new MossyProject("%s-%s".formatted(loader, version), loader, version, loaderManager));
			}
		});

		return projects;
	}

	public static List<String> getLoaders(Properties gradleProperties) {
		return Arrays.stream(MossyUtils.getProperty(gradleProperties, "mod_loaders").split(" ")).toList();
	}

	public static @NotNull Properties getGradleProperties(File project) {
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(project.toPath().resolve("gradle.properties").toFile())) {
			properties.load(reader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return properties;
	}

}
