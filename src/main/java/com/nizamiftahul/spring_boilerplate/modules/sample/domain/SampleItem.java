package com.nizamiftahul.spring_boilerplate.modules.sample.domain;

import com.nizamiftahul.spring_boilerplate.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sample_items")
public class SampleItem extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	protected SampleItem() {
	}

	public SampleItem(String name, String description, Long ownerId) {
		this.name = name;
		this.description = description;
		this.ownerId = ownerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getOwnerId() {
		return ownerId;
	}
}
