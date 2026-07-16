package com.nizamiftahul.spring_boilerplate.modules.sample.web;

import com.nizamiftahul.spring_boilerplate.modules.sample.application.SampleService;
import com.nizamiftahul.spring_boilerplate.modules.sample.domain.SampleItem;
import com.nizamiftahul.spring_boilerplate.modules.sample.web.dto.CreateSampleRequest;
import com.nizamiftahul.spring_boilerplate.modules.sample.web.dto.SampleResponse;
import com.nizamiftahul.spring_boilerplate.modules.sample.web.dto.UpdateSampleRequest;
import com.nizamiftahul.spring_boilerplate.modules.user.api.UserApi;
import com.nizamiftahul.spring_boilerplate.platform.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/samples")
public class SampleController {

	private final SampleService sampleService;
	private final UserApi userApi;

	public SampleController(SampleService sampleService, UserApi userApi) {
		this.sampleService = sampleService;
		this.userApi = userApi;
	}

	@GetMapping
	public List<SampleResponse> findAll() {
		return sampleService.findAll().stream().map(this::toResponse).toList();
	}

	@GetMapping("/{id}")
	public SampleResponse findById(@PathVariable Long id) {
		return toResponse(sampleService.findById(id));
	}

	@PostMapping
	public SampleResponse create(@Valid @RequestBody CreateSampleRequest request, Principal principal) {
		Long ownerId = userApi.findByUsername(principal.getName())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getName()))
				.id();
		return toResponse(sampleService.create(request.name(), request.description(), ownerId));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public SampleResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSampleRequest request) {
		return toResponse(sampleService.update(id, request.name(), request.description()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		sampleService.delete(id);
	}

	private SampleResponse toResponse(SampleItem item) {
		return new SampleResponse(item.getId(), item.getName(), item.getDescription(), item.getOwnerId());
	}
}
