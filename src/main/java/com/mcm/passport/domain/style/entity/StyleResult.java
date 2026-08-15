package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.style.analysis.ValidatedStyleAnalysis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "style_results")
public class StyleResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false, unique = true)
	private PassportSession passportSession;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private CityCode cityCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private RecommendedProduct recommendedProduct;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private StyleMood styleMood;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private CityBackground background;

	@Lob
	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private int matchScore;

	@Column(nullable = false)
	private boolean usedFallback;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected StyleResult() {
	}

	private StyleResult(
			PassportSession passportSession,
			ValidatedStyleAnalysis analysis,
			boolean usedFallback
	) {
		this.passportSession = passportSession;
		this.cityCode = analysis.cityCode();
		this.recommendedProduct = analysis.recommendedProduct();
		this.styleMood = analysis.styleMood();
		this.background = analysis.background();
		this.description = analysis.description();
		this.matchScore = analysis.matchScore();
		this.usedFallback = usedFallback;
		this.createdAt = Instant.now();
	}

	public static StyleResult create(
			PassportSession passportSession,
			ValidatedStyleAnalysis analysis,
			boolean usedFallback
	) {
		return new StyleResult(passportSession, analysis, usedFallback);
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public CityCode getCityCode() {
		return cityCode;
	}

	public RecommendedProduct getRecommendedProduct() {
		return recommendedProduct;
	}

	public StyleMood getStyleMood() {
		return styleMood;
	}

	public CityBackground getBackground() {
		return background;
	}

	public String getDescription() {
		return description;
	}

	public int getMatchScore() {
		return matchScore;
	}

	public boolean isUsedFallback() {
		return usedFallback;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
