package net.lopymine.mossyplugin.stonecutter;

import dev.kikugie.stonecutter.controller.StonecutterControllerExtension;
import dev.kikugie.stonecutter.data.StonecutterProject;
import java.util.*;
import java.util.Map.Entry;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.stonecutter.tasks.*;
import org.gradle.*;
import org.gradle.api.*;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
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

		for (StonecutterProject version : controller.getVersions()) {
			tasks.register("buildAndCollect+%s".formatted(version.getProject()), (task) -> {
				task.dependsOn(":%s:buildAndCollect".formatted(version.getProject()));
				task.setGroup("mossy-build");
			});
		}

		for (StonecutterProject version : controller.getVersions()) {
			tasks.register("publish+%s".formatted(version.getProject()), (task) -> {
				task.dependsOn(":%s:publishMods".formatted(version.getProject()));
				task.setGroup("mossy-publish");
			});
		}

		tasks.register("buildAndCollect+All", (task) -> {
			controller.getVersions().forEach((version) -> {
				task.dependsOn(":%s:buildAndCollect".formatted(version.getProject()));
			});
			task.setGroup("mossy-build");
		});

		tasks.register("buildAndCollect+Specified", (task) -> {
			List<String> versionsSpecifications = getVersionsSpecifications(project);
			controller.getVersions().forEach((version) -> {
				if (!versionsSpecifications.contains(version.getProject())) {
					return;
				}
				task.dependsOn(":%s:buildAndCollect".formatted(version.getProject()));
			});
			task.setGroup("mossy-build");
		});

		tasks.register("publish+All", (task) -> {
			List<StonecutterProject> versions = controller.getVersions()
					.stream()
					.sorted((a, b) -> controller.compare(a.getProject(), b.getProject()))
					.toList();

			for (String publishTask : List.of("publishModrinth", "publishCurseforge")) {
				for (int i = 1; i < versions.size(); i++) {
					StonecutterProject first = versions.get(i - 1);
					StonecutterProject second = versions.get(i);

					TaskProvider<Task> firstTask = childProjects.get(first.getProject()).getTasks().named(publishTask);
					TaskProvider<Task> secondTask = childProjects.get(second.getProject()).getTasks().named(publishTask);
					task.dependsOn(firstTask, secondTask);

					secondTask.configure((t) -> t.setMustRunAfter(List.of(firstTask)));
				}
			}

			task.setGroup("mossy-publish");
		});

		tasks.register("publish+Specified", (task) -> {
			List<String> versionsSpecifications = getVersionsSpecifications(project);
			controller.getVersions().forEach((version) -> {
				if (!versionsSpecifications.contains(version.getProject())) {
					return;
				}
				task.dependsOn(":%s:publishMods".formatted(version.getProject()));
			});
			task.setGroup("mossy-publish");
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
					task.setGroup("mossy-stonecutter");
				}

				Map<String, List<String>> repositoryAndProjects = new HashMap<>();

				childProjects.forEach((id, pr) -> {
					RepositoryHandler repositories = pr.getExtensions().getByType(PublishingExtension.class).getRepositories();
					for (String repo : repositories.getNames()) {
						if (repositoryAndProjects.containsKey(repo)) {
							repositoryAndProjects.get(repo).add(id);
						} else {
							repositoryAndProjects.put(repo, new ArrayList<>(List.of(id)));
						}
					}
				});


				for (Entry<String, List<String>> entry : repositoryAndProjects.entrySet()) {
					String repository = entry.getKey();
					List<String> projects = entry.getValue();

					tasks.register("publish+All+" + repository, (task) -> {
						task.setGroup("mossy-publish-" + repository.toLowerCase());

						List<StonecutterProject> versions = controller.getVersions()
								.stream()
								.filter((p) -> projects.contains(p.getProject()))
								.sorted((a, b) -> controller.compare(a.getProject(), b.getProject()))
								.toList();

						for (String publishTask : List.of("publishMossyPluginPublicationTo%sRepository".formatted(repository))) {
							for (int i = 1; i < versions.size(); i++) {
								StonecutterProject first = versions.get(i - 1);
								StonecutterProject second = versions.get(i);

								TaskProvider<Task> firstTask = childProjects.get(first.getProject()).getTasks().named(publishTask);
								TaskProvider<Task> secondTask = childProjects.get(second.getProject()).getTasks().named(publishTask);
								task.dependsOn(firstTask, secondTask);

								secondTask.configure((t) -> t.setMustRunAfter(List.of(firstTask)));
							}
						}
					});

				}
			}

			@Override
			public void buildFinished(@NotNull BuildResult result) {

			}
		});

		project.getTasks().register("generatePublishWorkflowsForEachVersion", GeneratePublishWorkflowsForEachVersionTask.class, (task) -> {
			task.setGroup("mossy");
			List<String> list = controller.getVersions().stream().map(StonecutterProject::getProject).toList();
			task.setMultiVersions(list);
		});
		project.getTasks().register("generatePersonalProperties", GeneratePersonalPropertiesTask.class, (task) -> {
			task.setGroup("mossy");
		});
		project.getTasks().register("regenerateRunConfigurations", Delete.class, (task) -> {
			task.setGroup("mossy");

			List<String> list = controller.getVersions().stream().map(StonecutterProject::getProject).toList();
			for (String version : list) {
				task.delete(project.file(".idea/runConfigurations/Minecraft_Client___%s__%s.xml".formatted(version.replace(".", "_"), version)));
				task.delete(project.file(".idea/runConfigurations/Minecraft_Server___%s__%s.xml".formatted(version.replace(".", "_"), version)));
			}

			for (String version : list) {
				task.finalizedBy(":%s:ideaSyncTask".formatted(version));
			}
		});
	}

	public static List<String> getVersionsSpecifications(@NotNull Project project) {
		return Arrays.stream(MossyUtils.getProperty(project, "versions_specifications")
				.split(" "))
				.map((version) -> MossyUtils.substringBefore(version, "["))
				.toList();
	}
}
