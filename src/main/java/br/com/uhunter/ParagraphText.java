package br.com.uhunter;

import java.util.List;

import com.google.cloud.vision.v1.Vertex;

public class ParagraphText {

	private List<Vertex> vertexes;
	private List<String> lines;

	public ParagraphText(List<Vertex> vertexes, List<String> lines) {
		this.vertexes = vertexes;
		this.lines = lines;
	}

	public List<String> getLines() {		
		return lines;
	}

	public List<Vertex> getVertexes() {
		return vertexes;
	}

}
