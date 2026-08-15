package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "passport_session_id", unique = true)
	private PassportSession passportSession;

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

	public void connect(PassportSession session) {
		if (status == StyleSpotStatus.CONNECTED && hasSession(session.getId())) {
			return;
		}
		if (status != StyleSpotStatus.WAITING && status != StyleSpotStatus.RESET) {
			throw new BusinessException(ErrorCode.STYLE_SPOT_IN_USE);
		}

		session.enterStyleSpot();
		passportSession = session;
		status = StyleSpotStatus.CONNECTED;
	}

	public long startAnalysis() {
		if (status != StyleSpotStatus.CONNECTED || passportSession == null) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		analysisAttempt++;
		status = StyleSpotStatus.ANALYZING;
		return analysisAttempt;
	}

	public void finishAnalysis(Long passportSessionId, long expectedAttempt) {
		if (!isCurrentAnalysisAttempt(passportSessionId, expectedAttempt)
				|| status != StyleSpotStatus.ANALYZING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		status = StyleSpotStatus.RESULT;
	}

	public void failAnalysis(Long passportSessionId, long expectedAttempt) {
		if (status == StyleSpotStatus.ANALYZING
				&& isCurrentAnalysisAttempt(passportSessionId, expectedAttempt)) {
			status = StyleSpotStatus.CONNECTED;
		}
	}

	public boolean isCurrentAnalysisAttempt(Long passportSessionId, long expectedAttempt) {
		return hasSession(passportSessionId) && analysisAttempt == expectedAttempt;
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
		passportSession = null;
		status = StyleSpotStatus.RESET;
	}

	public boolean hasSession(Long passportSessionId) {
		return passportSession != null && Objects.equals(passportSession.getId(), passportSessionId);
	}

	public String getCode() {
		return code;
	}

	public StyleSpotStatus getStatus() {
		return status;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public long getAnalysisAttempt() {
		return analysisAttempt;
	}
}
