package br.com.uhunter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import br.com.uhunter.responsive.IsMobileFriendly;

public class TestApi {
	
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

}
