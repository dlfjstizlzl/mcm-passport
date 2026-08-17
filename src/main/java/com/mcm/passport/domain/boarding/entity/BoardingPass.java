package com.mcm.passport.domain.boarding.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "boarding_pass", uniqueConstraints = @UniqueConstraint(
		name = "uk_boarding_pass_passport_session", columnNames = "passport_session_id"))
public class BoardingPass {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@Column(nullable = false, length = 50)
	private String gate;

	@Column(name = "issued_at", nullable = false, updatable = false)
	private Instant issuedAt;

	protected BoardingPass() {}

	private BoardingPass(PassportSession passportSession, String gate) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		if (gate == null || gate.isBlank()) throw new IllegalArgumentException("gate must not be blank");
		this.gate = gate.trim();
		this.issuedAt = Instant.now();
	}

	public static BoardingPass issue(PassportSession passportSession, String gate) {
		return new BoardingPass(passportSession, gate);
	}

	public Long getId() { return id; }
	public PassportSession getPassportSession() { return passportSession; }
	public String getGate() { return gate; }
	public Instant getIssuedAt() { return issuedAt; }
}
