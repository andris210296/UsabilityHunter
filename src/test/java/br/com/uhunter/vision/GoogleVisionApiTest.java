package br.com.uhunter.vision;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.uhunter.config.UhunterProperties;

class GoogleVisionApiTest {

	@Test
	void failsFastWhenConfiguredCredentialsFileIsMissing() {
		UhunterProperties properties = new UhunterProperties();
		properties.setGoogleApplicationCredentials("/definitely/not/here/key.json");

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> new GoogleVisionApi(properties).validateConfiguration());

		assertTrue(ex.getMessage().contains("/definitely/not/here/key.json"));
	}

	@Test
	void creatingTheBeanNeverContactsGoogleOrNeedsCredentials() {
		// The client is created lazily, so the app (and the non-Vision checks) start without credentials.
		assertDoesNotThrow(() -> new GoogleVisionApi(new UhunterProperties()).shutdown());
	}
}
