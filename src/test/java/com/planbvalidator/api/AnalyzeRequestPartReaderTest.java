package com.planbvalidator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.TestFixtures;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyzeRequestPartReaderTest {

    private AnalyzeRequestPartReader reader;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        reader = new AnalyzeRequestPartReader(new ObjectMapper(), validator);
    }

    @Test
    void shouldParseRequestPartWithOctetStreamContentType() throws Exception {
        String json = TestFixtures.minimalAnalyzeRequestJson();
        MockMultipartFile part = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                json.getBytes());

        var request = reader.readPart(part);

        assertEquals("Engineer", request.profile().currentProfession());
    }

    @Test
    void shouldRejectMissingRequestPart() {
        assertThrows(ResponseStatusException.class, () -> reader.readPart(null));
    }
}
