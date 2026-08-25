package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.ContentDocument;
import org.application.repository.ContentDocumentRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentDocumentService {

    private final ContentDocumentRepository contentDocumentRepository;

    @Transactional(readOnly = true)
    public ContentDocument findBySlug(String slug) {
        return contentDocumentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "Documento não encontrado."));
    }
}
