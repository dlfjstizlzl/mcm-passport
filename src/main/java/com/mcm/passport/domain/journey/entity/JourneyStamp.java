package com.mcm.passport.domain.journey.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
		name = "journey_stamps",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_journey_stamp_session_spot",
				columnNames = {"passport_session_id", "spot_code"}
		)
)
public class JourneyStamp {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@Column(name = "spot_code", nullable = false, length = 64)
	private String spotCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected JourneyStamp() {
	}

	private JourneyStamp(PassportSession passportSession, String spotCode) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		if (spotCode == null || spotCode.isBlank()) {
			throw new IllegalArgumentException("spotCode must not be blank");
		}
		this.spotCode = spotCode;
		this.createdAt = Instant.now();
	}

	public static JourneyStamp create(PassportSession passportSession, String spotCode) {
		return new JourneyStamp(passportSession, spotCode);
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public String getSpotCode() {
		return spotCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
