package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.PatchNote;
import org.application.model.User;
import org.application.repository.PatchNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatchNoteService {

    private final PatchNoteRepository patchNoteRepository;

    @Transactional(readOnly = true)
    public boolean hasUnseen(User user) {
        return patchNoteRepository.findTopByOrderByPublishedAtDesc()
                .map(latest -> isUnseen(latest.getPublishedAt(), user.getLastSeenPatchNoteAt()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<PatchNote> findUnseen(User user) {
        OffsetDateTime lastSeen = user.getLastSeenPatchNoteAt();
        return lastSeen == null
                ? patchNoteRepository.findAllByOrderByPublishedAtAsc()
                : patchNoteRepository.findByPublishedAtAfterOrderByPublishedAtAsc(lastSeen);
    }

    private boolean isUnseen(OffsetDateTime publishedAt, OffsetDateTime lastSeen) {
        return lastSeen == null || publishedAt.isAfter(lastSeen);
    }
}
