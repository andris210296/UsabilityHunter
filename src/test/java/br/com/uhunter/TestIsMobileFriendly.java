package br.com.uhunter;

import org.junit.Test;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.searchconsole.v1.SearchConsole;
import com.google.api.services.searchconsole.v1.SearchConsole.UrlTestingTools;
import com.google.api.services.searchconsole.v1.SearchConsole.UrlTestingTools.MobileFriendlyTest;
import com.google.api.services.searchconsole.v1.SearchConsole.UrlTestingTools.MobileFriendlyTest.Run;
import com.google.appengine.repackaged.org.apache.http.HttpEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class TestIsMobileFriendly {

	@Test
	public void TestIsMobileFriendlyTest() throws Exception {

		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";

		IsMobileFriendly isMobileFriendlyExpected1 = mock(IsMobileFriendly.class);
		when(isMobileFriendlyExpected1.getResult()).thenReturn(true);

		IsMobileFriendly isMobileFriendlyResult1 = new IsMobileFriendly(url1);

		assertEquals(isMobileFriendlyExpected1.getResult(), isMobileFriendlyResult1.getResult());
		assertEquals(new ArrayList<>(), isMobileFriendlyResult1.getListOfIssues());
				
		
		String url2 = "https://www.vestibularfatec.com.br/home/";

		IsMobileFriendly isMobileFriendlyExpected2 = mock(IsMobileFriendly.class);
		when(isMobileFriendlyExpected2.getResult()).thenReturn(false);

		IsMobileFriendly isMobileFriendlyResult2 = new IsMobileFriendly(url2);

		List<String> listOfIssues = new ArrayList<>();
		listOfIssues.add("USE_LEGIBLE_FONT_SIZES");
		listOfIssues.add("CONFIGURE_VIEWPORT");
		listOfIssues.add("SIZE_CONTENT_TO_VIEWPORT");
		listOfIssues.add("TAP_TARGETS_TOO_CLOSE");
		
		assertEquals(isMobileFriendlyExpected2.getResult(), isMobileFriendlyResult2.getResult());
		assertEquals(listOfIssues.size(), isMobileFriendlyResult2.getListOfIssues().size());
		for (String issue : isMobileFriendlyResult2.getListOfIssues()) {
			assertTrue(listOfIssues.contains(issue));
		}
		
	}

	@Test
	public void TestParseJsonResult() throws Exception {

		String stringJsonResult1 = "{\r\n" + "  \"testStatus\": {\r\n" + "    \"status\": \"COMPLETE\"\r\n" + "  },\r\n"
				+ "  \"mobileFriendliness\": \"MOBILE_FRIENDLY\",\r\n" + "  \"resourceIssues\": [\r\n" + "    {\r\n"
				+ "      \"blockedResource\": {\r\n"
				+ "        \"url\": \"https://login.wikimedia.org/wiki/Special:CentralAutoLogin/checkLoggedIn?type=script&wikiid=ptwiki&mobile=1&proto=https&mobile=1\"\r\n"
				+ "      }\r\n" + "    }\r\n" + "  ]\r\n" + "}";

		IsMobileFriendly isMobileFriendlyExpected1 = mock(IsMobileFriendly.class);
		when(isMobileFriendlyExpected1.getResult()).thenReturn(true);

		IsMobileFriendly isMobileFriendlyResult1 = new IsMobileFriendly();
		isMobileFriendlyResult1.httpResulToJson(stringJsonResult1);

		assertEquals(isMobileFriendlyExpected1.getResult(), isMobileFriendlyResult1.getResult());

		String stringJsonResult2 = "{\r\n" + "  \"testStatus\": {\r\n" + "    \"status\": \"COMPLETE\"\r\n" + "  },\r\n"
				+ "  \"mobileFriendliness\": \"NOT_MOBILE_FRIENDLY\",\r\n" + "  \"mobileFriendlyIssues\": [\r\n"
				+ "    {\r\n" + "      \"rule\": \"TAP_TARGETS_TOO_CLOSE\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"rule\": \"SIZE_CONTENT_TO_VIEWPORT\"\r\n" + "    }\r\n" + "  ]\r\n" + "}";

		IsMobileFriendly isMobileFriendlyExpected2 = mock(IsMobileFriendly.class);
		when(isMobileFriendlyExpected1.getResult()).thenReturn(false);

		IsMobileFriendly isMobileFriendlyResult2 = new IsMobileFriendly();
		isMobileFriendlyResult2.httpResulToJson(stringJsonResult2);

		assertEquals(isMobileFriendlyExpected2.getResult(), isMobileFriendlyResult2.getResult());

		List<String> listOfIssues = new ArrayList<>();
		listOfIssues.add("TAP_TARGETS_TOO_CLOSE");
		listOfIssues.add("SIZE_CONTENT_TO_VIEWPORT");

		when(isMobileFriendlyExpected2.getListOfIssues()).thenReturn(listOfIssues);

		assertEquals(isMobileFriendlyExpected2.getListOfIssues(), isMobileFriendlyResult2.getListOfIssues());

	}
}
