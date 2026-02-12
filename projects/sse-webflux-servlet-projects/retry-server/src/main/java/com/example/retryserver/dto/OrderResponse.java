package com.example.retryserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

	@JsonProperty("order_id")
	private Long orderId;

	@JsonProperty("user_id")
	private Long userId;

	@JsonProperty("total_amount")
	private BigDecimal totalAmount;

	@JsonProperty("order_status")
	private String orderStatus;

	@JsonProperty("item_count")
	private Integer itemCount;

	@JsonProperty("shipping_address")
	private String shippingAddress;

	@JsonProperty("order_date")
	private LocalDateTime orderDate;

	@JsonProperty("estimated_delivery")
	private LocalDateTime estimatedDelivery;

	@JsonProperty("payment_method")
	private String paymentMethod;

	public static OrderResponse of(Long id) {
		return new OrderResponse(
			id,
			100L + id,
			new BigDecimal("99." + String.format("%02d", (id % 100))),
			"PROCESSING",
			(int) ((id % 10) + 1),
			id + " Main St, City, State " + String.format("%05d", id),
			LocalDateTime.now().minusDays(id),
			LocalDateTime.now().plusDays(5 + (id % 5)),
			"CREDIT_CARD"
		);
	}
}
