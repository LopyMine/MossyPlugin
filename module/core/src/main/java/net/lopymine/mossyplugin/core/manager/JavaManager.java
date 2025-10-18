package net.lopymine.mossyplugin.core.manager;

import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class JavaManager {

	public static void apply(@NotNull Project project, MossyPluginCore mossyPlugin) {
		int javaVersionIndex = mossyPlugin.getJavaVersionIndex();
		JavaVersion javaVersion = mossyPlugin.getJavaVersion();

		TaskCollection<JavaCompile> collection = project.getTasks().withType(JavaCompile.class);
		for (JavaCompile javaCompile : collection) {
			javaCompile.getOptions().getRelease().set(javaVersionIndex);
		}

		JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		javaExtension.setSourceCompatibility(javaVersion);
		javaExtension.setTargetCompatibility(javaVersion);
	}
}
