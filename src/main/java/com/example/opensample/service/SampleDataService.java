package com.example.opensample.service;

import com.example.opensample.api.dto.PagedResponse;
import com.example.opensample.api.dto.SampleDataResponse;
import com.example.opensample.domain.SampleData;
import com.example.opensample.repository.SampleDataRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleDataService {

    private final SampleDataRepository repository;

    public SampleDataService(SampleDataRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<SampleDataResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<SampleDataResponse> result = repository.findAll(pageable).map(this::toResponse);
        return new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public SampleDataResponse findById(long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new SampleDataNotFoundException(id));
    }

    private SampleDataResponse toResponse(SampleData item) {
        return new SampleDataResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCreatedAt()
        );
    }
}
