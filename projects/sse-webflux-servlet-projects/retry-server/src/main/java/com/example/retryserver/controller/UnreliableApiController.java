package com.example.retryserver.controller;

import com.example.retryserver.dto.OrderResponse;
import com.example.retryserver.dto.UserResponse;
import com.example.retryserver.service.FailureSimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UnreliableApiController {

	private static final Logger log = LoggerFactory.getLogger(UnreliableApiController.class);

	@Autowired
	private FailureSimulatorService failureSimulator;

	/**
	 * Unstable endpoint - simulates intermittent failures.
	 * Use this endpoint to test retry mechanisms.
	 */
	@GetMapping("/users/{id}")
	public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
		log.info("GET /api/users/{}", id);

		// Simulate failure before processing
		failureSimulator.simulateFailure();

		UserResponse response = UserResponse.of(id);
		log.info("Returning user: userId={}, userName={}", response.getUserId(), response.getUserName());

		return ResponseEntity.ok(response);
	}

	/**
	 * Unstable endpoint - simulates intermittent failures.
	 */
	@GetMapping("/orders/{id}")
	public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
		log.info("GET /api/orders/{}", id);

		// Simulate failure before processing
		failureSimulator.simulateFailure();

		OrderResponse response = OrderResponse.of(id);
		log.info("Returning order: orderId={}, totalAmount={}",
			response.getOrderId(), response.getTotalAmount());

		return ResponseEntity.ok(response);
	}

	/**
	 * Stable endpoint - always succeeds.
	 * Use this to establish baseline performance without retries.
	 */
	@GetMapping("/users/{id}/stable")
	public ResponseEntity<UserResponse> getUserStable(@PathVariable Long id) {
		log.info("GET /api/users/{}/stable (stable endpoint)", id);

		// No failure simulation
		failureSimulator.noFailure();

		UserResponse response = UserResponse.of(id);
		log.info("Returning user (stable): userId={}, userName={}",
			response.getUserId(), response.getUserName());

		return ResponseEntity.ok(response);
	}

	/**
	 * Health check endpoint - always succeeds.
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of(
			"status", "UP",
			"service", "retry-server",
			"failureRate", String.format("%.0f%%", failureSimulator.getFailureRate() * 100)
		));
	}

	/**
	 * Admin endpoint to dynamically adjust failure rate for testing.
	 */
	@PostMapping("/admin/failure-rate")
	public ResponseEntity<Map<String, String>> setFailureRate(@RequestBody Map<String, Double> request) {
		Double rate = request.get("rate");
		if (rate == null || rate < 0.0 || rate > 1.0) {
			return ResponseEntity.badRequest().body(Map.of("error", "Rate must be between 0.0 and 1.0"));
		}

		failureSimulator.setFailureRate(rate);
		return ResponseEntity.ok(Map.of(
			"message", "Failure rate updated",
			"failureRate", String.format("%.0f%%", rate * 100)
		));
	}

	@GetMapping("/admin/failure-rate")
	public ResponseEntity<Map<String, String>> getFailureRate() {
		return ResponseEntity.ok(Map.of(
			"failureRate", String.format("%.2f", failureSimulator.getFailureRate())
		));
	}
}
