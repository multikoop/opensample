package com.example.opensample;

import com.example.opensample.api.dto.PagedResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleUnitTests {

    @Test
    void pagedResponseShouldExposeMetadata() {
        PagedResponse<String> response = new PagedResponse<>(List.of("a", "b"), 0, 20, 2, 1);

        assertThat(response.items()).containsExactly("a", "b");
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
