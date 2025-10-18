package net.lopymine.mossyplugin.common;

import java.util.function.Supplier;
import org.gradle.api.Project;

public class MossyLogger {

	private Supplier<String> name;
	private final String pluginModule;

	public MossyLogger(String pluginModule) {
		this.pluginModule = pluginModule;
	}

	public void setup(String name) {
		this.name = () -> name;
	}

	public void setup(Project project) {
		this.name = project::getName;
	}

	@SuppressWarnings("all")
	public void log(String text, Object... objects) {
		System.out.println("[Mossy%s/%s] %s".formatted(this.pluginModule, this.name.get(), text.formatted(objects)));
	}

	@SuppressWarnings("all")
	public void logModule(String module, String text, Object... objects) {
		System.out.println("[Mossy%s/%s/%s] %s".formatted(this.pluginModule, this.name.get(), module, text.formatted(objects)));
	}

}
