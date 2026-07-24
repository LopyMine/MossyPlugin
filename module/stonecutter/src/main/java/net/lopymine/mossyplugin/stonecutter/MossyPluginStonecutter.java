package net.lopymine.mossyplugin.stonecutter;

import dev.kikugie.stonecutter.controller.StonecutterControllerExtension;
import dev.kikugie.stonecutter.data.StonecutterProject;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.stonecutter.tasks.*;
import org.gradle.api.*;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyUtils.class)
public class MossyPluginStonecutter implements Plugin<Project> {

	@Override
	public void apply(@NotNull Project project) {
		Map<String, Project> childProjects = project.getChildProjects();
		TaskContainer tasks = project.getTasks();
		StonecutterControllerExtension controller = project.getExtensions().getByType(StonecutterControllerExtension.class);

		String ciLoader = project.getProviders().gradleProperty("ci_loader").getOrNull();
		if (ciLoader == null) {
			File file = project.file("versions/active.txt");

			if (!file.exists()) {
				try {
					@SuppressWarnings("unused")
					boolean unused = file.createNewFile();
					Files.write(file.toPath(), controller.getVcsVersion().getProject().getBytes());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			controller.active(file);
		} else {
			controller.active(null);
		}

		Map<String, List<StonecutterProject>> loaderAndProjects = new HashMap<>();

		for (StonecutterProject version : controller.getVersions()) {
			String loader = MossyUtils.substringBefore(version.getProject(), "-");
			List<StonecutterProject> projects = loaderAndProjects.computeIfAbsent(loader, (key) -> new ArrayList<>());
			projects.add(version);
		}

		loaderAndProjects.forEach((loader, projects) -> {
			//

			projects.forEach((version) -> {
				tasks.register("buildAndCollect+%s+%s".formatted(loader, version.getVersion()), (task) -> {
					task.dependsOn(":%s:buildAndCollect".formatted(version.getProject()));
					task.setGroup("ab-mossy-build-%s".formatted(loader));
				});
			});

			tasks.register("buildAndCollect+%s+All".formatted(loader), (task) -> {
				projects.forEach((version) -> {
					task.dependsOn(":%s:buildAndCollect".formatted(version.getProject()));
				});
				task.setGroup("ab-mossy-build-%s".formatted(loader));
			});

			tasks.register("buildAndCollect+%s+Specified".formatted(loader), (task) -> {
				List<String> versionsSpecifications = getVersionsSpecifications(project, loader);
				projects.forEach((version) -> {
					if (!versionsSpecifications.contains(version.getVersion())) {
						return;
					}
					task.dependsOn(":%s:buildAndCollect".formatted(version.getProject()));
				});
				task.setGroup("ab-mossy-build-%s".formatted(loader));
			});

			//

			List<String> publishTasks = new ArrayList<>();

			String modrinthId = project.getProperty("modrinth_id");
			String curseForgeId = project.getProperty("curseforge_id");

			if (!modrinthId.equals("none")) {
				publishTasks.add("publishModrinth");
			}
			if (!curseForgeId.equals("none")) {
				publishTasks.add("publishCurseforge");
			}

			projects.forEach((version) -> {
				tasks.register("publish+%s+%s".formatted(loader, version.getVersion()), (task) -> {
					task.dependsOn(":%s:publishMods".formatted(version.getProject()));
					task.setGroup("ac-mossy-release-%s".formatted(loader));
				});
			});

			tasks.register("publish+%s+All".formatted(loader), (task) -> {
				configurePublishAllTaskWithRightOrder(projects, publishTasks, task, controller, childProjects);
				task.setGroup("ac-mossy-release-%s".formatted(loader));
			});

			tasks.register("publish+%s+Specified".formatted(loader), (task) -> {
				List<StonecutterProject> list = new ArrayList<>();
				List<String> versionsSpecifications = getVersionsSpecifications(project, loader);
				projects.forEach((pr) -> {
					if (!versionsSpecifications.contains(pr.getVersion())) {
						return;
					}
					list.add(pr);
				});
				configurePublishAllTaskWithRightOrder(list, publishTasks, task, controller, childProjects);
				task.setGroup("ac-mossy-release-%s".formatted(loader));
			});

			//
		});

		tasks.register("buildAndCollect+All", (task) -> {
			for (String loader : loaderAndProjects.keySet()) {
				task.dependsOn("buildAndCollect+%s+All".formatted(loader));
			}
			task.setGroup("aa-mossy-main");
		});

		tasks.register("buildAndCollect+Specified", (task) -> {
			for (String loader : loaderAndProjects.keySet()) {
				task.dependsOn("buildAndCollect+%s+Specified".formatted(loader));
			}
			task.setGroup("aa-mossy-main");
		});

		tasks.register("publish+All", (task) -> {
			for (String loader : loaderAndProjects.keySet()) {
				task.dependsOn("publish+%s+All".formatted(loader));
			}
			task.setGroup("aa-mossy-main");
		});

		tasks.register("publish+Specified", (task) -> {
			for (String loader : loaderAndProjects.keySet()) {
				task.dependsOn("publish+%s+Specified".formatted(loader));
			}
			task.setGroup("aa-mossy-main");
		});

		tasks.configureEach((task) -> {
			if (!"stonecutter".equals(task.getGroup())) {
				return;
			}
			task.setGroup("aa-mossy-stonecutter");
		});

		Set<String> seenRepoAll = new HashSet<>();
		Set<String> seenLoaderAll = new HashSet<>();
		boolean[] seenAll = {false};

		loaderAndProjects.forEach((loader, projects) -> {
			projects.forEach((version) -> {
				Project child = childProjects.get(version.getProject());
				child.getPluginManager().withPlugin("maven-publish", (applied) -> child.afterEvaluate((cp) -> {
					PublishingExtension publishing = cp.getExtensions().findByType(PublishingExtension.class);
					if (publishing == null) {
						return;
					}
					for (String repository : publishing.getRepositories().getNames()) {
						registerPublishMavenTasks(tasks, loader, repository, projects, controller, childProjects, seenRepoAll, seenLoaderAll, seenAll);
					}
				}));
			});
		});

		project.getTasks().register("generatePublishWorkflowsForEachVersion", GeneratePublishWorkflowsForEachVersionTask.class, (task) -> {
			task.setGroup("aa-mossy-project");
			List<String> list = controller.getVersions().stream().map(StonecutterProject::getProject).toList();
			task.setMultiVersions(list);
		});
		project.getTasks().register("generatePersonalProperties", GeneratePersonalPropertiesTask.class, (task) -> {
			task.setGroup("aa-mossy-project");
		});
		project.getTasks().register("updateRunConfigurations", Delete.class, (task) -> {
			task.setGroup("aa-mossy-project");

			List<String> list = controller.getVersions().stream().map(StonecutterProject::getProject).toList();
			for (String version : list) {
				if (MossyUtils.isOldNeoForgeProject(version)) {
					continue;
				}
				if (version.contains("forge")) {
					task.delete(childProjects.get(version).file("build/moddev"));
				} else {
					String d = version.replace("fabric-", "");
					String s = d.replace(".", "_")
							.replace("-", "_");
					String formatted = ".idea/runConfigurations/fabric___%s___server_fabric-%s.xml".formatted(s, d);
					task.delete(project.file(formatted));
					String formatted1 = ".idea/runConfigurations/fabric___%s___client_fabric-%s.xml".formatted(s, d);
					task.delete(project.file(formatted1));
				}
			}

			for (String version : list) {
				if (MossyUtils.isOldNeoForgeProject(version)) {
					task.finalizedBy(":%s:idePostSync".formatted(version));
				} else if (version.contains("forge")) {
					task.finalizedBy(":%s:createLaunchScripts".formatted(version));
				} else {
					task.finalizedBy(":%s:ideaSyncTask".formatted(version));
				}
			}
		});

		project.getTasks().register("cleanTestAssets", Delete.class, (task) -> {
			task.setGroup("aa-mossy-project");
			for (StonecutterProject version : controller.getVersions()) {
				task.delete(childProjects.get(version.getProject()).file("tests"));
			}
		});
	}

	private static void registerPublishMavenTasks(
			TaskContainer tasks,
			String loader,
			String repository,
			List<StonecutterProject> projects,
			StonecutterControllerExtension controller,
			Map<String, Project> childProjects,
			Set<String> seenRepoAll,
			Set<String> seenLoaderAll,
			boolean[] seenAll
	) {
		if (!seenRepoAll.add("%s+%s".formatted(loader, repository))) {
			return;
		}

		String repoAll = "publishMaven+%s+%s+All".formatted(loader, repository);
		tasks.register(repoAll, (task) -> {
			configurePublishAllTaskWithRightOrder(projects, List.of("publishMossyPluginPublicationTo%sRepository".formatted(repository)), task, controller, childProjects);
			task.setGroup("ad-mossy-maven-%s".formatted(loader));
		});

		String loaderAll = "publishMaven+%s+All".formatted(loader);
		if (seenLoaderAll.add(loader)) {
			tasks.register(loaderAll, (task) -> task.setGroup("ad-mossy-maven-%s".formatted(loader)));

			if (!seenAll[0]) {
				seenAll[0] = true;
				tasks.register("publishMaven+All", (task) -> task.setGroup("aa-mossy-main"));
			}
			tasks.named("publishMaven+All").configure((task) -> task.dependsOn(loaderAll));
		}

		tasks.named(loaderAll).configure((task) -> task.dependsOn(repoAll));
	}

	private static void configurePublishAllTaskWithRightOrder(List<StonecutterProject> projects, List<String> publishTasks, Task task, StonecutterControllerExtension controller, Map<String, Project> childProjects) {
		List<StonecutterProject> versions =
				projects.size() == 1 ?
						projects
						:
						projects.stream()
							.sorted((a, b) -> controller.compare(a.getVersion(), b.getVersion()))
							.toList();

		// task paths keep this lazy, resolving them through another project's task container would realize it
		for (String publishTask : publishTasks) {
			if (versions.size() == 1) {
				task.dependsOn(getTaskPath(versions.get(0), publishTask));
				continue;
			}
			for (int i = 1; i < versions.size(); i++) {
				StonecutterProject second = versions.get(i);

				String firstPath = getTaskPath(versions.get(i - 1), publishTask);
				String secondPath = getTaskPath(second, publishTask);
				task.dependsOn(firstPath, secondPath);

				childProjects.get(second.getProject()).getTasks().named(publishTask).configure((t) -> t.mustRunAfter(firstPath));
			}
		}
	}

	private static @NotNull String getTaskPath(@NotNull StonecutterProject project, String taskName) {
		return ":%s:%s".formatted(project.getProject(), taskName);
	}

	public static List<String> getVersionsSpecifications(@NotNull Project project, String loader) {
		return Arrays.stream(MossyUtils.getProperty(project, "%s.versions_specifications".formatted(loader))
				.split(" "))
				.map((version) -> MossyUtils.substringBefore(version, "["))
				.toList();
	}

}
