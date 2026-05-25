package com.planbvalidator.resume;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class ResumePdfExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResumePdfExtractor.class);

    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_EXTRACTED_CHARS = 20_000;

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resume PDF file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resume PDF must be at most 10MB");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!filename.endsWith(".pdf") && !contentType.contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resume must be a PDF file");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "could not extract text from PDF resume");
            }
            String normalized = text.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
            int originalLen = normalized.length();
            String extracted = originalLen <= MAX_EXTRACTED_CHARS
                    ? normalized
                    : normalized.substring(0, MAX_EXTRACTED_CHARS);
            log.info("resume event=pdf_extracted chars={} truncated={} filename={}",
                    originalLen, originalLen > MAX_EXTRACTED_CHARS, filename);
            return extracted;
        } catch (ResponseStatusException e) {
            log.warn("resume event=pdf_extract_failed reason={}", e.getReason());
            throw e;
        } catch (IOException e) {
            log.warn("resume event=pdf_extract_failed reason=invalid_or_corrupted", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid or corrupted PDF resume");
        }
    }
}
