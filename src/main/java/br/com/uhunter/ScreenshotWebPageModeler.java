package br.com.uhunter;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;

import org.json.JSONObject;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;

public class ScreenshotWebPageModeler {
	
	private static final Point GOOGLE_VISION_MINIMUM_SIZE = new Point(640, 480);
	
	private int horizontalPieces = 0;
	private int verticalPieces = 0;
	
	private String url;
	private BufferedImage bufferedImage;
	private InputStream imputStream;
	private InputStream[][] imputStreamMatrix;
	
	private List<String> apiKeys = new ArrayList<String>() {{
		add("ak-6qkw8-fcb6p-s0asj-tzb5b-yv1qa");
		add("ak-c57am-7waaf-yrbn0-zbm79-8fycw");
		add("ak-qy3qb-j4fnn-p97q0-qj101-x5vxj");
		add("ak-jd5q0-s7sr1-6cg2x-xb4jb-7zkwn");
		add("ak-chzzw-d56fs-dg8qf-ezdzp-27kmn");
	}};

	public ScreenshotWebPageModeler(String url) throws Exception {
		setUrl(url);
		setImputStream(takeScreenshot(getUrl()));
		
		setBufferedImage(ImageUtils.inputStreamToBufferedImage(getImputStream()));
		setQuantityOfPieces(getBufferedImage());
		
		setImputStreamMatrix(bufferedMatrixToInputStreamMatrix((getImagePieces())));
	}
	
	public ScreenshotWebPageModeler() {
	}

	public InputStream takeScreenshot(String url) throws Exception{
		
		Random rand = new Random();		
		
		HttpTransport httpTransport = new NetHttpTransport();
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		String apiUrl = String.format("http://PhantomJsCloud.com/api/browser/v2/%s/", apiKeys.get(rand.nextInt(apiKeys.size()-1)));
		
		GenericUrl urlApi = new GenericUrl(apiUrl);
		
		JSONObject json = new JSONObject();
		json.put("url", url);
		json.put("renderType", "jpg");
		json.put("outputAsJson:false", false);		
		String requestBody = json.toString();
		
		HttpRequest request = requestFactory.buildPostRequest(urlApi,ByteArrayContent.fromString("application/json", requestBody));
		HttpResponse httpResponse = request.execute();
					
		return httpResponse.getContent();
	}
	
	/**
	 * This method changes the values of the number of possible cuts that can be
	 * done vertically and horizontally of a screenshot. This numbers are based on
	 * the size of the web page and based on Google Vision API images size
	 * limitation.
	 *
	 * @param img
	 */
	public void setQuantityOfPieces(BufferedImage img) {
		Double widthDouble = img.getWidth() / GOOGLE_VISION_MINIMUM_SIZE.getX();
		int width = widthDouble.intValue();

		Double heightDouble = img.getHeight() / GOOGLE_VISION_MINIMUM_SIZE.getY();
		int height = heightDouble.intValue();

		setVerticalPieces(height);
		setHorizontalPieces(width);
	}
	
	/**
	 * This method cuts in peaces the image and adds to a matrix.
	 *
	 * @return
	 * @throws Exception
	 */
	public BufferedImage[][] getImagePieces() throws Exception {
		
		BufferedImage bfImage = getBufferedImage();

		BufferedImage cut = null;

		int h = 0;
		int v = 0;

		BufferedImage[][] imagePieces = new BufferedImage[getVerticalPieces()][getHorizontalPieces()];

		Double widthCutDouble = (double) (bfImage.getWidth() / getHorizontalPieces());
		int widthCut = widthCutDouble.intValue();

		Double heightCutDouble = (double) (bfImage.getHeight() / getVerticalPieces());
		int heightCut = heightCutDouble.intValue();

		while (v < getVerticalPieces()) {
			h = 0;
			while (h < getHorizontalPieces()) {
				cut = bfImage.getSubimage(h * widthCut, v * heightCut, widthCut, heightCut);
				imagePieces[v][h] = cut;

				h += 1;
			}
			v += 1;
		}
		v = 0;

		return imagePieces;
	}
	
	public InputStream[][] bufferedMatrixToInputStreamMatrix(BufferedImage[][] bufferedImages) throws Exception {
		InputStream[][] byteString = new InputStream[getVerticalPieces()][getHorizontalPieces()];

		for (int v = 0; v < getVerticalPieces(); v++) {
			for (int h = 0; h < getHorizontalPieces(); h++) {
				byteString[v][h] = ImageUtils.bufferedImageToInputStream(bufferedImages[v][h]);	
			}
		}
		return byteString;
	}
	
	public InputStream getImagePiece(int v, int h) {
		return getImputStreamMatrix()[v][h];
	}

	public void setHorizontalPieces(int horizontalPieces) {
		this.horizontalPieces = horizontalPieces;		
	}

	public void setVerticalPieces(int verticalPieces) {
		this.verticalPieces = verticalPieces;		
	}

	public int getVerticalPieces() {
		return verticalPieces;
	}

	public int getHorizontalPieces() {
		return horizontalPieces;
	}
	
	public InputStream getImputStream() {
		return imputStream;
	}

	public void setImputStream(InputStream imputStream) {
		this.imputStream = imputStream;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public InputStream[][] getImputStreamMatrix() {
		return imputStreamMatrix;
	}

	public void setImputStreamMatrix(InputStream[][] imputStreamMatrix) {
		this.imputStreamMatrix = imputStreamMatrix;
	}

	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	public void setBufferedImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}
	
}
