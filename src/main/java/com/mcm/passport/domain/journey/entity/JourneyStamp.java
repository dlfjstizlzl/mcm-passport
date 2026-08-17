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
		name = "journey_stamp",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_journey_stamp_session_spot",
				columnNames = {"passport_session_id", "journey_spot_id"}
		)
)
public class JourneyStamp {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "journey_spot_id", nullable = false)
	private JourneySpot journeySpot;

	@Column(name = "stamped_at", nullable = false, updatable = false)
	private Instant stampedAt;

	protected JourneyStamp() {
	}

	private JourneyStamp(PassportSession passportSession, JourneySpot journeySpot) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.journeySpot = Objects.requireNonNull(journeySpot, "journeySpot must not be null");
		this.stampedAt = Instant.now();
	}

	public static JourneyStamp create(PassportSession passportSession, JourneySpot journeySpot) {
		return new JourneyStamp(passportSession, journeySpot);
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public JourneySpot getJourneySpot() {
		return journeySpot;
	}

	public Instant getStampedAt() {
		return stampedAt;
	}
}
