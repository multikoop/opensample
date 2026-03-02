package com.example.opensample.repository;

import com.example.opensample.domain.SampleData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleDataRepository extends JpaRepository<SampleData, Long> {
}
