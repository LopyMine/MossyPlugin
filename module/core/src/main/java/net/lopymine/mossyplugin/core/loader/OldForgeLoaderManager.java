package net.lopymine.mossyplugin.core.loader;

import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.lopymine.mossyplugin.core.manager.forge.OldForgeManager;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import net.minecraftforge.gradle.userdev.jarjar.JarJarProjectExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.*;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.gradle.plugins.MixinExtension;

@ExtensionMethod(MossyPluginCore.class)
public class OldForgeLoaderManager implements LoaderManager {

	private static final OldForgeLoaderManager INSTANCE = new OldForgeLoaderManager();

	public static OldForgeLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		// ForgeGradle only brings "java", but the "api" configuration is expected to exist
		plugins.apply("java-library");
		plugins.apply("net.minecraftforge.gradle");
		plugins.apply("org.parchmentmc.librarian.forgegradle");
		plugins.apply("org.spongepowered.mixin");
	}

	@Override
	public void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension dependencies) {
		Project project = data.project();
		ExtensionContainer extensions = project.getExtensions();

		OldForgeManager.apply(data, dependencies);

		String mixinExtrasVersion = project.getProperty("base.mixinextras_version");
		String mixinVersion = project.getProperty("base.mixin_version");

		JarJarProjectExtension jarJar = extensions.getByType(JarJarProjectExtension.class);
		jarJar.enable();

		DependencyHandler deps = project.getDependencies();
		// "build.forge" already holds the full "<minecraft>-<forge>" artifact version
		deps.add("minecraft", "net.minecraftforge:forge:%s".formatted(dependencies.getForge()));

		deps.add("annotationProcessor", "io.github.llamalad7:mixinextras-common:%s".formatted(mixinExtrasVersion));
		deps.add("implementation", "io.github.llamalad7:mixinextras-common:%s".formatted(mixinExtrasVersion));

		Dependency mixinExtrasForge = deps.add("jarJar", "io.github.llamalad7:mixinextras-forge:%s".formatted(mixinExtrasVersion));
		if (mixinExtrasForge != null) {
			jarJar.ranged(mixinExtrasForge, "[%s,)".formatted(mixinExtrasVersion));
		}
		deps.add("implementation", "io.github.llamalad7:mixinextras-forge:%s".formatted(mixinExtrasVersion));

		if (!"true".equals(dependencies.getDisableMixinAp())) {
			deps.add("annotationProcessor", "org.spongepowered:mixin:%s:processor".formatted(mixinVersion));
		}

		this.configureMixins(extensions, project);
	}

	private void configureMixins(ExtensionContainer extensions, Project project) {
		MixinExtension  mixin = extensions.getByType(MixinExtension.class);
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
		Project project = data.project();
		TaskContainer tasks = project.getTasks();

		Jar jar = (Jar) tasks.getByName("jar");
		jar.getArchiveClassifier().set("slim");
		jar.finalizedBy("reobfJar");

		Jar jarJar = (Jar) tasks.getByName("jarJar");
		jarJar.getArchiveClassifier().set("");
		jarJar.finalizedBy("reobfJarJar");
		jarJar.from(project.getRootFile("LICENSE"), (spec) -> {
			spec.rename((s) -> "%s_%s".formatted(s, jarJar.getArchiveBaseName().get()));
		});
		tasks.getByName("assemble").dependsOn(jarJar);

		for (JavaCompile compile : tasks.withType(JavaCompile.class)) {
			compile.getOptions().getCompilerArgs().add("-Xlint:-removal");
			compile.getOptions().getCompilerArgs().add("-Xlint:-deprecation");
		}
	}

	@Override
	public String getModDependenciesImplementationMethod(MossyProjectConfigurationData data) {
		return "mossyImplementation";
	}

	@Override
	public String getJarTaskName(MossyProjectConfigurationData data) {
		return "jarJar";
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
			map.put(s, s.equals("include") ? "jarJar" : s);
		}
		return map;
	}

	@Override
	public Configuration registerCustomConfiguration(@NotNull MossyProjectConfigurationData data, String name, String originalName, String loaderName) {
		Project project = data.project();
		Configuration configuration = project.getConfigurations().create(name, (spec) -> {
			spec.setDescription("Configuration for dependencies that needs to be remapped");
		});

		if (originalName.equals("include")) {
			return configuration;
		}

		DependencyManagementExtension fg = project.getExtensions().getByType(DependencyManagementExtension.class);
		configuration.withDependencies((dependencies) -> {
			List<Dependency> original = new ArrayList<>(dependencies);
			dependencies.clear();
			for (Dependency dependency : original) {
				dependencies.add(fg.deobf(dependency));
			}
		});

		return configuration;
	}
}
