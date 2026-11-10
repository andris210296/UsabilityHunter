package br.com.uhunter.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	public static final String USABILITY_TASK_EXECUTOR = "usabilityTaskExecutor";

	@Bean(name = USABILITY_TASK_EXECUTOR)
	Executor usabilityTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("uhunter-");
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setAllowCoreThreadTimeOut(true);
		executor.initialize();
		return executor;
	}
}
