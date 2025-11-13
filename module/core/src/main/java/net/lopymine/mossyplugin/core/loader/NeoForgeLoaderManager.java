package net.lopymine.mossyplugin.core.loader;

import java.util.List;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.neoforge.NeoForgeManager;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Project;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.PluginContainer;
import org.jetbrains.annotations.NotNull;

public class NeoForgeLoaderManager implements LoaderManager {

	private static final NeoForgeLoaderManager INSTANCE = new NeoForgeLoaderManager();

	public static NeoForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		plugins.apply("net.neoforged.moddev");
	}

	@Override
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension dependencies) {
		NeoForgeExtension extension = data.project().getExtensions().getByType(NeoForgeExtension.class);
		extension.setVersion(dependencies.getNeoForge());
		NeoForgeManager.apply(data, extension, dependencies);
	}

	@Override
	public void configureExtensions(@NotNull MossyProjectConfigurationData data) {
		data.project().afterEvaluate((project) -> {
			project.getTasks().named("createMinecraftArtifacts").configure((task) -> {
				task.dependsOn(":%s:stonecutterGenerate".formatted(data.projectName()));
			});
		});
	}

	@Override
	public String getModDependenciesImplementationMethod() {
		return "implementation";
	}

	@Override
	public String getJarTaskName() {
		return "jar";
	}

	@Override
	public String getAWFileExtension() {
		return "cfg";
	}

	@Override
	public boolean excludeUselessFiles(FileCopyDetails details) {
		boolean excluded = false;
		for (String file : List.of("fabric.mod.json", "mods.toml")) {
			if (details.getName().equals(file)) {
				details.exclude();
				excluded = true;
			}
		}
		return excluded;
	}
}
