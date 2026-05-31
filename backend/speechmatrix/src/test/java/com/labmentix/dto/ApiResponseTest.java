package com.labmentix.speechmatrix.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ApiResponse wrapper tests")
class ApiResponseTest {

    @Test
    @DisplayName("success(message, data) sets all fields correctly")
    void successWithMessageAndData() {
        ApiResponse<String> r = ApiResponse.success("Done", "payload");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo("Done");
        assertThat(r.getData()).isEqualTo("payload");
        assertThat(r.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("success(data) sets null message")
    void successDataOnly() {
        ApiResponse<Integer> r = ApiResponse.success(42);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isNull();
        assertThat(r.getData()).isEqualTo(42);
    }

    @Test
    @DisplayName("message() sets null data")
    void messageOnly() {
        ApiResponse<Void> r = ApiResponse.message("Deleted");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo("Deleted");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("timestamp is always populated")
    void timestampAlwaysSet() {
        assertThat(ApiResponse.success("x").getTimestamp()).isNotNull();
        assertThat(ApiResponse.message("y").getTimestamp()).isNotNull();
        assertThat(ApiResponse.success("msg", "data").getTimestamp()).isNotNull();
    }
}
