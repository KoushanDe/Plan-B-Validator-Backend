package com.planbvalidator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.TestFixtures;
import com.planbvalidator.domain.request.AnalyzeRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzeMultipartSupportTest {

    private AnalyzeMultipartSupport support;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        AnalyzeRequestPartReader reader = new AnalyzeRequestPartReader(new ObjectMapper(), validator);
        support = new AnalyzeMultipartSupport(reader);
    }

    @Test
    void shouldReadRequestFromNamedFilePart() {
        String json = TestFixtures.minimalAnalyzeRequestJson();
        MockMultipartHttpServletRequest multipart = new MockMultipartHttpServletRequest();
        multipart.addFile(new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_OCTET_STREAM_VALUE, json.getBytes()));

        AnalyzeRequest request = support.readRequest(multipart);

        assertEquals("Engineer", request.profile().currentProfession());
    }

    @Test
    void shouldReadRequestFromFormField() {
        MockMultipartHttpServletRequest multipart = new MockMultipartHttpServletRequest();
        multipart.addParameter("request", TestFixtures.minimalAnalyzeRequestJson());

        AnalyzeRequest request = support.readRequest(multipart);

        assertEquals("Engineer", request.profile().currentProfession());
    }

    @Test
    void shouldReadRequestFromAlternatePartName() {
        MockMultipartHttpServletRequest multipart = new MockMultipartHttpServletRequest();
        multipart.addFile(new MockMultipartFile(
                "payload", "payload.json", MediaType.APPLICATION_JSON_VALUE,
                TestFixtures.minimalAnalyzeRequestJson().getBytes()));

        AnalyzeRequest request = support.readRequest(multipart);

        assertEquals("Engineer", request.profile().currentProfession());
    }

}
