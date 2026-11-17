package com.martinpopov.inventorykit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

/** Every translation must cover the same texts as English, with the same placeholders. */
class LanguageFilesTest {
	private static final List<String> TRANSLATIONS = List.of("bg_bg", "de_de", "es_es", "fr_fr");

	private static Map<String, String> load(String language) throws IOException {
		String path = "/assets/inventorykit/lang/" + language + ".json";

		try (InputStream stream = LanguageFilesTest.class.getResourceAsStream(path)) {
			assertNotNull(stream, path + " is missing");

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				Map<String, String> texts = new Gson().fromJson(reader, new TypeToken<TreeMap<String, String>>() { }.getType());
				return texts;
			}
		}
	}

	private static int count(String text, String part) {
		return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
	}

	@Test
	void translationsMatchEnglish() throws IOException {
		Map<String, String> english = load("en_us");

		for (String language : TRANSLATIONS) {
			Map<String, String> translated = load(language);
			assertEquals(english.keySet(), translated.keySet(), language + " has different keys");

			for (Map.Entry<String, String> entry : english.entrySet()) {
				String text = translated.get(entry.getKey());
				String where = language + " " + entry.getKey();

				assertFalse(text.isBlank(), where + " is empty");
				assertEquals(count(entry.getValue(), "%s"), count(text, "%s"), where + " placeholders");
				assertEquals(count(entry.getValue(), "\n"), count(text, "\n"), where + " line breaks");
			}
		}
	}
}
