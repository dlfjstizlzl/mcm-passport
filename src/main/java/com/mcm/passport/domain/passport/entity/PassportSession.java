package com.mcm.passport.domain.passport.entity;

import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "passport_session")
public class PassportSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_card_id", nullable = false)
	private PassportCard passportCard;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PassportSessionStatus status;

	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PassportSession() {
	}

	private PassportSession(PassportCard passportCard, PassportSessionStatus status) {
		this.passportCard = Objects.requireNonNull(passportCard, "passportCard must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		Instant now = Instant.now();
		this.startedAt = now;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static PassportSession start(PassportCard passportCard) {
		return new PassportSession(passportCard, PassportSessionStatus.EXPLORING);
	}

	public static PassportSession readyToBoard(PassportCard passportCard) {
		return new PassportSession(passportCard, PassportSessionStatus.READY_TO_BOARD);
	}

	public void markReadyToBoard() {
		transition(PassportSessionStatus.EXPLORING, PassportSessionStatus.READY_TO_BOARD);
	}

	public void enterStyleSpot() {
		transition(PassportSessionStatus.READY_TO_BOARD, PassportSessionStatus.STYLE_SPOT);
	}

	public void returnToReadyToBoard() {
		transition(PassportSessionStatus.STYLE_SPOT, PassportSessionStatus.READY_TO_BOARD);
	}

	public void complete() {
		transition(PassportSessionStatus.STYLE_SPOT, PassportSessionStatus.COMPLETED);
		this.completedAt = Instant.now();
	}

	private void transition(PassportSessionStatus expected, PassportSessionStatus target) {
		if (status != expected) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		this.status = target;
		this.updatedAt = Instant.now();
	}

	@PrePersist
	private void initializeTimestamps() {
		Instant now = Instant.now();
		if (startedAt == null) {
			startedAt = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	private void updateTimestamp() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public PassportCard getPassportCard() {
		return passportCard;
	}

	public PassportSessionStatus getStatus() {
		return status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
