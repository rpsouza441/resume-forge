package com.resumeforge.export.controller;

import com.resumeforge.auth.security.UserPrincipal;
import com.resumeforge.export.service.DocxGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ExportController {

    @Autowired
    private DocxGenerationService docxService;

    /**
     * Downloads a generated resume as a DOCX file.
     *
     * Filename pattern: [Nome]-otimizado-[Empresa]-[YYYY-MM-DD].docx
     *
     * @param id the generated resume ID
     * @return the DOCX file as a binary download
     */
    @GetMapping("/generated/{id}/docx")
    public ResponseEntity<?> downloadDocx(@PathVariable UUID id) {
        // Extract userId from JWT via SecurityContextHolder
        UUID userId = extractUserIdFromSecurityContext();

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "error", "unauthorized",
                            "message", "Nao autenticado.",
                            "timestamp", java.time.OffsetDateTime.now().toString()
                    ));
        }

        // Generate DOCX (returns bytes + computed filename)
        DocxGenerationService.DocxResult result = docxService.generateDocx(id, userId);
        byte[] docxBytes = result.bytes();
        String filename = result.filename();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(docxBytes.length)
                .body(new ByteArrayResource(docxBytes));
    }

    /**
     * Extracts the user ID from the current SecurityContext.
     */
    private UUID extractUserIdFromSecurityContext() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        return null;
    }
}
