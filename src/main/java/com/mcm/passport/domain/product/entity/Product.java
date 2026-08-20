package com.mcm.passport.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.util.Locale;

@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, length = 100)
	private String category;

	@Column(length = 1000)
	private String description;

	@Column(length = 100)
	private String color;

	@Column(length = 100)
	private String material;

	@Column(length = 100)
	private String silhouette;

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

	@Column(nullable = false)
	private boolean recommendable;

	@Column(nullable = false)
	private boolean active;

	protected Product() {
	}

	private Product(
			String name,
			String category,
			String description,
			String color,
			String material,
			String silhouette,
			String imageUrl,
			boolean recommendable
	) {
		this.name = requireText(name, "name");
		this.category = requireText(category, "category");
		this.description = normalizeNullable(description);
		this.color = normalizeNullable(color);
		this.material = normalizeNullable(material);
		this.silhouette = normalizeNullable(silhouette);
		this.imageUrl = normalizeNullable(imageUrl);
		this.recommendable = recommendable;
		this.active = true;
	}

	public static Product create(
			String name,
			String category,
			String color,
			String material,
			String silhouette,
			String imageUrl,
			boolean recommendable
	) {
		return create(name, category, null, color, material, silhouette, imageUrl, recommendable);
	}

	public static Product create(
			String name,
			String category,
			String description,
			String color,
			String material,
			String silhouette,
			String imageUrl,
			boolean recommendable
	) {
		return new Product(name, category, description, color, material, silhouette, imageUrl, recommendable);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static String normalizeNullable(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public void updateDescription(String description) {
		this.description = normalizeNullable(description);
	}

	public void updateCatalogData(
			String category,
			String description,
			String color,
			String material,
			String silhouette,
			String imageUrl,
			boolean recommendable
	) {
		this.category = requireText(category, "category");
		this.description = normalizeNullable(description);
		this.color = normalizeNullable(color);
		this.material = normalizeNullable(material);
		this.silhouette = normalizeNullable(silhouette);
		this.imageUrl = normalizeNullable(imageUrl);
		this.recommendable = recommendable;
		this.active = true;
	}

	public String getColor() {
		return color;
	}

	public String getMaterial() {
		return material;
	}

	public String getSilhouette() {
		return silhouette;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public boolean isRecommendable() {
		return recommendable;
	}

	public boolean isActive() {
		return active;
	}

	public void deactivate() {
		this.active = false;
	}

	/** Compatibility code used by the current BE2 prototype catalog without an extra DB column. */
	public String getCode() {
		return Normalizer.normalize(name, Normalizer.Form.NFKD)
				.replaceAll("[^A-Za-z0-9]+", "_")
				.replaceAll("^_+|_+$", "")
				.toUpperCase(Locale.ROOT);
	}
}
