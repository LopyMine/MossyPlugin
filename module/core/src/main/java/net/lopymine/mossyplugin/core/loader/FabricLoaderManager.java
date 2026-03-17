package net.lopymine.mossyplugin.core.loader;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import lombok.experimental.ExtensionMethod;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.fabric.LoomManager;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.PluginContainer;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class FabricLoaderManager implements LoaderManager {

	private static final FabricLoaderManager INSTANCE = new FabricLoaderManager();

	public static FabricLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		if (isRemapVersion(data)) {
			plugins.apply("fabric-loom");
		} else {
			plugins.apply("net.fabricmc.fabric-loom");
		}
	}

	@Override
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension) {
		Project project = data.project();
		String minecraft = extension.getMinecraft();
		String fabricApi = extension.getFabricApi();
		String fabricLoader = extension.getFabricLoader();

		DependencyHandler dependencies = project.getDependencies();
		dependencies.add("minecraft", "com.mojang:minecraft:%s".formatted(minecraft));

		if (isRemapVersion(data)) {
			dependencies.add("mappings", ((LoomGradleExtensionAPI) project.getExtensions().getByName("loom")).officialMojangMappings());
		}

		dependencies.add(this.getModDependenciesImplementationMethod(data), "net.fabricmc.fabric-api:fabric-api:%s".formatted(fabricApi));
		dependencies.add(this.getModDependenciesImplementationMethod(data), "net.fabricmc:fabric-loader:%s".formatted(fabricLoader));
	}

	@Override
	public void configureExtensions(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().configure(LoomGradleExtensionAPI.class, (loom) -> {
			LoomManager.apply(data, loom);
		});
	}

	@Override
	public String getModDependenciesImplementationMethod(MossyProjectConfigurationData data) {
		if (!isRemapVersion(data)) {
			return "implementation";
		}
		return "modImplementation";
	}

	@Override
	public String getJarTaskName(MossyProjectConfigurationData data) {
		if (!isRemapVersion(data)) {
			return "jar";
		}
		return "remapJar";
	}

	@Override
	public String getAWFileExtension(MossyProjectConfigurationData data) {
		if (!isRemapVersion(data)) {
			return "classTweaker";
		}
		return "accesswidener";
	}

	@Override
	public boolean excludeUselessFiles(FileCopyDetails details) {
		if (details.getName().contains("mods.toml")) {
			details.exclude();
			return true;
		}
		return false;
	}

	private static boolean isRemapVersion(MossyProjectConfigurationData data) {
		StonecutterBuildExtension stonecutter = data.project().getStonecutter();
		return stonecutter.eval(data.comparableMinecraftVersion(), "<26.1");
	}
}
