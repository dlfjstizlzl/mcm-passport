package com.mcm.passport.domain.style.entity;

import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Objects;

@Entity
@Table(name = "style_spots")
public class StyleSpot {

	@Id
	@Column(length = 50)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StyleSpotStatus status;

	@Column(name = "analysis_attempt", nullable = false)
	private long analysisAttempt;

	@Version
	private Long version;

	protected StyleSpot() {
	}

	private StyleSpot(String code) {
		this.code = Objects.requireNonNull(code, "code must not be null");
		this.status = StyleSpotStatus.WAITING;
	}

	public static StyleSpot waiting(String code) {
		return new StyleSpot(code);
	}

	public void connect() {
		if (status != StyleSpotStatus.WAITING && status != StyleSpotStatus.RESET) {
			throw new BusinessException(ErrorCode.STYLE_SPOT_IN_USE);
		}

		status = StyleSpotStatus.CONNECTED;
	}

	public long startAnalysis() {
		if (status != StyleSpotStatus.CONNECTED) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		analysisAttempt++;
		status = StyleSpotStatus.ANALYZING;
		return analysisAttempt;
	}

	public void finishAnalysis(long expectedAttempt) {
		if (!isCurrentAnalysisAttempt(expectedAttempt) || status != StyleSpotStatus.ANALYZING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		status = StyleSpotStatus.RESULT;
	}

	public void failAnalysis(long expectedAttempt) {
		if (status == StyleSpotStatus.ANALYZING
				&& isCurrentAnalysisAttempt(expectedAttempt)) {
			status = StyleSpotStatus.CONNECTED;
		}
	}

	public boolean isCurrentAnalysisAttempt(long expectedAttempt) {
		return analysisAttempt == expectedAttempt;
	}

	public void reset() {
		if (status == StyleSpotStatus.RESET) {
			return;
		}
		if (status != StyleSpotStatus.CONNECTED
				&& status != StyleSpotStatus.ANALYZING
				&& status != StyleSpotStatus.RESULT) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		status = StyleSpotStatus.RESET;
	}

	public String getCode() {
		return code;
	}

	public StyleSpotStatus getStatus() {
		return status;
	}

	public long getAnalysisAttempt() {
		return analysisAttempt;
	}
}
