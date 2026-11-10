package br.com.uhunter.vision;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Vertex;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;

import br.com.uhunter.config.UhunterProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Google Cloud Vision implementation of {@link VisionApi}.
 *
 * <p>Owns the shared {@link ImageAnnotatorClient}: it is created lazily on first use (so the app starts
 * and the non-Vision checks work without credentials) and closed on shutdown. Credentials come from
 * {@code uhunter.google-application-credentials}, then {@code GOOGLE_APPLICATION_CREDENTIALS}, then
 * Application Default Credentials ({@code gcloud auth application-default login}).
 */
@Component
public class GoogleVisionApi implements VisionApi {

	private static final Logger log = LoggerFactory.getLogger(GoogleVisionApi.class);
	private static final List<String> VISION_SCOPES = List.of(
			"https://www.googleapis.com/auth/cloud-vision",
			"https://www.googleapis.com/auth/cloud-platform");

	private final UhunterProperties properties;
	private final Object clientLock = new Object();
	private volatile ImageAnnotatorClient client;

	public GoogleVisionApi(UhunterProperties properties) {
		this.properties = properties;
	}

	/** Fail fast on a configured-but-missing key file instead of on the first request. */
	@PostConstruct
	void validateConfiguration() {
		String path = configuredCredentialsPath();
		if (path == null) {
			log.info("No Vision credentials file configured; will use Application Default Credentials on first use.");
			return;
		}
		if (!new File(path).isFile()) {
			throw new IllegalStateException("Vision credentials file not found: " + path);
		}
	}

	@PreDestroy
	void shutdown() {
		ImageAnnotatorClient current = client;
		client = null;
		if (current == null) {
			return;
		}
		try {
			current.close();
		} catch (Exception e) {
			log.debug("Vision client close: {}", e.toString());
		}
	}

	@Override
	public List<String> detectLogo(byte[] image) throws Exception {
		AnnotateImageResponse response = annotate(image, Type.LOGO_DETECTION);
		List<String> result = new ArrayList<>();
		for (EntityAnnotation annotation : response.getLogoAnnotationsList()) {
			String description = annotation.getDescription();
			if (description != null && !description.isBlank()) {
				result.add(description.trim());
			}
		}
		log.debug("Vision LOGO_DETECTION returned {} label(s): {}", result.size(), result);
		return result;
	}

