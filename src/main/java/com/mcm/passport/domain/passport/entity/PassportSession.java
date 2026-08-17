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
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "passport_sessions")
public class PassportSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PassportSessionStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected PassportSession() {
	}

	private PassportSession(PassportSessionStatus status) {
		this.status = status;
		this.createdAt = Instant.now();
	}

	public static PassportSession start() {
		return new PassportSession(PassportSessionStatus.ACTIVE);
	}

	public static PassportSession readyToBoard() {
		return new PassportSession(PassportSessionStatus.READY_TO_BOARD);
	}

	public void markReadyToBoard() {
		transition(PassportSessionStatus.ACTIVE, PassportSessionStatus.READY_TO_BOARD);
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
	}

	public Long getId() {
		return id;
	}

	public PassportSessionStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}
}
