package net.lopymine.mossyplugin.core.extension;

import java.util.List;
import lombok.Getter;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.Nullable;

@Getter
public class MossyCoreProcessResourcesExtension {

	@Input
	@Nullable
	List<String> expandFiles;

}
