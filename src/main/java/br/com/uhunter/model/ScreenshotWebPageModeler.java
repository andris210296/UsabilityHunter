package br.com.uhunter.model;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;


public class ScreenshotWebPageModeler {

	private static final String API_KEY ="ak-qy3qb-j4fnn-p97q0-qj101-x5vxj"; // "ak-c57am-7waaf-yrbn0-zbm79-8fycw"; 
																			 // "ak-6qkw8-fcb6p-s0asj-tzb5b-yv1qa";
	private static final String URL_API = "http://PhantomJsCloud.com/api/browser/v2/" + API_KEY + "/";

	private static final Point LOGO_DETECTION = new Point(640, 480);

	private int horizontalPieces = 0;
	private int verticalPieces = 0;

	private String url;
	private InputStream imputStream;

	/**
	 * This constructor method receives an url and takes a screenshot of a web page.
	 * 
	 * @param url
	 * @throws Exception
	 */
	public ScreenshotWebPageModeler(String url) throws Exception {
		setUrl(url);
		setImputStream(takeShot(getUrl()));
	}

	/**
	 * This method uses the API PhantomJSCloud that returns a screenshot of a given
	 * url.
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	private InputStream takeShot(String url) throws Exception {

		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost request = new HttpPost(URL_API);
		StringEntity params = new StringEntity("{url:\"" + url + "\",renderType:\"jpg\",outputAsJson:false}");
		request.addHeader("content-type", "application/json");
		request.setEntity(params);
		HttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();

		InputStream instream = entity.getContent();

		setImputStream(instream);

		return instream;
	}

	/**
	 * This method changes the values of the number of possible cuts that can be
	 * done vertically and horizontally of a screenshot. This numbers are based on
	 * the size of the web page and based on Google Vision API images size
	 * limitation.
	 * 
	 * @param img
	 */
	private void setQuantityOfPieces(BufferedImage img) {
		Double widthDouble = img.getWidth() / LOGO_DETECTION.getX();
		int width = widthDouble.intValue();

		Double heightDouble = img.getHeight() / LOGO_DETECTION.getY();
		int height = heightDouble.intValue();

		setVerticalPieces(height);
		setHorizontalPieces(width);
	}

	/**
	 * This method transforms an ImputStream to BufferedImage.
	 * 
	 * @return
	 * @throws IOException
	 */
	private BufferedImage imputStreamToBufferedImage() throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		try {
			while ((len = getImputStream().read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] b = baos.toByteArray();

		BufferedImage img = ImageIO.read(new ByteArrayInputStream(b));

		setQuantityOfPieces(img);

		return img;
	}

	/**
	 * This method cuts in peaces the image and adds to a matrix.
	 * 
	 * @return
	 * @throws Exception
	 */
	public BufferedImage[][] getImagePieces() throws Exception {

		BufferedImage bfImage = imputStreamToBufferedImage();

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

	/**
	 * This method transforms all BufferedImage that are inside of a matrix and
	 * changes to a matrix of InputStream.
	 * 
	 * @param bufferedImage
	 * @return
	 * @throws Exception 
	 */
	private InputStream[][] bufferedImageToInputStream(BufferedImage[][] bufferedImage) throws Exception {

		InputStream[][] byteString = new InputStream[getVerticalPieces()][getHorizontalPieces()];

		for (int v = 0; v < getVerticalPieces(); v++) {
			for (int h = 0; h < getHorizontalPieces(); h++) {

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(bufferedImage[v][h], "jpg", baos);
				byte[] imagem = baos.toByteArray();				
				
				ByteArrayInputStream bais = new ByteArrayInputStream(imagem);
				byteString[v][h] = bais;				
			}
		}
		return byteString;
	}

	/**
	 * This method gets a single image from a matrix.
	 * 
	 * @param v
	 * @param h
	 * @return
	 * @throws Exception
	 */
	public InputStream getImagePiece(int v, int h) throws Exception {
		return bufferedImageToInputStream(getImagePieces())[v][h];
	}

	public static void inputStreamToFile(InputStream inputStream) throws Exception {
		FileOutputStream output = new FileOutputStream("C:/Users/andri/OneDrive/Documentos/Imgs/image.jpg");
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			output.write(buffer, 0, len);
		}
		output.close();
	}
	
	public static void bufferedImageToFile(BufferedImage bufferedImage) throws Exception{
		 File outputfile = new File("C:/Users/andri/OneDrive/Documentos/Imgs/image.jpg");
		 ImageIO.write(bufferedImage, "jpg", outputfile);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public InputStream getImputStream() {
		return imputStream;
	}

	public void setImputStream(InputStream imputStream) {
		this.imputStream = imputStream;
	}

	private int getHorizontalPieces() {
		return horizontalPieces;
	}

	private void setHorizontalPieces(int horizontalPieces) {
		this.horizontalPieces = horizontalPieces;
	}

	private int getVerticalPieces() {
		return verticalPieces;
	}

	private void setVerticalPieces(int verticalPieces) {
		this.verticalPieces = verticalPieces;
	}

}
