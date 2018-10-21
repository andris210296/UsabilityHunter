package br.com.uhunter.utils;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.*;

import org.json.JSONObject;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.appengine.repackaged.com.google.common.io.ByteStreams;

public class ScreenshotWebPageModeler {

	private static final Point GOOGLE_VISION_MINIMUM_SIZE = new Point(640, 480);

	private int horizontalPieces = 0;
	private int verticalPieces = 0;

	private String url;
	private BufferedImage bufferedImage;
	private byte[] byteImage;
	private byte[][][] byteImageMatrix;

	private List<String> apiKeys = new ArrayList<String>() {
		{
			add("ak-6qkw8-fcb6p-s0asj-tzb5b-yv1qa");
			add("ak-c57am-7waaf-yrbn0-zbm79-8fycw");
			add("ak-qy3qb-j4fnn-p97q0-qj101-x5vxj");
			add("ak-pnh0r-7008b-mnrc6-r7a6v-qqx2v");
			add("ak-jd5q0-s7sr1-6cg2x-xb4jb-7zkwn");
			add("ak-chzzw-d56fs-dg8qf-ezdzp-27kmn");
		}
	};

	public ScreenshotWebPageModeler(String url) throws Exception {
		setUrl(url);
		setByteImage(takeScreenshot(getUrl()));

		setBufferedImage(ImageUtils.byteArrayToBufferedImage(getByteImage()));
		setQuantityOfPieces(getBufferedImage());

		setByteImageMatrix(getBytesImagePieces());
		
	}

	public ScreenshotWebPageModeler() {
	}

	public byte[] takeScreenshot(String url) throws Exception {

		HttpResponse httpResponse;

		while (true) {
			try {

				Random rand = new Random();

				HttpTransport httpTransport = new NetHttpTransport();
				HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
				String apiUrl = String.format("http://PhantomJsCloud.com/api/browser/v2/%s/",
						apiKeys.get(rand.nextInt(apiKeys.size() - 1)));

				GenericUrl urlApi = new GenericUrl(apiUrl);

				JSONObject json = new JSONObject();
				json.put("url", url);
				json.put("renderType", "jpg");
				json.put("outputAsJson:false", false);
				String requestBody = json.toString();

				HttpRequest request = requestFactory
						.buildPostRequest(urlApi, ByteArrayContent.fromString("application/json", requestBody))
						.setConnectTimeout(3 * 60000).setReadTimeout(3 * 60000);

				httpResponse = request.execute();
				break;

			} catch (SocketTimeoutException e) {
				System.err.println("SocketTimeoutException :" + e.getMessage());
				continue;
			}
		}

		InputStream inputStream = httpResponse.getContent();
		byte[] byteImage = ImageUtils.inputStreamToByteArray(inputStream);
		
		inputStream.close();
		httpResponse.disconnect();
		
		return byteImage;
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
	public byte[][][] getBytesImagePieces() throws IOException {
		
		BufferedImage bfImage = ImageUtils.byteArrayToBufferedImage(getByteImage());

		BufferedImage cut = null;

		int h = 0;
		int v = 0;

		byte[][][] imagePieces = new byte[getVerticalPieces()][getHorizontalPieces()][];

		Double widthCutDouble = (double) (bfImage.getWidth() / getHorizontalPieces());
		int widthCut = widthCutDouble.intValue();

		Double heightCutDouble = (double) (bfImage.getHeight() / getVerticalPieces());
		int heightCut = heightCutDouble.intValue();

		while (v < getVerticalPieces()) {
			h = 0;
			while (h < getHorizontalPieces()) {
				cut = bfImage.getSubimage(h * widthCut, v * heightCut, widthCut, heightCut);
				imagePieces[v][h] = ImageUtils.bufferedImageToByteArray(cut);

				h += 1;
			}
			v += 1;
		}
		v = 0;

		return imagePieces;
	}

	public byte[] getByteImagePiece(int v, int h) {
		return getByteImageMatrix()[v][h];
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
	
	public byte[] getByteImage() {
		return byteImage;
	}

	public void setByteImage(byte[] byteImage) {
		this.byteImage = byteImage;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public byte[][][] getByteImageMatrix() {
		return byteImageMatrix;
	}
	
	public void setByteImageMatrix(byte[][][] byteImageMatrix) {
		this.byteImageMatrix = byteImageMatrix;
	}

	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	public void setBufferedImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}

}
