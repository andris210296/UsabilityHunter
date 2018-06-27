package br.com.uhunter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.google.appengine.repackaged.org.apache.commons.httpclient.URI;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashMap;

import br.com.uhunter.model.LogoIdentification;
import br.com.uhunter.model.UsabilityRunTests;

public class UsabilityRestTest {

	private static final String JSON_URL_TEST = "{url:\"www.test.com \"}";

	@InjectMocks
	UsabilityRestImpl usabilityRestImpl;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void postRequestTest() throws Exception {

		LogoIdentification logoIdentification = mock(LogoIdentification.class);
		//String response = usabilityRestImpl.setTest(JSON_URL_TEST);

		//assertEquals("", response);

	}
}
