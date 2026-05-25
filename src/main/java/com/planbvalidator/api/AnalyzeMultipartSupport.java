package com.planbvalidator.api;

import com.planbvalidator.domain.request.AnalyzeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves analyze JSON + optional resume from multipart requests.
 * Accepts common part names used by browsers and AI Studio proxies.
 */
@Component
public class AnalyzeMultipartSupport {

    private static final List<String> REQUEST_PART_NAMES = List.of(
            "request", "data", "payload", "body", "analyzeRequest", "json"
    );

    private static final Set<String> RESUME_PART_NAMES = Set.of("resume", "resumePdf", "file", "pdf");

    private final AnalyzeRequestPartReader requestPartReader;

    public AnalyzeMultipartSupport(AnalyzeRequestPartReader requestPartReader) {
        this.requestPartReader = requestPartReader;
    }

    public AnalyzeRequest readRequest(MultipartHttpServletRequest multipart) {
        for (String name : REQUEST_PART_NAMES) {
            MultipartFile file = multipart.getFile(name);
            if (file != null && !file.isEmpty()) {
                return requestPartReader.readPart(file);
            }
            String field = multipart.getParameter(name);
            if (field != null && !field.isBlank() && field.trim().startsWith("{")) {
                return requestPartReader.readJson(field);
            }
        }
        MultipartFile fallback = findFallbackFilePart(multipart);
        if (fallback != null) {
            return requestPartReader.readPart(fallback);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingRequestPartMessage(multipart));
    }

    public MultipartFile readResume(MultipartHttpServletRequest multipart) {
        for (String name : RESUME_PART_NAMES) {
            MultipartFile file = multipart.getFile(name);
            if (file != null && !file.isEmpty()) {
                return file;
            }
        }
        return null;
    }

    private MultipartFile findFallbackFilePart(MultipartHttpServletRequest multipart) {
        for (Map.Entry<String, MultipartFile> entry : multipart.getFileMap().entrySet()) {
            if (RESUME_PART_NAMES.contains(entry.getKey())) {
                continue;
            }
            MultipartFile file = entry.getValue();
            if (file != null && !file.isEmpty() && looksLikeJson(file)) {
                return file;
            }
        }
        return null;
    }

    private static boolean looksLikeJson(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && (name.endsWith(".json") || name.equals("blob"))) {
            return true;
        }
        String contentType = file.getContentType();
        return contentType != null && (contentType.contains("json") || contentType.startsWith("text/"));
    }

    private static String missingRequestPartMessage(MultipartHttpServletRequest multipart) {
        StringBuilder received = new StringBuilder();
        if (!multipart.getFileMap().isEmpty()) {
            received.append("files=[");
            received.append(String.join(", ", multipart.getFileMap().keySet()));
            received.append("]");
        }
        if (multipart.getParameterMap() != null && !multipart.getParameterMap().isEmpty()) {
            if (!received.isEmpty()) {
                received.append(" ");
            }
            received.append("fields=[");
            received.append(String.join(", ", multipart.getParameterMap().keySet()));
            received.append("]");
        }
        if (received.isEmpty()) {
            received.append("none");
        }
        return "Missing analyze JSON in multipart body. Send part 'request' with AnalyzeRequest JSON "
                + "(Blob or string). Received: " + received + ". "
                + "Without a resume, POST application/json instead.";
    }
}
