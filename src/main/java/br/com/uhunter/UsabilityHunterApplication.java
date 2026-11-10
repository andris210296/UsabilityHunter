package br.com.uhunter;

import java.security.Security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UsabilityHunterApplication {

	/** Seconds the JVM keeps successful DNS lookups; see {@code UrlSafetyValidator} (DNS rebinding). */
	private static final String DNS_CACHE_TTL_SECONDS = "60";

	public static void main(String[] args) {
		pinDnsCache();
		SpringApplication.run(UsabilityHunterApplication.class, args);
	}

	/**
	 * Must run before the first lookup. Makes the address validated by {@code UrlSafetyValidator}
	 * the same one the HTTP clients connect to, instead of letting an attacker's DNS answer twice.
	 */
	static void pinDnsCache() {
		if (Security.getProperty("networkaddress.cache.ttl") == null
				&& System.getProperty("networkaddress.cache.ttl") == null) {
			Security.setProperty("networkaddress.cache.ttl", DNS_CACHE_TTL_SECONDS);
		}
	}
}
