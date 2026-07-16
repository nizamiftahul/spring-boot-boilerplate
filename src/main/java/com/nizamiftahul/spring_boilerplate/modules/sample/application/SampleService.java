package com.nizamiftahul.spring_boilerplate.modules.sample.application;

import com.nizamiftahul.spring_boilerplate.modules.sample.domain.SampleItem;
import com.nizamiftahul.spring_boilerplate.modules.sample.domain.SampleItemRepository;
import com.nizamiftahul.spring_boilerplate.modules.user.api.UserApi;
import com.nizamiftahul.spring_boilerplate.platform.exception.DomainException;
import com.nizamiftahul.spring_boilerplate.platform.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleService {

	private final SampleItemRepository sampleItemRepository;
	private final UserApi userApi;

	public SampleService(SampleItemRepository sampleItemRepository, UserApi userApi) {
		this.sampleItemRepository = sampleItemRepository;
		this.userApi = userApi;
	}

	public List<SampleItem> findAll() {
		return sampleItemRepository.findAll();
	}

	public SampleItem findById(Long id) {
		return sampleItemRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sample item not found: " + id));
	}

	@Transactional
	public SampleItem create(String name, String description, Long ownerId) {
		userApi.getById(ownerId).orElseThrow(() -> new DomainException("Owner does not exist: " + ownerId));
		return sampleItemRepository.save(new SampleItem(name, description, ownerId));
	}

	@Transactional
	public SampleItem update(Long id, String name, String description) {
		SampleItem item = findById(id);
		item.setName(name);
		item.setDescription(description);
		return sampleItemRepository.save(item);
	}

	@Transactional
	public void delete(Long id) {
		SampleItem item = findById(id);
		sampleItemRepository.delete(item);
	}
}
