package com.planbvalidator.api;

import com.planbvalidator.domain.request.RunwayCalculateRequest;
import com.planbvalidator.domain.response.RunwayCalculateResponse;
import com.planbvalidator.scoring.RunwayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/runway")
public class RunwayController {

    private final RunwayService runwayService;

    public RunwayController(RunwayService runwayService) {
        this.runwayService = runwayService;
    }

    @PostMapping("/calculate")
    public RunwayCalculateResponse calculate(@Valid @RequestBody RunwayCalculateRequest request) {
        return runwayService.calculate(request);
    }
}
