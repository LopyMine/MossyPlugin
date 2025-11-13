package net.lopymine.mossyplugin.core.manager;

import java.util.*;
import java.util.function.Consumer;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.*;
import net.lopymine.mossyplugin.core.extension.MossyCoreAdditionalDependencies.AdditionalDependencyOverride;
import net.lopymine.mossyplugin.core.loader.LoaderManager;
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension;
import org.gradle.api.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.repositories.*;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class DependenciesManager {

	private static void addDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension) {
		Project project = data.project();
		LoaderManager loaderManager = data.loaderManager();

		String minecraft = extension.getMinecraft();
		String lombok = extension.getLombok();

		DependencyHandler dependencies = project.getDependencies();
		loaderManager.applyDependencies(data, extension);
		dependencies.add("compileOnly", "org.projectlombok:lombok:%s".formatted(lombok));
		dependencies.add("annotationProcessor", "org.projectlombok:lombok:%s".formatted(lombok));

		Map<String, String> properties = project.getMossyProperties("dep");
		MossyCoreAdditionalDependencies additional = extension.getAdditional();
		additional.disable("yacl");

		String modImplementation = loaderManager.getModDependenciesImplementationMethod();

		Map<String, AdditionalDependencyOverride> overrides = additional.getOverrides();
		Set<String> disabled = additional.getDisabled();
		properties.forEach((modId, version) -> {
			if (disabled.contains(modId)) {
				return;
			}

			if (version.equals("unknown")) {
				return;
			}

			AdditionalDependencyOverride override = overrides.get(modId);
			String configurationName = override != null ? override.configurationName() : modImplementation;
			dependencies.add(configurationName, "maven.modrinth:%s:%s".formatted(modId, version));
		});

		project.getConfigurations().forEach((configuration) -> {
			configuration.resolutionStrategy((strategy) -> {
				strategy.force("com.twelvemonkeys.common:common-io:3.10.0");
				strategy.force("com.twelvemonkeys.common:common-lang:3.10.0");
				strategy.force("com.twelvemonkeys.common:common-image:3.10.0");
				strategy.force("com.twelvemonkeys.imageio:imageio-metadata:3.10.0");
				strategy.force("com.twelvemonkeys.imageio:imageio-webp:3.10.0");
				strategy.force("com.twelvemonkeys.imageio:imageio-core:3.10.0");
			});
		});

		String yaclVersion = properties.get("yacl");
		if (yaclVersion != null && !yaclVersion.equals("unknown")) {
			Set<String> oldMavenVersions = Set.of("1.19.4", "1.20", "1.20.2", "1.20.3");
			AdditionalDependencyOverride override = overrides.get("yacl");
			String configurationName = override != null ? override.configurationName() : modImplementation;
			if (oldMavenVersions.contains(minecraft)) {
				dependencies.add(configurationName, "dev.isxander.yacl:yet-another-config-lib-fabric:%s".formatted(MossyUtils.substringBeforeLast(yaclVersion, "-")));
			} else {
				dependencies.add(configurationName, "dev.isxander:yet-another-config-lib:%s".formatted(yaclVersion));
			}
		}
	}

	private static void addRepositories(Project project) {
		project.getRepositories().mavenCentral();
		addRepository(project, "Forge", "https://maven.minecraftforge.net");
		addRepository(project, "Minecraft libraries", "https://libraries.minecraft.net");
		addRepository(project, "Quilt", "https://maven.quiltmc.org/repository/release/");
		addRepository(project, "Sonatype", "https://oss.sonatype.org/content/repositories/snapshots/");
		addRepository(project, "Terraformers", "https://maven.terraformersmc.com/");
		addRepository(project, "YACL", "https://maven.isxander.dev/releases");
		addRepository(project, "Nucleoid", "https://maven.nucleoid.xyz/");
		addRepository(project, "Modrinth", "https://api.modrinth.com/maven", (repository) -> {
			repository.content((descriptor) -> {
				descriptor.includeGroup("maven.modrinth");
			});
		});
		addRepository(project, "Sponge", "https://repo.spongepowered.org/repository/maven-public", (repository) -> {
			project.getRepositories().exclusiveContent((content) -> {
				content.forRepositories(repository);
				@SuppressWarnings("all")
				ExclusiveContentRepository filter = content.filter((descriptor) -> {
					descriptor.includeGroupAndSubgroups("org.spongepowered");
				});
			});
		});
		addRepository(project, "YACL Kotlin For Forge", "https://thedarkcolour.github.io/KotlinForForge/", (repository) -> {
			project.getRepositories().exclusiveContent((content) -> {
				content.forRepositories(repository);
				@SuppressWarnings("all")
				ExclusiveContentRepository filter = content.filter((descriptor) -> {
					descriptor.includeGroup("thedarkcolour");
				});
			});
		});
	}

	private static void addRepository(Project project, String name, String url) {
		addRepository(project, name, url, (repository) -> {});
	}

	private static void addRepository(Project project, String name, String url, Consumer<MavenArtifactRepository> consumer) {
		project.getRepositories().maven((repository) -> {
			repository.setName(name);
			repository.setUrl(url);
			consumer.accept(repository);
		});
	}

	public static void apply(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().create("mossyDependencies", MossyCoreDependenciesExtension.class);

		addRepositories(project);
		project.getGradle().addProjectEvaluationListener(new ProjectEvaluationListener() {
			@Override
			public void beforeEvaluate(@NotNull Project project) {
			}

			@Override
			public void afterEvaluate(@NotNull Project project, @NotNull ProjectState state) {
				MossyCoreDependenciesExtension extension = project.getExtensions().getByType(MossyCoreDependenciesExtension.class);
				DependenciesManager.addDependencies(data, extension);
				project.getGradle().removeProjectEvaluationListener(this);
			}
		});
	}
}
