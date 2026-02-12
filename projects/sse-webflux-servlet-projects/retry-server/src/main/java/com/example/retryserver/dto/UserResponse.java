package com.example.retryserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	@JsonProperty("user_id")
	private Long userId;

	@JsonProperty("user_name")
	private String userName;

	@JsonProperty("email_address")
	private String emailAddress;

	@JsonProperty("phone_number")
	private String phoneNumber;

	@JsonProperty("account_status")
	private String accountStatus;

	@JsonProperty("created_at")
	private LocalDateTime createdAt;

	@JsonProperty("last_login")
	private LocalDateTime lastLogin;

	public static UserResponse of(Long id) {
		return new UserResponse(
			id,
			"User " + id,
			"user" + id + "@example.com",
			"+1-555-" + String.format("%04d", id),
			"ACTIVE",
			LocalDateTime.now().minusDays(id * 10L),
			LocalDateTime.now().minusHours(id)
		);
	}
}
