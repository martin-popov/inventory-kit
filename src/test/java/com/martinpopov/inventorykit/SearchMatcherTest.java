package com.martinpopov.inventorykit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SearchMatcherTest {
	@Test
	void shortQueriesMatchTheStartOfAnyWord() {
		assertTrue(SearchMatcher.matches("Rotten Flesh", "r"));
		assertTrue(SearchMatcher.matches("Raw Iron", "r"));
		assertTrue(SearchMatcher.matches("Block of Redstone", "r"));
		assertTrue(SearchMatcher.matches("Rotten Flesh", "fl"));
		assertFalse(SearchMatcher.matches("Iron Bars", "r"));
		assertFalse(SearchMatcher.matches("Dirt", "r"));
	}

	@Test
	void longerQueriesMatchAnywhere() {
		assertTrue(SearchMatcher.matches("Cobblestone", "ston"));
		assertFalse(SearchMatcher.matches("Cobblestone", "st"));
	}

	@Test
	void caseAndSurroundingSpacesAreIgnored() {
		assertTrue(SearchMatcher.matches("Rotten Flesh", "  ROT "));
		assertTrue(SearchMatcher.matches("Rotten Flesh", "rotten f"));
	}

	@Test
	void emptyQueryMatchesEverything() {
		assertTrue(SearchMatcher.matches("Anything", ""));
		assertTrue(SearchMatcher.matches("Anything", "   "));
	}
}
