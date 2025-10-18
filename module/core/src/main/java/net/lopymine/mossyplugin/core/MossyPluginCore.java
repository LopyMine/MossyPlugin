package net.lopymine.mossyplugin.core;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import lombok.Getter;
import me.modmuss50.mpp.ModPublishExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.task.RemapJarTask;
import net.lopymine.mossyplugin.common.*;
import net.lopymine.mossyplugin.core.manager.*;
import net.lopymine.mossyplugin.core.tasks.*;
import net.lopymine.mossyplugin.core.util.MultiVersion;
import org.gradle.api.*;
import org.gradle.api.plugins.*;
import org.gradle.api.tasks.*;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

@Getter
public class MossyPluginCore implements Plugin<Project> {

	public static final MossyLogger LOGGER = new MossyLogger("Core");

	private MultiVersion projectMultiVersion;
	private int javaVersionIndex;
	private JavaVersion javaVersion;

	@Override
	public void apply(@NotNull Project project) {
		LOGGER.setup(project);

		//

		PluginContainer plugins = project.getPlugins();
		plugins.apply("dev.kikugie.stonecutter");
		plugins.apply("fabric-loom");
		plugins.apply("me.modmuss50.mod-publish-plugin");
		plugins.apply("dev.kikugie.fletching-table");

		//

		this.projectMultiVersion = MossyPluginCore.getProjectMultiVersion(project);
		this.javaVersionIndex = MossyPluginCore.getJavaVersion(project);
		this.javaVersion = JavaVersion.toVersion(this.javaVersionIndex);

		//

		MossyPluginCore.configureProject(project, this);

		JavaManager.apply(project,this);
		J52JManager.apply(project);
		ProcessResourcesManager.apply(project, this);

		DependenciesManager.apply(project);
		StonecutterManager.apply(project, this);

		//

		MossyPluginCore.configureExtensions(project, this);
		MossyPluginCore.configureTasks(project, this);

		LOGGER.log("Project Version: %s", project.getVersion());
		LOGGER.log("Java Version: %s", this.javaVersionIndex);
	}

	private static void configureExtensions(@NotNull Project project, MossyPluginCore plugin) {
		project.getExtensions().configure(LoomGradleExtensionAPI.class, (loom) -> {
			LoomManager.apply(project, plugin, loom);
		});

		project.getExtensions().configure(ModPublishExtension.class, (mpe) -> {
			ModPublishManager.apply(project, plugin, mpe);
		});
	}

	private static void configureTasks(@NotNull Project project, MossyPluginCore plugin) {
		project.getTasks().register("generatePublishWorkflowsForEachVersion", GeneratePublishWorkflowsForEachVersionTask.class, (task) -> {
			task.setGroup("mossy");
		});
		project.getTasks().register("generatePersonalProperties", GeneratePersonalPropertiesTask.class, (task) -> {
			task.setGroup("mossy");
		});
		project.getTasks().register("regenerateRunConfigurations", Delete.class, (task) -> {
			task.setGroup("mossy");
			String version = plugin.getProjectMultiVersion().projectVersion();
			task.delete(getRootFile(project, ".idea/runConfigurations/Minecraft_Client___%s__%s.xml".formatted(version.replace(".", "_"), version)));
			task.delete(getRootFile(project, ".idea/runConfigurations/Minecraft_Server___%s__%s.xml".formatted(version.replace(".", "_"), version)));
			task.finalizedBy("ideaSyncTask");
		});
		project.getTasks().register("rebuildLibs", Delete.class, task -> {
			task.setGroup("build");
			String modName = MossyUtils.getProperty(project, "data.mod_name").replace(" ", "");
			String version = project.getVersion().toString();

			String jarFileName = "libs/%s-%s.jar".formatted(modName, version);
			String sourcesJarFileName = "libs/%s-%s-sources.jar".formatted(modName, version);

			task.delete(getRootFile(project, jarFileName));
			task.delete(project.getLayout().getBuildDirectory().file(jarFileName));
			task.delete(project.getLayout().getBuildDirectory().file(sourcesJarFileName));
		});
		project.getTasks().named("build", task -> {
			task.mustRunAfter("rebuildLibs");
		});
		project.getTasks().register("buildAndCollect", Copy.class, task -> {
			task.setGroup("build");
			task.dependsOn("rebuildLibs", "build");
			task.from(((RemapJarTask) project.getTasks().getByName("remapJar")).getArchiveFile().get());
			task.into(getRootFile(project, "libs/"));
		});
		for (String publishTask : List.of("publishModrinth", "publishCurseforge")) {
			project.getTasks().named(publishTask).configure((task) -> {
				task.doLast((t) -> {
					try {
						Thread.sleep(1000L);
					} catch (Exception e) {
						MossyPluginCore.LOGGER.log("Failed to wait before publishing!");
						e.printStackTrace();
					}
				});
			});
		}
	}

