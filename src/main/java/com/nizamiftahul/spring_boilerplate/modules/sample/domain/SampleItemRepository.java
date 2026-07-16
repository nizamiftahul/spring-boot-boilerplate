package com.nizamiftahul.spring_boilerplate.modules.sample.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleItemRepository extends JpaRepository<SampleItem, Long> {
}
