package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journey_souvenirs")
public class JourneySouvenir {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false, unique = true)
	private PassportSession passportSession;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "style_result_id", nullable = false, unique = true)
	private StyleResult styleResult;

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

	@ElementCollection
	@CollectionTable(name = "journey_souvenir_stamps", joinColumns = @JoinColumn(name = "souvenir_id"))
	@OrderColumn(name = "stamp_order")
	@Column(name = "spot_code", nullable = false, length = 50)
	private List<String> journeyStamps = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "journey_souvenir_products", joinColumns = @JoinColumn(name = "souvenir_id"))
	@OrderColumn(name = "product_order")
	@Column(name = "product_code", nullable = false, length = 80)
	private List<String> taggedProductCodes = new ArrayList<>();

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected JourneySouvenir() {
	}

	private JourneySouvenir(
			PassportSession passportSession,
			StyleResult styleResult,
			JourneyDataSnapshot journeyData
	) {
		this.passportSession = passportSession;
		this.styleResult = styleResult;
		this.cityCode = styleResult.getCityCode();
		this.recommendedProduct = styleResult.getRecommendedProduct();
		this.styleMood = styleResult.getStyleMood();
		this.background = styleResult.getBackground();
		this.journeyStamps = journeyData.stamps().stream()
				.map(JourneyDataSnapshot.StampSignal::spotCode)
				.toList();
		this.taggedProductCodes = journeyData.taggedProducts().stream()
				.map(JourneyDataSnapshot.ProductSignal::code)
				.toList();
		this.createdAt = Instant.now();
	}

	public static JourneySouvenir create(
			PassportSession passportSession,
			StyleResult styleResult,
			JourneyDataSnapshot journeyData
	) {
		return new JourneySouvenir(passportSession, styleResult, journeyData);
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public StyleResult getStyleResult() {
		return styleResult;
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

	public List<String> getJourneyStamps() {
		return List.copyOf(journeyStamps);
	}

	public List<String> getTaggedProductCodes() {
		return List.copyOf(taggedProductCodes);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
