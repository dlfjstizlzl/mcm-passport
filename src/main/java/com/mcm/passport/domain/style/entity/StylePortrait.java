package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "style_portraits")
public class StylePortrait {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false, unique = true)
	private PassportSession passportSession;

	@Column(name = "image_url", nullable = false, length = 1000)
	private String imageUrl;

	@Column(nullable = false)
	private boolean consent;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected StylePortrait() {
	}

	private StylePortrait(PassportSession passportSession, String imageUrl, boolean consent) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.imageUrl = requireText(imageUrl);
		this.consent = consent;
		this.createdAt = Instant.now();
	}

	public static StylePortrait create(PassportSession passportSession, String imageUrl, boolean consent) {
		return new StylePortrait(passportSession, imageUrl, consent);
	}

	private static String requireText(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			throw new IllegalArgumentException("imageUrl must not be blank");
		}
		return imageUrl.trim();
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public boolean isConsent() {
		return consent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
