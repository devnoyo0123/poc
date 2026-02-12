package com.example.retryserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class FailureSimulatorService {

	private static final Logger log = LoggerFactory.getLogger(FailureSimulatorService.class);

	@Value("${failure.rate:0.4}")
	private double failureRate;

	/**
	 * Simulates intermittent failures based on configured failure rate.
	 * Throws ResponseStatusException with random HTTP error codes.
	 *
	 * @throws ResponseStatusException if random check indicates failure
	 */
	public void simulateFailure() {
		double random = ThreadLocalRandom.current().nextDouble();
		log.debug("Failure simulation check: random={}, threshold={}", random, failureRate);

		if (random < failureRate) {
			// Randomly choose between 500 and 503 errors
			HttpStatus status = ThreadLocalRandom.current().nextDouble() < 0.5
				? HttpStatus.INTERNAL_SERVER_ERROR
				: HttpStatus.SERVICE_UNAVAILABLE;

			log.warn("Simulating failure: {} {}", status.value(), status.getReasonPhrase());

			throw new ResponseStatusException(status,
				"Simulated intermittent failure (rate=" + (failureRate * 100) + "%)");
		}

		log.debug("Request succeeded (no failure simulated)");
	}

	/**
	 * Always succeeds - used for stable endpoints to establish baseline.
	 */
	public void noFailure() {
		log.debug("Stable endpoint - always succeeds");
	}

	/**
	 * Allows dynamic failure rate modification for testing.
	 *
	 * @param newFailureRate new failure rate between 0.0 and 1.0
	 */
	public void setFailureRate(double newFailureRate) {
		if (newFailureRate < 0.0 || newFailureRate > 1.0) {
			throw new IllegalArgumentException("Failure rate must be between 0.0 and 1.0");
		}
		this.failureRate = newFailureRate;
		log.info("Failure rate updated to: {}%", (failureRate * 100));
	}

	public double getFailureRate() {
		return failureRate;
	}
}
