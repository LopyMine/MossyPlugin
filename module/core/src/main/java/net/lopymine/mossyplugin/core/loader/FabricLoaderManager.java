package net.lopymine.mossyplugin.core.loader;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.LoomManager;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.PluginContainer;
import org.jetbrains.annotations.NotNull;

public class FabricLoaderManager implements LoaderManager {

	private static final FabricLoaderManager INSTANCE = new FabricLoaderManager();

	public static FabricLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		plugins.apply("fabric-loom");
	}

	@Override
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension) {
		Project project = data.project();
		String minecraft = extension.getMinecraft();
		String mappings = extension.getMappings();
		String fabricApi = extension.getFabricApi();
		String fabricLoader = extension.getFabricLoader();

		DependencyHandler dependencies = project.getDependencies();
		dependencies.add("minecraft", "com.mojang:minecraft:%s".formatted(minecraft));
		if ("official".equals(mappings)) {
			dependencies.add("mappings", ((LoomGradleExtensionAPI) project.getExtensions().getByName("loom")).officialMojangMappings());
		} else {
			dependencies.add("mappings", "net.fabricmc:yarn:%s:v2".formatted(mappings));
		}
		dependencies.add("modImplementation", "net.fabricmc.fabric-api:fabric-api:%s".formatted(fabricApi));
		dependencies.add("modImplementation", "net.fabricmc:fabric-loader:%s".formatted(fabricLoader));
	}

	@Override
	public void configureExtensions(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().configure(LoomGradleExtensionAPI.class, (loom) -> {
			LoomManager.apply(data, loom);
		});
	}

	@Override
	public String getModDependenciesImplementationMethod() {
		return "modImplementation";
	}

	@Override
	public String getJarTaskName() {
		return "remapJar";
	}

	@Override
	public String getAWFileExtension() {
		return "accesswidener";
	}

	@Override
	public boolean excludeUselessFiles(FileCopyDetails details) {
		System.out.println(details.getPath());
		if (details.getPath().startsWith("META-INF")) {
			details.exclude();
			return true;
		}
		return false;
	}
}
