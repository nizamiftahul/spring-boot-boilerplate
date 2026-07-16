package com.nizamiftahul.spring_boilerplate.modules.sample.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nizamiftahul.spring_boilerplate.modules.sample.domain.SampleItem;
import com.nizamiftahul.spring_boilerplate.modules.sample.domain.SampleItemRepository;
import com.nizamiftahul.spring_boilerplate.modules.user.api.UserApi;
import com.nizamiftahul.spring_boilerplate.modules.user.api.UserSummary;
import com.nizamiftahul.spring_boilerplate.platform.exception.DomainException;
import com.nizamiftahul.spring_boilerplate.platform.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SampleServiceTest {

	private SampleItemRepository sampleItemRepository;
	private UserApi userApi;
	private SampleService sampleService;

	@BeforeEach
	void setUp() {
		sampleItemRepository = mock(SampleItemRepository.class);
		userApi = mock(UserApi.class);
		sampleService = new SampleService(sampleItemRepository, userApi);
	}

	@Test
	void create_persistsWhenOwnerExists() {
		when(userApi.getById(1L)).thenReturn(Optional.of(new UserSummary(1L, "alice", "alice@example.com", "USER")));
		when(sampleItemRepository.save(any(SampleItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SampleItem item = sampleService.create("widget", "a widget", 1L);

		assertThat(item.getName()).isEqualTo("widget");
		assertThat(item.getOwnerId()).isEqualTo(1L);
		verify(sampleItemRepository).save(any(SampleItem.class));
	}

	@Test
	void create_rejectsWhenOwnerMissing() {
		when(userApi.getById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sampleService.create("widget", "a widget", 99L))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void findById_throwsWhenMissing() {
		when(sampleItemRepository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> sampleService.findById(42L)).isInstanceOf(ResourceNotFoundException.class);
	}
}