	@Override
	public List<String> detectText(byte[] image) throws Exception {
		AnnotateImageResponse response = annotate(image, Type.TEXT_DETECTION);
		List<String> result = new ArrayList<>();
		if (response.getTextAnnotationsCount() == 0) {
			log.debug("Vision TEXT_DETECTION returned no annotations");
			return result;
		}
		for (String line : response.getTextAnnotationsList().get(0).getDescription().split("\n")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				result.add(trimmed);
			}
		}
		log.debug("Vision TEXT_DETECTION returned {} line(s)", result.size());
		return result;
	}

	@Override
	public List<String> detectWebEntities(byte[] image) throws Exception {
		AnnotateImageResponse response = annotate(image, Type.WEB_DETECTION);
		if (!response.hasWebDetection()) {
			log.debug("Vision WEB_DETECTION returned no payload");
			return List.of();
		}

		LinkedHashSet<String> result = new LinkedHashSet<>();
		WebDetection web = response.getWebDetection();
		for (WebDetection.WebLabel label : web.getBestGuessLabelsList()) {
			addIfPresent(result, label.getLabel());
		}
		for (WebDetection.WebEntity entity : web.getWebEntitiesList()) {
			if (entity.getScore() > 0 && entity.getScore() < 0.2f) {
				continue;
			}
			addIfPresent(result, entity.getDescription());
		}
		for (WebDetection.WebPage page : web.getPagesWithMatchingImagesList()) {
			addIfPresent(result, page.getPageTitle());
		}

		List<String> list = new ArrayList<>(result);
		log.debug("Vision WEB_DETECTION returned {} entity/label(s): {}", list.size(),
				list.size() > 8 ? list.subList(0, 8) + "…" : list);
		return list;
	}

	@Override
	public List<VisionTextBlock> detectTextBlocks(byte[] image) throws Exception {
		AnnotateImageResponse response = annotate(image, Type.DOCUMENT_TEXT_DETECTION);
		List<VisionTextBlock> blocks = new ArrayList<>();
		if (!response.hasFullTextAnnotation()) {
			log.debug("Vision DOCUMENT_TEXT_DETECTION returned no full-text annotation");
			return blocks;
		}

		TextAnnotation annotation = response.getFullTextAnnotation();
		for (Page page : annotation.getPagesList()) {
			for (Block block : page.getBlocksList()) {
				for (Paragraph para : block.getParagraphsList()) {
					String text = paragraphText(para);
					if (text.isEmpty()) {
						continue;
					}
					List<Vertex> vertices = new ArrayList<>(para.getBoundingBox().getVerticesList());
					blocks.add(VisionTextBlock.builder()
							.vertices(vertices)
							.lines(List.of(text))
							.text(text)
							.build());
				}
			}
		}
		log.debug("Vision DOCUMENT_TEXT_DETECTION returned {} paragraph block(s)", blocks.size());
		return blocks;
	}

	private static String paragraphText(Paragraph paragraph) {
		StringBuilder text = new StringBuilder();
		for (Word word : paragraph.getWordsList()) {
			StringBuilder token = new StringBuilder();
			for (Symbol symbol : word.getSymbolsList()) {
				token.append(symbol.getText());
			}
			String trimmed = token.toString().trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (text.length() > 0) {
				text.append(' ');
			}
			text.append(trimmed);
		}
		return text.toString();
	}

	private AnnotateImageResponse annotate(byte[] image, Type featureType) throws Exception {
		log.debug("Calling Vision API feature={} imageBytes={}", featureType, image == null ? 0 : image.length);
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
				.addFeatures(Feature.newBuilder().setType(featureType).build())
				.setImage(Image.newBuilder().setContent(ByteString.copyFrom(image)).build())
				.build();

		BatchAnnotateImagesResponse batch = visionClient().batchAnnotateImages(List.of(request));
		if (batch.getResponsesCount() == 0) {
			throw new IllegalStateException("Vision returned an empty response");
		}
		AnnotateImageResponse response = batch.getResponses(0);
		if (response.hasError()) {
			throw new IllegalStateException(response.getError().getMessage());
		}
		return response;
	}

	private ImageAnnotatorClient visionClient() throws IOException {
		ImageAnnotatorClient current = client;
		if (current != null) {
			return current;
		}
		synchronized (clientLock) {
			if (client == null) {
				client = createClient();
			}
			return client;
		}
	}

	private ImageAnnotatorClient createClient() throws IOException {
		GoogleCredentials credentials = loadCredentials();
		ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
				.build();
		return ImageAnnotatorClient.create(settings);
	}

	private GoogleCredentials loadCredentials() throws IOException {
		String path = configuredCredentialsPath();
		if (path != null) {
			try (FileInputStream in = new FileInputStream(path)) {
				log.info("Loaded Google Vision credentials from {}", new File(path).getAbsolutePath());
				return GoogleCredentials.fromStream(in).createScoped(VISION_SCOPES);
			}
		}
		try {
			GoogleCredentials adc = GoogleCredentials.getApplicationDefault().createScoped(VISION_SCOPES);
			log.info("Using Application Default Credentials for Google Vision");
			return adc;
		} catch (IOException e) {
			throw new IllegalStateException(
					"Google Vision credentials are not available. Set uhunter.google-application-credentials "
							+ "(or GOOGLE_APPLICATION_CREDENTIALS) to a service-account JSON, or run "
							+ "'gcloud auth application-default login'.", e);
		}
	}

	private String configuredCredentialsPath() {
		return firstNonBlank(properties.getGoogleApplicationCredentials(),
				System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
	}

	private static void addIfPresent(LinkedHashSet<String> target, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		String cleaned = value.replaceAll("\\s+", " ").trim();
		if (!cleaned.isEmpty()) {
			target.add(cleaned);
		}
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}
}
