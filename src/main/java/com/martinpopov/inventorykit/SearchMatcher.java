package com.martinpopov.inventorykit;

import java.util.Locale;

/** Decides whether an item name matches what was typed into the search box. */
public final class SearchMatcher {
	/** From this many letters on, the text may match anywhere in the name. */
	private static final int CONTAINS_FROM_LENGTH = 3;

	private SearchMatcher() {
	}

	/**
	 * A name matches when one of its words starts with the query, so "r"
	 * finds "Rotten Flesh" and "Raw Iron" but not "Iron Bars". Longer queries
	 * also match in the middle of a word, so "ston" finds "Cobblestone".
	 */
	public static boolean matches(String name, String query) {
		String needle = query.trim().toLowerCase(Locale.ROOT);

		if (needle.isEmpty()) {
			return true;
		}

		String haystack = name.toLowerCase(Locale.ROOT);

		if (needle.length() >= CONTAINS_FROM_LENGTH && haystack.contains(needle)) {
			return true;
		}

		if (haystack.startsWith(needle)) {
			return true;
		}

		for (int i = 1; i < haystack.length(); i++) {
			if (!Character.isLetterOrDigit(haystack.charAt(i - 1)) && haystack.startsWith(needle, i)) {
				return true;
			}
		}

		return false;
	}
}
