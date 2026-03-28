package net.lopymine.mossyplugin.core.loader;

import java.lang.reflect.Method;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.neoforge.NeoForgeManager;
import net.neoforged.moddevgradle.legacyforge.dsl.*;
import net.neoforged.moddevgradle.legacyforge.internal.MinecraftMappings;
import org.gradle.api.*;
import org.gradle.api.artifacts.*;
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

		if (!"true".equals(dependencies.getDisableMixinAp())) {
			deps.add("annotationProcessor", "org.spongepowered:mixin:%s:processor".formatted(mixinVersion));
		}

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
		data.project().getTasks().getByName("jar").finalizedBy(this.getJarTaskName(data));
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
	public String getModDependenciesImplementationMethod(MossyProjectConfigurationData data) {
		return "modImplementation";
	}

	@Override
	public String getJarTaskName(MossyProjectConfigurationData data) {
		return "reobfJar";
	}

	@Override
	public String getAWFileExtension(MossyProjectConfigurationData data) {
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

	@Override
	public Map<String, String> getLoaderConfigurations(List<String> configurations, MossyProjectConfigurationData data) {

		Map<String, String> map = new HashMap<>();
		for (String s : configurations) {
			if (s.equals("include")) {
				map.put(s, "jarJar");
				continue;
			}
			map.put(s, "mod" + String.valueOf(s.charAt(0)).toUpperCase(Locale.ROOT) + s.substring(1));
		}
		return map;
	}


	@SuppressWarnings("UnstableApiUsage")
	@Override
	public Configuration registerCustomConfiguration(@NotNull MossyProjectConfigurationData data, String name, String originalName, String loaderName) {
		Project project = data.project();
		Configuration parent = project.getConfigurations().getByName(loaderName);
		MinecraftMappings namedMappings = project.getObjects().named(MinecraftMappings.class, "named");

		return project.getConfigurations().create(name, (spec) -> {
			spec.setDescription("Configuration for dependencies that needs to be remapped");
			spec.setCanBeConsumed(false);
			spec.setCanBeResolved(false);
			spec.setTransitive(false);
			spec.withDependencies((dependencies) -> dependencies.forEach((dep) -> {
				switch (dep) {
					case ExternalModuleDependency externalModuleDependency -> {
						project.getDependencies().constraints((constraints) -> {
							String var10001 = parent.getName();
							String var10002 = externalModuleDependency.getGroup();
							constraints.add(var10001, var10002 + ":" + externalModuleDependency.getName() + ":" + externalModuleDependency.getVersion(), (c) -> c.attributes((a) -> a.attribute(MinecraftMappings.ATTRIBUTE, namedMappings)));
						});
						externalModuleDependency.setTransitive(false);
					}
					case FileCollectionDependency fileCollectionDependency -> project.getDependencies().constraints((constraints) -> constraints.add(parent.getName(), fileCollectionDependency.getFiles(), (c) -> c.attributes((a) -> a.attribute(MinecraftMappings.ATTRIBUTE, namedMappings))));
					case ProjectDependency projectDependency -> {
						project.getDependencies().constraints((constraints) -> constraints.add(parent.getName(), getProjectDependencyProject(project, projectDependency), (c) -> c.attributes((a) -> a.attribute(MinecraftMappings.ATTRIBUTE, namedMappings))));
						projectDependency.setTransitive(false);
					}
					default -> throw new IllegalStateException("Unexpected value: " + dep);
				}

			}));
		});
	}

	private static Project getProjectDependencyProject(Project project, ProjectDependency projectDependency) {
		try {
			Class<ProjectDependency> clazz = ProjectDependency.class;

			try {
				Method getPathMethod = clazz.getMethod("getPath");
				String path = (String)getPathMethod.invoke(projectDependency);
				return project.project(path);
			} catch (NoSuchMethodException var5) {
				@SuppressWarnings("all")
				Method getDependencyProjectMethod = clazz.getMethod("getDependencyProject");
				return (Project)getDependencyProjectMethod.invoke(projectDependency);
			}
		} catch (ReflectiveOperationException exception) {
			throw new RuntimeException("Failed to access project of ProjectDependency", exception);
		}
	}
}
