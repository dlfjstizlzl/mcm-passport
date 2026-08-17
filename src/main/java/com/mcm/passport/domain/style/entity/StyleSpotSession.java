package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
		name = "style_spot_sessions",
		indexes = {
				@Index(name = "idx_style_spot_session_spot_active", columnList = "style_spot_id, disconnected_at"),
				@Index(name = "idx_style_spot_session_passport", columnList = "passport_session_id")
		},
		uniqueConstraints = @UniqueConstraint(
				name = "uk_style_spot_session_active_spot",
				columnNames = "active_spot_key"
		)
)
public class StyleSpotSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "style_spot_id", nullable = false)
	private StyleSpot styleSpot;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StyleSpotSessionStatus status;

	@Column(name = "connected_at", nullable = false, updatable = false)
	private Instant connectedAt;

	@Column(name = "disconnected_at")
	private Instant disconnectedAt;

	@Column(name = "active_spot_key", length = 50)
	private String activeSpotKey;

	protected StyleSpotSession() {
	}

	private StyleSpotSession(StyleSpot styleSpot, PassportSession passportSession) {
		this.styleSpot = Objects.requireNonNull(styleSpot, "styleSpot must not be null");
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.status = StyleSpotSessionStatus.CONNECTED;
		this.connectedAt = Instant.now();
		this.activeSpotKey = styleSpot.getCode();
	}

	public static StyleSpotSession connect(StyleSpot styleSpot, PassportSession passportSession) {
		return new StyleSpotSession(styleSpot, passportSession);
	}

	public void disconnect() {
		if (status == StyleSpotSessionStatus.DISCONNECTED) {
			return;
		}
		status = StyleSpotSessionStatus.DISCONNECTED;
		disconnectedAt = Instant.now();
		activeSpotKey = null;
	}

	public boolean isActive() {
		return status == StyleSpotSessionStatus.CONNECTED && disconnectedAt == null;
	}

	public Long getId() {
		return id;
	}

	public StyleSpot getStyleSpot() {
		return styleSpot;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public StyleSpotSessionStatus getStatus() {
		return status;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

	public Instant getDisconnectedAt() {
		return disconnectedAt;
	}
}
