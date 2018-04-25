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

	public static List<String> detectLogos(InputStream inputStream) throws Exception {		

		List<AnnotateImageRequest> requests = new ArrayList<>();
		List<String> logos = new ArrayList<>();

		ByteString imgBytes = ByteString.readFrom(inputStream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		ImageAnnotatorClient client = ImageAnnotatorClient.create();
		BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
		List<AnnotateImageResponse> responses = response.getResponsesList();

		for (AnnotateImageResponse res : responses) {
			if (res.hasError()) {
				throw new Exception();
			}

			for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
				logos.add(annotation.getDescription());
			}
			return logos;
		}

		logos.add("Error!");
		return logos;
	}

	public static List<String> detectText(InputStream inputStream) throws Exception, IOException {
		
		List<AnnotateImageRequest> requests = new ArrayList<>();
		List<String> texts = new ArrayList<>();

		ByteString imgBytes = ByteString.readFrom(inputStream);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					texts.add("Error: " + res.getError().getMessage());
					return texts;
				}

				for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
					texts.add(annotation.getDescription());
				}
				return texts;
			}
		} catch (Exception e) {
			texts.add("Error: " + e.getMessage());
			return texts;
		}
		texts.add("Error!");
		return texts;

	}

}
