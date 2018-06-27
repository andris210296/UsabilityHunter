package br.com.uhunter;

import org.junit.Test;
import org.mockito.Mock;

import br.com.uhunter.model.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class LogoIdentificationTest {
	
	@Mock
	LogoIdentification logoIdentification;
	
	@Test
	public void detectLogoByImageTest() throws Exception{
		
		List<String> logoResult = new ArrayList<>();
		logoResult.add("logo");
		logoResult.add("anything");
		
		when(GoogleVision.detectLogos(any(InputStream.class))).thenReturn(logoResult);
		
		
		String result = logoIdentification.detectLogoByImage(any(ScreenshotWebPageModeler.class));
		
		assertEquals("logo", result);
		
	}

  
}
