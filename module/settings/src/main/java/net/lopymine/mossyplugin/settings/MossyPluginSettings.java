package net.lopymine.mossyplugin.settings;

import java.io.*;
import java.util.*;
import lombok.Getter;
import net.lopymine.mossyplugin.common.*;
import net.lopymine.mossyplugin.settings.manager.*;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

@Getter
public class MossyPluginSettings implements Plugin<Settings> {

	public static final MossyLogger LOGGER = new MossyLogger("Settings");

	@Override
	public void apply(@NotNull Settings settings) {
		Properties gradleProperties = getGradleProperties(settings.getRootDir());
		settings.getRootProject().setName(MossyUtils.getProperty(gradleProperties, "data.mod_name"));
		LOGGER.setup(settings.getRootProject().getName());

		List<String> additionalDependencies = getAdditionalDependencies(gradleProperties);
		if (additionalDependencies.isEmpty()) {
			LOGGER.log("No additional dependencies!");
		} else {
			LOGGER.log("Found additional dependencies: [%s]".formatted(String.join(", ", additionalDependencies)));
		}

		List<String> multiVersions = getMultiVersions(gradleProperties);

		LOGGER.log("Found MC versions: [%s]".formatted(String.join(", ", multiVersions)));

		StonecutterManager.apply(settings, multiVersions);
		AccessWidenerManager.apply(settings, multiVersions);
		VersionedGradlePropertiesManager.apply(settings, gradleProperties, multiVersions, additionalDependencies);
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

	public static List<String> getMultiVersions(Properties gradleProperties) {
		return Arrays.stream(MossyUtils.getProperty(gradleProperties, "multi_versions").split(" ")).toList();
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
