package net.lopymine.mossyplugin.core.loader;

import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.neoforge.NeoForgeManager;
import net.neoforged.moddevgradle.legacyforge.dsl.*;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class ForgeLoaderManager implements LoaderManager {

	private static final ForgeLoaderManager INSTANCE = new ForgeLoaderManager();

	public static ForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		plugins.apply("net.neoforged.moddev.legacyforge");
	}

	@Override
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension dependencies) {
		Project project = data.project();
		ExtensionContainer extensions = project.getExtensions();
		LegacyForgeExtension extension = extensions.getByType(LegacyForgeExtension.class);
		extension.setVersion(dependencies.getForge());
		NeoForgeManager.apply(data, extension, dependencies);

		String mixinExtrasVersion = project.getProperty("base.mixinextras_version");
		String mixinVersion = project.getProperty("base.mixin_version");

		DependencyHandler deps = project.getDependencies();
		deps.add("annotationProcessor", "io.github.llamalad7:mixinextras-common:%s".formatted(mixinExtrasVersion));
		deps.add("implementation", "io.github.llamalad7:mixinextras-common:%s".formatted(mixinExtrasVersion));

		deps.add("jarJar", "io.github.llamalad7:mixinextras-forge:%s".formatted(mixinExtrasVersion));
		deps.add("implementation", "io.github.llamalad7:mixinextras-forge:%s".formatted(mixinExtrasVersion));

		deps.add("annotationProcessor", "org.spongepowered:mixin:%s:processor".formatted(mixinVersion));

		this.configureMixins(extensions, project);
	}

	private void configureMixins(ExtensionContainer extensions, Project project) {
		MixinExtension mixin = extensions.getByType(MixinExtension.class);
		JavaPluginExtension java = extensions.getByType(JavaPluginExtension.class);
		String modId = project.getProperty("data.mod_id");

		List<String> registeredMixinConfigs = new ArrayList<>();

		mixin.add(java.getSourceSets().getByName("main"), "%s.refmap.json".formatted(modId));
		String mainMixin = "%s.mixins.json".formatted(modId);

		mixin.config(mainMixin);
		registeredMixinConfigs.add(mainMixin);

		String additionalMixinConfigIds = project.getProperty("data.mixin_configs");
		if (!additionalMixinConfigIds.equals("none")) {
			String[] mixins = additionalMixinConfigIds.split(" ");
			for (String mixinConfig : mixins) {
				String id = "%s-%s.mixins.json".formatted(modId, mixinConfig);

				mixin.config(id);
				registeredMixinConfigs.add(id);
			}
		}

		String mixinConfigs = String.join(",", registeredMixinConfigs);
		Jar jar = (Jar) project.getTasks().getByName("jar");
		jar.getManifest().getAttributes().put("MixinConfigs", mixinConfigs);
	}

	@Override
	public void configureExtensions(@NotNull MossyProjectConfigurationData data) {
		data.project().getTasks().getByName("jar").finalizedBy(this.getJarTaskName());
		for (JavaCompile compile : data.project().getTasks().withType(JavaCompile.class)) {
			compile.getOptions().getCompilerArgs().add("-Xlint:-removal");
			compile.getOptions().getCompilerArgs().add("-Xlint:-deprecation");
		}

		data.project().afterEvaluate((project) -> {
			project.getTasks().named("createMinecraftArtifacts").configure((task) -> {
				task.dependsOn(":%s:stonecutterGenerate".formatted(data.projectName()));
			});
		});
	}

	@Override
	public String getModDependenciesImplementationMethod() {
		return "modImplementation";
	}

	@Override
	public String getJarTaskName() {
		return "reobfJar";
	}

	@Override
	public String getAWFileExtension() {
		return "cfg";
	}

	@Override
	public boolean excludeUselessFiles(FileCopyDetails details) {
		boolean excluded = false;
		for (String file : List.of("fabric.mod.json", "neoforge.mods.toml")) {
			if (details.getName().equals(file)) {
				details.exclude();
				excluded = true;
			}
		}
		return excluded;
	}
}
