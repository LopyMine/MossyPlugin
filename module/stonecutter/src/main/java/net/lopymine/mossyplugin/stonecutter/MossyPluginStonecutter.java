package net.lopymine.mossyplugin.stonecutter;

import dev.kikugie.stonecutter.controller.StonecutterControllerExtension;
import dev.kikugie.stonecutter.data.StonecutterProject;
import java.util.*;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.stonecutter.tasks.*;
import org.gradle.*;
import org.gradle.api.*;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

public class MossyPluginStonecutter implements Plugin<Project> {

	@Override
	public void apply(@NotNull Project project) {
		Map<String, Project> childProjects = project.getChildProjects();
		TaskContainer tasks = project.getTasks();
		StonecutterControllerExtension controller = project.getExtensions().getByType(StonecutterControllerExtension.class);

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

			List<String> publishTasks = List.of("publishModrinth", "publishCurseforge");

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

		project.getGradle().addBuildListener(new BuildListener() {
			@Override
			public void settingsEvaluated(@NotNull Settings settings) {

			}

			@Override
			public void projectsLoaded(@NotNull Gradle gradle) {

			}

			@Override
			public void projectsEvaluated(@NotNull Gradle gradle) {
				for (Task task : tasks) {
					if (!"stonecutter".equals(task.getGroup())) {
						continue;
					}
					task.setGroup("aa-mossy-stonecutter");
				}

				Map<String, Map<String, List<StonecutterProject>>> loaderAndTheRepoAndProject = new HashMap<>();

				loaderAndProjects.forEach((loader, projects) -> {
					Map<String, List<StonecutterProject>> repoAndProjects = new HashMap<>();

					projects.forEach((pr) -> {
						RepositoryHandler repositories = childProjects.get(pr.getProject()).getExtensions().getByType(PublishingExtension.class).getRepositories();
						for (String repository : repositories.getNames()) {
							List<StonecutterProject> list = repoAndProjects.computeIfAbsent(repository, (key) -> new ArrayList<>());
							list.add(pr);
						}
					});

					loaderAndTheRepoAndProject.put(loader, repoAndProjects);
				});

				List<TaskProvider<?>> list = new ArrayList<>();
				loaderAndTheRepoAndProject.forEach((loader, repoAndProjects) -> {
					repoAndProjects.forEach((repository, projects) -> {
						tasks.register("publishMaven+%s+%s+All".formatted(loader, repository), (task) -> {
							configurePublishAllTaskWithRightOrder(projects, List.of("publishMossyPluginPublicationTo%sRepository".formatted(repository)), task, controller, childProjects);
							task.setGroup("ad-mossy-maven-%s".formatted(loader));
						});
					});

					if (!repoAndProjects.isEmpty()) {
						TaskProvider<Task> registered = tasks.register("publishMaven+%s+All".formatted(loader), (task) -> {
							for (String repository : repoAndProjects.keySet()) {
								task.dependsOn("publishMaven+%s+%s+All".formatted(loader, repository));
							}
							task.setGroup("ad-mossy-maven-%s".formatted(loader));
						});
						list.add(registered);
					}
				});

				if (!list.isEmpty()) {
					tasks.register("publishMaven+All", (task) -> {
						for (TaskProvider<?> taskProvider : list) {
							task.dependsOn(taskProvider);
						}
						task.setGroup("aa-mossy-main");
					});
				}
			}

			@Override
			public void buildFinished(@NotNull BuildResult result) {

			}
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
				if (version.contains("forge")) {
					task.delete(childProjects.get(version).file("build/moddev"));
				} else {
					task.delete(project.file(".idea/runConfigurations/Minecraft_Client___%s__%s.xml".formatted(version.replace(".", "_"), version)));
					task.delete(project.file(".idea/runConfigurations/Minecraft_Server___%s__%s.xml".formatted(version.replace(".", "_"), version)));
				}
			}

			for (String version : list) {
				if (version.contains("forge")) {
					task.finalizedBy(":%s:createLaunchScripts".formatted(version));
				} else {
					task.finalizedBy(":%s:ideaSyncTask".formatted(version));
				}
			}
		});
	}

	private static void configurePublishAllTaskWithRightOrder(List<StonecutterProject> projects, List<String> publishTasks, Task task, StonecutterControllerExtension controller, Map<String, Project> childProjects) {
		List<StonecutterProject> versions =
				projects.size() == 1 ?
						projects
						:
						projects.stream()
							.sorted((a, b) -> controller.compare(a.getVersion(), b.getVersion()))
							.toList();

		for (String publishTask : publishTasks) {
			if (versions.size() == 1) {
				task.dependsOn(childProjects.get(versions.get(0).getProject()).getTasks().named(publishTask));
				continue;
			}
			for (int i = 1; i < versions.size(); i++) {
				StonecutterProject first = versions.get(i - 1);
				StonecutterProject second = versions.get(i);

				TaskProvider<Task> firstTask = childProjects.get(first.getProject()).getTasks().named(publishTask);
				TaskProvider<Task> secondTask = childProjects.get(second.getProject()).getTasks().named(publishTask);
				task.dependsOn(firstTask, secondTask);

				secondTask.configure((t) -> t.setMustRunAfter(List.of(firstTask)));
			}
		}
	}

	public static List<String> getVersionsSpecifications(@NotNull Project project, String loader) {
		return Arrays.stream(MossyUtils.getProperty(project, "%s.versions_specifications".formatted(loader))
				.split(" "))
				.map((version) -> MossyUtils.substringBefore(version, "["))
				.toList();
	}

}
