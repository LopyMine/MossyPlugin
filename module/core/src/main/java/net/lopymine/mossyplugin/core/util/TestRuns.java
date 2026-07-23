package net.lopymine.mossyplugin.core.util;

import java.io.File;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class TestRuns {

	public static final String ENABLED_PROPERTY = "tests.parallel_runs";
	public static final String LANE_PROPERTY = "mossy.lane";

	public static boolean isEnabled(@NotNull Project project) {
		return project.getProperties().containsKey(ENABLED_PROPERTY);
	}

	public static String getLane(@NotNull Project project) {
		Object lane = project.getProperties().get(LANE_PROPERTY);
		return lane == null ? "1" : lane.toString();
	}

	public static @NotNull File getRunDirectory(@NotNull Project project, String runName) {
		return project.getRootProject().getProjectDir().toPath()
				.resolve("runs")
				.resolve("tests")
				.resolve(getRunDirectoryName(project, runName))
				.toFile();
	}

	public static @NotNull String getRelativeRunDirectory(@NotNull Project project, String runName) {
		return "../../runs/tests/%s".formatted(getRunDirectoryName(project, runName));
	}

	private static @NotNull String getRunDirectoryName(@NotNull Project project, String runName) {
		return "%s_%s".formatted(getLane(project), runName);
	}

}
