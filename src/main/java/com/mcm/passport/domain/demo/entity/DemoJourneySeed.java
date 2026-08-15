package com.mcm.passport.domain.demo.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
		name = "demo_journey_seeds",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_demo_journey_seed_session",
				columnNames = "passport_session_id"
		)
)
public class DemoJourneySeed {

	@Id
	@Column(name = "seed_key", nullable = false, updatable = false, length = 64)
	private String seedKey;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected DemoJourneySeed() {
	}

	private DemoJourneySeed(String seedKey, PassportSession passportSession) {
		this.seedKey = requireText(seedKey);
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.createdAt = Instant.now();
	}

	public static DemoJourneySeed create(String seedKey, PassportSession passportSession) {
		return new DemoJourneySeed(seedKey, passportSession);
	}

	private static String requireText(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("seedKey must not be blank");
		}
		return value;
	}

	public String getSeedKey() {
		return seedKey;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public void replacePassportSession(PassportSession passportSession) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
