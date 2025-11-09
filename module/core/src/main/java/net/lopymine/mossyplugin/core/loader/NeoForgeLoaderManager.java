package net.lopymine.mossyplugin.core.loader;

import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.*;
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
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension) {
		NeoForgeManager.apply(data, extension);
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
		if (details.getName().equals("fabric.mod.json")) {
			details.exclude();
			return true;
		}
		return false;
	}
}
