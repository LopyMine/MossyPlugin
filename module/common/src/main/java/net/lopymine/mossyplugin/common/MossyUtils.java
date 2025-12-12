package net.lopymine.mossyplugin.common;

import java.util.*;
import java.util.regex.Pattern;
import org.gradle.api.Project;
import org.jetbrains.annotations.*;

@SuppressWarnings("unused")
public class MossyUtils {

	private static final Pattern PLAYER_NICKNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]{2,16}$");

	public static String getPlayerNickname(Properties personalProperties) {
		Object o = personalProperties.get("player_nickname");
		if (o == null) {
			return "Player";
		}
		String playerNickname = o.toString();
		if (!PLAYER_NICKNAME_PATTERN.matcher(playerNickname).matches()) {
			return "Player";
		}
		return playerNickname;
	}

	public static @Nullable UUID getPlayerUuid(Properties personalProperties) {
		try {
			Object o = personalProperties.get("player_uuid");
			if (o == null) {
				return null;
			}
			return UUID.fromString(o.toString());
		} catch (Exception e) {
			return null;
		}
	}

	public static String getProperty(@NotNull Properties properties, String id) {
		if (!properties.containsKey(id)) {
			throw new IllegalArgumentException("Missing important property with id \"%s\" !".formatted(id));
		}
		return properties.get(id).toString();
	}

	public static String getProperty(@NotNull Project project, String id) {
		Map<String, ?> properties = project.getProperties();
		if (!properties.containsKey(id)) {
			throw new IllegalArgumentException("Missing important property with id \"%s\" !".formatted(id));
		}
		return properties.get(id).toString();
	}

	public static String substringBeforeLast(String value, String since) {
		int i = value.lastIndexOf(since);
		if (i == -1) {
			return value;
		}
		return value.substring(0, i);
	}

	public static String substringSinceLast(String value, String since) {
		int i = value.lastIndexOf(since);
		if (i == -1) {
			return value;
		}
		return value.substring(i + 1);
	}

	public static String substringBefore(String value, String since) {
		int i = value.indexOf(since);
		if (i == -1) {
			return value;
		}
		return value.substring(0, i);
	}

	public static String substringSince(String value, String since) {
		int i = value.indexOf(since);
		if (i == -1) {
			return value;
		}
		return value.substring(i + 1);
	}

}
