package br.com.uhunter.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.*;

public class GoogleVision {
	

	public static String detectLogosGcs(String url) throws Exception, IOException {
		List<AnnotateImageRequest> requests = new ArrayList<>();

		ImageSource imgSource = ImageSource.newBuilder().setImageUri(url).build();
		Image img = Image.newBuilder().setSource(imgSource).build();
		Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);
		
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					return "Error: " + res.getError().getMessage();
				}

				for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
					return annotation.getDescription();
				}
			}
		}
		return "Did not work!";
		
	}

}
