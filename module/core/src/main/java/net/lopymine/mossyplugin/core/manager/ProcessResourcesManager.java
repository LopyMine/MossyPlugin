package net.lopymine.mossyplugin.core.manager;

import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.extension.MossyCoreProcessResourcesExtension;
import org.gradle.api.*;
import org.gradle.api.internal.TaskInputsInternal;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class ProcessResourcesManager {

	public static void apply(@NotNull Project project, MossyPluginCore plugin) {
		project.getExtensions().create("mossyResources", MossyCoreProcessResourcesExtension.class);

		project.getGradle().addProjectEvaluationListener(new ProjectEvaluationListener() {
			@Override
			public void beforeEvaluate(@NotNull Project project) {
			}

			@Override
			public void afterEvaluate(@NotNull Project project, @NotNull ProjectState state) {
				MossyCoreProcessResourcesExtension extension = project.getExtensions().getByType(MossyCoreProcessResourcesExtension.class);
				ProcessResourcesManager.processResources(project, plugin, extension);
				project.getGradle().removeProjectEvaluationListener(this);
			}
		});
	}

	private static void processResources(Project project, MossyPluginCore plugin, MossyCoreProcessResourcesExtension extension) {
		ProcessResources processResources = (ProcessResources) project.getTasks().getByName("processResources");
		TaskInputsInternal inputs = processResources.getInputs();

		String mcVersion = plugin.getProjectMultiVersion().projectVersion();
		String modId = project.getProperty("data.mod_id");

		Map<String, String> properties = project.getMossyProperties("data");
		properties.putAll(project.getMossyProperties("build"));
		properties.putAll(project.getMossyProperties("dep"));
		properties.put("java", String.valueOf(plugin.getJavaVersionIndex()));
		properties.put("minecraft", mcVersion);
		properties.put("fabric_api_id", project.getStonecutter().compare("1.19.1", mcVersion) >= 0 ? "fabric" : "fabric-api");
		properties.put("mod_version", project.getVersion().toString());

		properties.forEach(inputs::property);

		List<String> patterns = new ArrayList<>(List.of("*.json5", "*.json", "assets/%s/lang/*.json".formatted(modId)));
		List<String> expandFiles = extension.getExpandFiles();
		if (expandFiles != null) {
			patterns.addAll(expandFiles);
		}

		processResources.filesMatching(patterns, (details) -> {
			details.expand(properties);
		});

		processResources.filesMatching("aws/*.accesswidener", (details) -> {
			if (!details.getName().startsWith(mcVersion)) {
				details.exclude();
			}
		});
	}
}
