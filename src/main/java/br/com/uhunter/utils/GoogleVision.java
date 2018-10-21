package br.com.uhunter.utils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;

public class GoogleVision {

	public static List<String> detectLogo(byte[] byteImage) throws Exception {

		List<AnnotateImageRequest> requests = new ArrayList<>();
		List<String> result = new ArrayList<>();

		ByteString imgBytes = ByteString.copyFrom(byteImage);

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
				result.add(annotation.getDescription());
			}
			return result;
		}

		result.add("Error!");
		return result;

	}

	public static List<List<Vertex>> detectBlockVertexesFromImage(byte[] byteImage) throws Exception {
		
		List<List<Vertex>> listVertexes = new ArrayList<>();
		List<Vertex> vertexes = new ArrayList<>();
		List<String> words = new ArrayList<>();

		List<AnnotateImageRequest> requests = new ArrayList<>();

		ByteString imgBytes = ByteString.copyFrom(byteImage);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();
			client.close();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					throw new Exception();
				}

				TextAnnotation annotation = res.getFullTextAnnotation();
				for (Page page : annotation.getPagesList()) {
					String pageText = "";
					for (Block block : page.getBlocksList()) {
						String blockText = "";
						for (Paragraph para : block.getParagraphsList()) {
							String paraText = "";

							for (Word word : para.getWordsList()) {
								String wordText = "";

								for (Symbol symbol : word.getSymbolsList()) {
									wordText = wordText + symbol.getText();
								}
								paraText = paraText + " " + wordText;
								words.add(wordText);
							}
							
							blockText = blockText + paraText;
							
							listVertexes.add(new ArrayList<Vertex>(){{
							    add(Vertex.newBuilder().setX(para.getBoundingBox().getVerticesList().get(0).getX()).setY(para.getBoundingBox().getVerticesList().get(0).getY()).build());
							    add(Vertex.newBuilder().setX(para.getBoundingBox().getVerticesList().get(1).getX()).setY(para.getBoundingBox().getVerticesList().get(1).getY()).build());
							    add(Vertex.newBuilder().setX(para.getBoundingBox().getVerticesList().get(2).getX()).setY(para.getBoundingBox().getVerticesList().get(2).getY()).build());
							    add(Vertex.newBuilder().setX(para.getBoundingBox().getVerticesList().get(3).getX()).setY(para.getBoundingBox().getVerticesList().get(3).getY()).build());
							}});
																						
						}
						pageText = pageText + blockText;
						
					}					
				}
			}
		} catch (Exception e) {
			throw new Exception();
		}
		return listVertexes;
	}
	
	public static List<String> detectText(byte[] byteImage) throws Exception {

		List<AnnotateImageRequest> requests = new ArrayList<>();
		List<String> result = new ArrayList<>();

		ByteString imgBytes = ByteString.copyFrom(byteImage);

		Image img = Image.newBuilder().setContent(imgBytes).build();
		Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		ImageAnnotatorClient client = ImageAnnotatorClient.create();
		BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
		List<AnnotateImageResponse> responses = response.getResponsesList();

		for (AnnotateImageResponse res : responses) {
			if (res.hasError()) {
				throw new Exception();
			}

			if (res.getTextAnnotationsCount() != 0) {
				for (String string : res.getTextAnnotationsList().get(0).getDescription().split("\n")) {
					result.add(string);
				}
			}

			return result;
		}

		result.add("Error!");
		return result;
	}

}
