package com.planbvalidator.api;

import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.domain.response.AnalyzeResponse;
import com.planbvalidator.resume.ResumePdfExtractor;
import com.planbvalidator.service.AnalyzeOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/v1")
public class AnalyzeController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);

    private final AnalyzeOrchestrator analyzeOrchestrator;
    private final ResumePdfExtractor resumePdfExtractor;
    private final AnalyzeMultipartSupport multipartSupport;

    public AnalyzeController(AnalyzeOrchestrator analyzeOrchestrator,
                             ResumePdfExtractor resumePdfExtractor,
                             AnalyzeMultipartSupport multipartSupport) {
        this.analyzeOrchestrator = analyzeOrchestrator;
        this.resumePdfExtractor = resumePdfExtractor;
        this.multipartSupport = multipartSupport;
    }

    /**
     * JSON-only analyze (no resume). Prefer multipart endpoint when uploading a resume PDF.
     */
    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnalyzeResponse analyzeJson(@Valid @RequestBody AnalyzeRequest request, HttpServletRequest servletRequest) {
        String requestId = String.valueOf(servletRequest.getAttribute("request_id"));
        log.info("analyze event=request_received requestId={} mode=json", requestId);
        return analyzeOrchestrator.analyze(requestId, request, null, com.planbvalidator.pipeline.PipelineProgressListener.NOOP);
    }

    /**
     * Recommended: multipart with JSON {@code request} part + optional {@code resume} PDF file.
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalyzeResponse analyzeMultipart(HttpServletRequest servletRequest) {
        MultipartHttpServletRequest multipart = requireMultipart(servletRequest);
        AnalyzeRequest request = multipartSupport.readRequest(multipart);
        MultipartFile resume = multipartSupport.readResume(multipart);
        String requestId = String.valueOf(servletRequest.getAttribute("request_id"));
        boolean hasResumeFile = resume != null && !resume.isEmpty();
        log.info("analyze event=request_received requestId={} mode=multipart hasResume={}", requestId, hasResumeFile);
        String resumeText = hasResumeFile ? resumePdfExtractor.extractText(resume) : null;
        return analyzeOrchestrator.analyze(requestId, request, resumeText, com.planbvalidator.pipeline.PipelineProgressListener.NOOP);
    }

    private static MultipartHttpServletRequest requireMultipart(HttpServletRequest servletRequest) {
        if (servletRequest instanceof MultipartHttpServletRequest multipart) {
            return multipart;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Expected multipart/form-data request");
    }
}
