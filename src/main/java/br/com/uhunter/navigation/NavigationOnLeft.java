package br.com.uhunter.navigation;

import java.awt.image.BufferedImage;
import java.io.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.util.*;

import com.google.cloud.vision.v1.Vertex;

import br.com.uhunter.utils.GoogleVision;
import br.com.uhunter.utils.ImageUtils;
import net.bytebuddy.dynamic.scaffold.MethodGraph.NodeList;

public class NavigationOnLeft {

	private byte[] byteImage;
	private BufferedImage bufferedImage;
	private String url;
	private List<ParagraphText> paragraphs = new ArrayList<>();
	private List<ParagraphText> paragraphsOnTheMiddleLeft = new ArrayList<>();
	private LinkedHashMap<String, String> linksFromPage = new LinkedHashMap<>();
	private List<String> items = new ArrayList<>();
	private int quantityOfItemsOnMiddleLeft;
	private boolean isTheNavigationOnTheLeftSide;
	private List<List<String>> itemsOnAlphabeticalOrder = new ArrayList<>();
	private List<String> itemsOnNavBarTag = new ArrayList<>();

	public NavigationOnLeft(byte[] byteImage, String url) throws Exception {
		setByteImage(byteImage);
		setBufferedImage(ImageUtils.byteArrayToBufferedImage(getByteImage()));
		setUrl(url);

		setParagraphs();
		setLinksFromPage();
		setItemsOnNavBarTag();
		setParagraphsParagraphsOnTheMiddleLeft();
		setNavigationOnTheLeftSide();
		setItemsOnAlphabeticalOrder();
	}

	public NavigationOnLeft() {

	}

	public void setParagraphs() throws Exception {

		List<ParagraphText> paragraphs = new ArrayList<>();

		List<List<Vertex>> vertexes = GoogleVision.detectBlockVertexesFromImage(getByteImage());

		for (List<Vertex> vertex : vertexes) {

			InputStream is = ImageUtils.getPiece(getBufferedImage(), vertex);

			List<String> lines = GoogleVision.detectText(is);
			paragraphs.add(new ParagraphText(vertex, lines));
		}

		this.paragraphs = paragraphs;
	}

	public void setLinksFromPage() throws IOException {
		Document document = Jsoup.connect(getUrl()).timeout(10000).get();

		Elements links = document.select("a[href]");

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

		for (Element link : links) {
			map.put(link.text(), link.attr("abs:href"));
		}

		this.linksFromPage = map;
	}

	public void setNavigationOnTheLeftSide() throws Exception {

		LinkedHashMap<String, String> links = getLinksFromPage();

		if (!itemsOnNavBarTag.isEmpty()) {
			isTheNavigationOnTheLeftSide = isTheNavBarTagOnLeftSide();

		} else {

			int count = 0;

			for (ParagraphText paragraphText : getParagraphsOnTheMiddleLeft()) {
				for (String link : links.keySet()) {
					if (paragraphText.getLines().contains(link)) {
						count += 1;
						items.add(link);
					}
				}
			}

			setQuantityOfItemsOnMiddleLeft(count);

			isTheNavigationOnTheLeftSide = isTheQuantityOfLinksByWordsEnough(getParagraphs(), count);
		}
	}

	public boolean isTheNavigationOnTheLeftSide() {
		return isTheNavigationOnTheLeftSide;
	}

	public boolean isTheQuantityOfLinksByWordsEnough(List<ParagraphText> paragraphs, int qtdOfLinks) {

		int qtdLines = 0;
		double percentage = 0.0;

		for (ParagraphText paragraph : paragraphs) {

			qtdLines = paragraph.getLines().size();
			percentage = ((double) qtdOfLinks / qtdLines) * 100;

			if (percentage >= 30) {
				return true;
			}
		}
		return false;
	}

