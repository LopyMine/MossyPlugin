package net.lopymine.mossyplugin.core.loader;

import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.neoforge.OldNeoForgeManager;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.PluginContainer;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class OldNeoForgeLoaderManager implements LoaderManager {

	private static final OldNeoForgeLoaderManager INSTANCE = new OldNeoForgeLoaderManager();

	public static OldNeoForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		plugins.apply("java-library");
		plugins.apply("net.neoforged.gradle.userdev");
	}

	@Override
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension dependencies) {
		Project project = data.project();
		OldNeoForgeManager.apply(data, dependencies);
		DependencyHandler deps = project.getDependencies();
		deps.add("implementation", "net.neoforged:neoforge:%s".formatted(dependencies.getNeoForge()));
	}

	@Override
	public void configureExtensions(@NotNull MossyProjectConfigurationData data) {
	}

	@Override
	public String getModDependenciesImplementationMethod(MossyProjectConfigurationData data) {
		return "implementation";
	}

	@Override
	public String getJarTaskName(MossyProjectConfigurationData data) {
		return "jar";
	}

	@Override
	public String getAWFileExtension(MossyProjectConfigurationData data) {
		return "cfg";
	}

	@Override
	public boolean excludeUselessFiles(MossyProjectConfigurationData data, FileCopyDetails details) {
		String metadata = data.project().getStonecutter().eval(data.comparableMinecraftVersion(), ">=1.20.5") ? "mods.toml" : "neoforge.mods.toml";

		boolean excluded = false;
		for (String file : List.of("fabric.mod.json", metadata)) {
			if (details.getName().equals(file)) {
				details.exclude();
				excluded = true;
			}
		}
		return excluded;
	}

	@Override
	public Map<String, String> getLoaderConfigurations(List<String> configurations, MossyProjectConfigurationData data) {
		Map<String, String> map = new HashMap<>();
		for (String s : configurations) {
			map.put(s, s.equals("include") ? "jarJar" : s);
		}
		return map;
	}
}
