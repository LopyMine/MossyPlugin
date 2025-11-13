package net.lopymine.mossyplugin.core.extension;

import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.tasks.*;

@Getter
public class MossyCoreDependenciesExtension {

	@Input
	String minecraft;

	@Input
	String mappings;

	@Input
	String fabricApi;

	@Input
	String fabricLoader;

	@Input
	String neoForge;

	@Input
	String forge;

	@Input
	String parchment;

	@Input
	String lombok;

	@Nested
	MossyCoreAdditionalDependencies additional = new MossyCoreAdditionalDependencies();

	public void additional(Action<MossyCoreAdditionalDependencies> action) {
		action.execute(this.additional);
	}
}
