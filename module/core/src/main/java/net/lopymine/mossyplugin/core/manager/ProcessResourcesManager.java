package net.lopymine.mossyplugin.core.manager;

import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreProcessResourcesExtension;
import org.gradle.api.*;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.TaskInputsInternal;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class ProcessResourcesManager {

	public static void apply(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().create("mossyResources", MossyCoreProcessResourcesExtension.class);

		project.getGradle().addProjectEvaluationListener(new ProjectEvaluationListener() {
			@Override
			public void beforeEvaluate(@NotNull Project project) {
			}

			@Override
			public void afterEvaluate(@NotNull Project project, @NotNull ProjectState state) {
				MossyCoreProcessResourcesExtension extension = project.getExtensions().getByType(MossyCoreProcessResourcesExtension.class);
				ProcessResourcesManager.processResources(data, extension);
				project.getGradle().removeProjectEvaluationListener(this);
			}
		});
	}

	private static void processResources(@NotNull MossyProjectConfigurationData data, MossyCoreProcessResourcesExtension extension) {
		Project project = data.project();
		MossyPluginCore plugin = data.plugin();

		ProcessResources processResources = (ProcessResources) project.getTasks().getByName("processResources");
		TaskInputsInternal inputs = processResources.getInputs();

		String mcVersion = plugin.getProjectMultiVersion().projectVersion();
		String modId = project.getProperty("data.mod_id");

		Map<String, String> properties = project.getMossyProperties("data");
		properties.putAll(project.getMossyProperties("build"));
		properties.putAll(project.getMossyProperties("dep"));
		properties.putAll(extension.getCustomProperties());
		properties.put("java", String.valueOf(plugin.getJavaVersionIndex()));
		properties.put("minecraft", mcVersion);
		properties.put("fabric_api_id", project.getStonecutter().compare("1.19.1", mcVersion) >= 0 ? "fabric" : "fabric-api");
		properties.put("mod_version", project.getVersion().toString());

		List<String> mixinConfigs = new ArrayList<>();
		mixinConfigs.add("%s.mixins.json".formatted(modId));
		String additionalMixinConfigIds = project.getProperty("data.mixin_configs");
		if (!additionalMixinConfigIds.equals("none")) {
			for (String config : additionalMixinConfigIds.split(" ")) {
				mixinConfigs.add("%s-%s.mixins.json".formatted(modId, config));
			}
		}

		properties.put("fabric_trick_mixin_configs", String.join("\",\"", mixinConfigs));
		properties.put("neoforge_trick_mixin_configs", String.join("\n", mixinConfigs.stream().map("[[mixins]]\nconfig = \"%s\"\n"::formatted).toList()));
		properties.put("fabric_trick_side", project.getProperty("data.sides").toLowerCase(Locale.ROOT).replace("both", "*"));
		properties.put("neoforge_trick_side", project.getProperty("data.sides").toUpperCase(Locale.ROOT));

		properties.forEach(inputs::property);

		List<String> patterns = new ArrayList<>(List.of("*.json5", "META-INF/*.toml", "pack.mcmeta", "*.json", "assets/%s/lang/*.json".formatted(modId)));
		List<String> expandFiles = extension.getExpandFiles();
		if (expandFiles != null) {
			patterns.addAll(expandFiles);
		}

		processResources.filesMatching(patterns, (details) -> {
			if (data.loaderManager().excludeUselessFiles(details)) {
				return;
			}
			details.expand(properties);
		});

		String e = data.loaderManager().getAWFileExtension();
		processResources.filesMatching("aws/*.*", (details) -> {
			if (!details.getName().equals("%s.%s".formatted(project.getName(), e))) {
				details.exclude();
			} else {
				if (data.loaderName().contains("forge")) {
					String[] segments = details.getRelativePath().getSegments();
					String[] strings = Arrays.copyOf(segments, segments.length);
					strings[strings.length-1] = "accesstransformer.cfg";
					strings[strings.length-2] = "META-INF";
					RelativePath path = new RelativePath(true, strings);
					details.setRelativePath(path);
				}
			}
		});

	}
}
