package br.com.uhunter.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

public class GoogleVision {

	public static String detectLogosGcs(InputStream inputStream) throws Exception, IOException {
				
		List<AnnotateImageRequest> requests = new ArrayList<>();

		ByteString imgBytes = ByteString.readFrom(inputStream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
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
		}catch (Exception e) {
			return "Did not work! "+e.getMessage();
		}
		return "Did not work!";	
		
	}
}