	public void setItemsOnAlphabeticalOrder() {
		List<String> sortedItems = new ArrayList<>();
		for (String item : getItems()) {
			sortedItems.add(item.toString());
		}
		Collections.sort(sortedItems);

		int nextSortedWord = 1;
		int nextWord = 1;
		List<List<String>> sortedBlocks = new ArrayList<>();
		List<String> items = new ArrayList<>();

		for (int i = 0; i < sortedItems.size(); i++) {
			for (int j = 0; j < getItems().size(); j++) {
				nextSortedWord = i + 1;
				nextWord = j + 1;

				if (sortedItems.get(i).equals(getItems().get(j))) {

					while ((nextSortedWord < sortedItems.size() && nextWord < getItems().size())
							&& sortedItems.get(nextSortedWord).equals(getItems().get(nextWord))) {

						for (List<String> block : sortedBlocks) {
							if (!block.contains(getItems().get(j))) {
								if (!items.contains(getItems().get(j))) {
									items.add(getItems().get(j));
								}
							} else {
								items.remove(getItems().get(j));
								break;
							}
						}

						for (List<String> block : sortedBlocks) {
							if (!block.contains(getItems().get(nextWord))) {
								if (!items.contains(getItems().get(nextWord))) {
									items.add(getItems().get(nextWord));
								}
							} else {
								items.remove(getItems().get(nextWord));
								break;
							}
						}
						nextWord += 1;
						nextSortedWord += 1;
					}
					sortedBlocks.add(items);
				}
				items = new ArrayList<>();
			}

		}

		List<List<String>> blocks = new ArrayList<>();
		for (List<String> block : sortedBlocks) {
			blocks.add(block);
		}

		for (List<String> block : sortedBlocks) {
			if (block.isEmpty()) {
				blocks.remove(block);
			}
		}

		this.itemsOnAlphabeticalOrder = blocks;
	}

	public boolean areThereEnoughItemsOnAlphabeticalOrder() {

		int blockWithMoreItems = 2;

		for (List<String> block : getItemsOnAlphabeticalOrder()) {
			if (block.size() > blockWithMoreItems) {

				double percentage = ((double) block.size() / getQuantityOfItemsOnMiddleLeft()) * 100;
				if (percentage >= 50) {
					return true;
				}
			}
		}
		return false;
	}

	public void setParagraphsParagraphsOnTheMiddleLeft() {

		List<ParagraphText> paragraphOnTheMiddleLeft = new ArrayList<ParagraphText>();

		double xMiddle = getBufferedImage().getWidth() / 2;

		for (ParagraphText paragraphText : getParagraphs()) {
			if (paragraphText.getVertexes().get(1).getX() <= xMiddle) {
				paragraphOnTheMiddleLeft.add(paragraphText);
			}
		}
		this.paragraphsOnTheMiddleLeft = paragraphOnTheMiddleLeft;
	}

	public void setItemsOnNavBarTag() throws IOException {

		List<String> itemsOnNavBar = new ArrayList<>();

		Document document = Jsoup.connect(getUrl()).timeout(10000).get();

		Elements navigationTag = document.getElementsByClass("navigation");

		if (!navigationTag.isEmpty()) {
			String[] values = navigationTag.text().split(" ");

			for (String string : values) {
				itemsOnNavBar.add(string);
			}
		}

		itemsOnNavBarTag = itemsOnNavBar;
	}

	public boolean isTheNavBarTagOnLeftSide() {

		int qtd = 0;

		if (!itemsOnNavBarTag.isEmpty()) {
			for (ParagraphText paragraphText : paragraphsOnTheMiddleLeft) {
				for (String string : itemsOnNavBarTag) {
					if (paragraphText.getLines().contains(string)) {
						qtd++;
						if (AreThereEnoughItemsOnNavBarTagOnLeftSide(qtd, itemsOnNavBarTag)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean AreThereEnoughItemsOnNavBarTagOnLeftSide(int qtd, List<String> itemsOnNavBarTag) {

		int qtdItemsOnNavBarTag = itemsOnNavBarTag.size();

		double percentage = ((double) qtd / qtdItemsOnNavBarTag) * 100;

		if (percentage >= 50) {
			return true;
		}

		return false;
	}

	public List<String> getItemsOnNavBarTag() {
		return itemsOnNavBarTag;
	}

	public int getQuantityOfItemsOnMiddleLeft() {
		return quantityOfItemsOnMiddleLeft;
	}

	public byte[] getByteImage() {
		return byteImage;
	}

	public void setByteImage(byte[] byteImage) {
		this.byteImage = byteImage;
	}

	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	public void setBufferedImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<ParagraphText> getParagraphs() {
		return paragraphs;
	}

	public void setParagraphs(List<ParagraphText> paragraphs) {
		this.paragraphs = paragraphs;
	}

	public LinkedHashMap<String, String> getLinksFromPage() {
		return linksFromPage;
	}

	public void setQuantityOfItemsOnMiddleLeft(int quantityOfItems) {
		this.quantityOfItemsOnMiddleLeft = quantityOfItems;
	}

	public List<String> getItems() {
		return items;
	}

	public List<List<String>> getItemsOnAlphabeticalOrder() {
		return itemsOnAlphabeticalOrder;
	}

	public List<ParagraphText> getParagraphsOnTheMiddleLeft() {
		return paragraphsOnTheMiddleLeft;
	}

}
