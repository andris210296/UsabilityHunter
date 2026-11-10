package br.com.uhunter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.uhunter.vision.VisionApi;
import br.com.uhunter.vision.VisionTextBlock;

/** Scriptable {@link VisionApi}: no Google, no credentials. */
public class FakeVision implements VisionApi {

	public final AtomicInteger logoCalls = new AtomicInteger();
	public final AtomicInteger webCalls = new AtomicInteger();
	public final AtomicInteger textCalls = new AtomicInteger();

	@Override
	public List<String> detectLogo(byte[] image) throws Exception {
		return logos(logoCalls.incrementAndGet());
	}

	@Override
	public List<String> detectText(byte[] image) throws Exception {
		textCalls.incrementAndGet();
		return List.of();
	}

	@Override
	public List<String> detectWebEntities(byte[] image) throws Exception {
		webCalls.incrementAndGet();
		return List.of();
	}

	@Override
	public List<VisionTextBlock> detectTextBlocks(byte[] image) throws Exception {
		return List.of();
	}

	/** Override to script what logo detection returns on the n-th call (1-based). */
	protected List<String> logos(int callNumber) throws Exception {
		return List.of();
	}
}