	private static void configureProject(@NotNull Project project, MossyPluginCore plugin) {
		String projectVersion = plugin.getMossyProjectVersion(project);
		String mavenGroup = MossyUtils.getProperty(project, "data.mod_maven_group");
		project.setVersion(projectVersion);
		project.setGroup(mavenGroup);

		BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
		base.getArchivesName().set(MossyUtils.getProperty(project, "data.mod_name").replace(" ", ""));

		Jar jar = (Jar) project.getTasks().getByName("jar");
		jar.getArchiveBaseName().set(base.getArchivesName().get());
		jar.from(getRootFile(project, "LICENSE"), (spec) -> {
			spec.rename(s -> "%s_%s".formatted(s, base.getArchivesName().get()));
		});
	}

	public static int getJavaVersion(Project project) {
		String currentMCVersion = getCurrentMCVersion(project);
		StonecutterBuildExtension stonecutter = getStonecutter(project);
		return stonecutter.compare("1.20.5", currentMCVersion) == 1 ?
				stonecutter.compare("1.18", currentMCVersion) == 1 ?
						stonecutter.compare("1.16.5", currentMCVersion) == 1 ?
								8
								:
								16
						:
						17
				:
				21;
	}


	public static MultiVersion getProjectMultiVersion(@NotNull Project currentProject) {
		String currentMCVersion = getCurrentMCVersion(currentProject);

		String[] versions = MossyUtils.getProperty(currentProject, "versions_specifications").split(" ");
		for (String version : versions) {
			String[] split = version.substring(0, version.length()-1).split("\\[");
			String project = split[0];
			if (Objects.equals(project, currentMCVersion)) {
				String supportedVersionsString = split[1];
				if (supportedVersionsString.contains("-")) {
					String[] supportedVersions = supportedVersionsString.split("-");
					return new MultiVersion(currentMCVersion, supportedVersions[0], supportedVersions[1]);
				} else if (supportedVersionsString.contains(".")) {
					return new MultiVersion(currentMCVersion, currentMCVersion, supportedVersionsString);
				} else {
					int a = project.indexOf(".");
					int i = project.lastIndexOf(".");
					if (a == i) {
						i = project.length();
					}
					String p = project.substring(0, i);
					String supportedMaxVersion = "%s.%s".formatted(p, supportedVersionsString);
					return new MultiVersion(currentMCVersion, currentMCVersion, supportedMaxVersion);
				}
			}
		}
		return new MultiVersion(currentMCVersion, currentMCVersion, currentMCVersion);
	}

	public static Properties getPersonalProperties(@NotNull Project project) {
		File file = project.getRootProject().file("personal/personal.properties");
		Properties personalProperties = new Properties();

		if (!file.exists()) {
			return personalProperties;
		}

		try (InputStream stream = new FileInputStream(file)) {
			personalProperties.load(stream);
		} catch (IOException e) {
			LOGGER.log("Something went wrong when parsing personal properties:");
			LOGGER.log(e.getMessage());
		}

		try {
			String mixinPath = "absolute_path_to_sponge_mixin";

			for (String line : Files.readAllLines(file.toPath())) {
				if (!line.startsWith(mixinPath)) {
					continue;
				}
				personalProperties.setProperty(mixinPath, line.substring(mixinPath.length() + 1));
			}
		} catch (Exception e) {
			LOGGER.log("Something went wrong when parsing personal properties mixin path:");
			LOGGER.log(e.getMessage());
		}

		return personalProperties;
	}

	public static Map<String, String> getMossyProperties(Project project, String prefix) {
		HashMap<String, String> dependencies = new HashMap<>();

		Map<String, ?> properties = project.getProperties();
		for (Entry<String, ?> entry : properties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (!key.startsWith(prefix + ".")) {
				continue;
			}
			dependencies.put(MossyUtils.substringSince(key, "."), value.toString());
		}

		return dependencies;
	}

	public static String getCurrentMCVersion(@NotNull Project project) {
		return getStonecutter(project).getCurrent().getProject();
	}

	public static @NotNull StonecutterBuildExtension getStonecutter(@NotNull Project project) {
		return (StonecutterBuildExtension) project.getExtensions().getByName("stonecutter");
	}

	public static String[] getMultiVersions(@NotNull Project project) {
		return MossyUtils.getProperty(project, "multi_versions").split(" ");
	}

	public static String getProperty(Project project, String id) {
		return MossyUtils.getProperty(project, id);
	}

	public String getMossyProjectVersion(Project project) {
		String modVersion = MossyUtils.getProperty(project, "data.mod_version");
		MultiVersion multiVersion = this.getProjectMultiVersion();
		return "%s+%s".formatted(modVersion, multiVersion.projectVersion());
	}

	public static File getRootFile(@NotNull Project project, String path) {
		return project.getRootProject().file(path);
	}

}
