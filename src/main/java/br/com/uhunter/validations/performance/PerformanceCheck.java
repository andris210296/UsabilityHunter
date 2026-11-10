package br.com.uhunter.validations.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.uhunter.api.dto.DevicePerformanceResultDto;
import br.com.uhunter.api.dto.PerformanceDevicesResultDto;
import br.com.uhunter.api.dto.PerformanceMetricDto;
import br.com.uhunter.api.dto.PerformanceValidationResultDto;
import br.com.uhunter.validations.AbstractUsabilityCheck;
import br.com.uhunter.validations.AnalysisContext;

/**
 * Loading performance (FCP, LCP, overall score) for mobile and desktop, from PageSpeed Insights.
 */
@Component
public class PerformanceCheck extends AbstractUsabilityCheck<PerformanceValidationResultDto> {

	private static final String METRICS_KEY = "metrics";
	private static final String FAST = "fast";
	private static final String AVERAGE = "average";
	private static final String SLOW = "slow";

	@Override
	protected String name() {
		return "Performance";
	}

	@Override
	protected PerformanceValidationResultDto execute(AnalysisContext context) throws Exception {
		PerformanceAnalysis desktop = new PerformanceAnalysis(context.url(), false, context.pageSpeed());
		PerformanceAnalysis mobile = new PerformanceAnalysis(context.url(), true, context.pageSpeed());
		PerformanceDevicesResultDto devices = PerformanceDevicesResultDto.builder()
				.mobileTest(toDevice(mobile))
				.desktopTest(toDevice(desktop))
				.build();
		return PerformanceValidationResultDto.builder()
				.performanceTest(devices)
				.build();
	}

	@Override
	protected PerformanceValidationResultDto onFailure(Exception e) {
		return PerformanceValidationResultDto.builder()
				.performanceTest(e.toString())
				.build();
	}

	private static DevicePerformanceResultDto toDevice(PerformanceAnalysis analysis) {
		return DevicePerformanceResultDto.builder()
				.optimizationPoints(analysis.getOptimizationPoints())
				.loadingOverall(analysis.getLoadingOverall())
				.loadingPerformance(Map.of(METRICS_KEY, toMetrics(analysis)))
				.build();
	}

	private static List<PerformanceMetricDto> toMetrics(PerformanceAnalysis analysis) {
		List<PerformanceMetricDto> metrics = new ArrayList<>();
		metrics.add(toMetric(analysis.getFCP()));
		metrics.add(toMetric(analysis.getLCP()));
		return metrics;
	}

	private static PerformanceMetricDto toMetric(PerformanceMetric metric) {
		Map<String, Double> distribution = new HashMap<>();
		distribution.put(FAST, metric.getFast());
		distribution.put(AVERAGE, metric.getAverage());
		distribution.put(SLOW, metric.getSlow());
		return PerformanceMetricDto.builder()
				.type(metric.getType())
				.description(metric.getDescription())
				.pageContentLoadDistributionAverage(metric.getOverallMetric())
				.pageSpeed(metric.getPageSpeedMetric())
				.pageContentLoadDistribution(distribution)
				.build();
	}
}
