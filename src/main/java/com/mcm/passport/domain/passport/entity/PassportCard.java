package com.mcm.passport.domain.passport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
		name = "passport_card",
		uniqueConstraints = @UniqueConstraint(name = "uk_passport_card_card_uid", columnNames = "card_uid")
)
public class PassportCard {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "card_uid", nullable = false, length = 100)
	private String cardUid;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected PassportCard() {
	}

	private PassportCard(String cardUid) {
		this.cardUid = Objects.requireNonNull(cardUid, "cardUid must not be null");
		this.active = true;
		this.createdAt = Instant.now();
	}

	public static PassportCard issue(String cardUid) {
		if (cardUid.isBlank()) {
			throw new IllegalArgumentException("cardUid must not be blank");
		}
		return new PassportCard(cardUid);
	}

	@PrePersist
	private void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getCardUid() {
		return cardUid;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
