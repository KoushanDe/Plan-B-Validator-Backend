package com.planbvalidator.resume;

import com.planbvalidator.domain.request.ProfileDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileCompletenessValidatorTest {

    private final ProfileCompletenessValidator validator = new ProfileCompletenessValidator();

    @Test
    void isComplete_whenAllFieldsPresent() {
        assertTrue(validator.isComplete(new ProfileDto("Engineer", "Tech", 5.0, "India", "Bengaluru")));
    }

    @Test
    void isComplete_falseWhenProfessionMissing() {
        assertFalse(validator.isComplete(new ProfileDto("", "Tech", 5.0, "India", "Bengaluru")));
    }

    @Test
    void isComplete_falseWhenYearsExperienceMissing() {
        assertFalse(validator.isComplete(new ProfileDto("Engineer", "Tech", null, "India", "Bengaluru")));
        assertFalse(validator.isComplete(new ProfileDto("Engineer", "Tech", 0.0, "India", "Bengaluru")));
    }
}
