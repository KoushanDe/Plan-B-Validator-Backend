package com.planbvalidator.resume;

import com.planbvalidator.domain.request.ProfileDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProfileCompletenessValidator {

    public void requireComplete(ProfileDto profile) {
        if (!hasText(profile.currentProfession())) {
            throw badRequest("profile.currentProfession is required (provide in form or resume)");
        }
        if (!hasText(profile.industry())) {
            throw badRequest("profile.industry is required (provide in form or resume)");
        }
        if (profile.yearsExperience() == null || profile.yearsExperience() <= 0) {
            throw badRequest("profile.yearsExperience is required (provide in form or resume)");
        }
        if (!hasText(profile.country())) {
            throw badRequest("profile.country is required (provide in form or resume)");
        }
        if (!hasText(profile.city())) {
            throw badRequest("profile.city is required (provide in form or resume)");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
