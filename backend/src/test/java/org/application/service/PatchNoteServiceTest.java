package org.application.service;

import org.application.model.PatchNote;
import org.application.model.User;
import org.application.repository.PatchNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchNoteServiceTest {

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @InjectMocks
    private PatchNoteService service;

    @Test
    void shouldReportUnseenWhenLatestNoteIsNewerThanLastSeen() {
        User user = User.builder().lastSeenPatchNoteAt(OffsetDateTime.parse("2026-08-01T00:00:00Z")).build();
        PatchNote latest = PatchNote.builder().publishedAt(OffsetDateTime.parse("2026-08-05T00:00:00Z")).build();
        when(patchNoteRepository.findTopByOrderByPublishedAtDesc()).thenReturn(Optional.of(latest));

        assertThat(service.hasUnseen(user)).isTrue();
    }

    @Test
    void shouldReportNoUnseenWhenLastSeenIsAfterLatestNote() {
        User user = User.builder().lastSeenPatchNoteAt(OffsetDateTime.parse("2026-08-10T00:00:00Z")).build();
        PatchNote latest = PatchNote.builder().publishedAt(OffsetDateTime.parse("2026-08-05T00:00:00Z")).build();
        when(patchNoteRepository.findTopByOrderByPublishedAtDesc()).thenReturn(Optional.of(latest));

        assertThat(service.hasUnseen(user)).isFalse();
    }

    @Test
    void shouldReportNoUnseenWhenThereAreNoPatchNotes() {
        User user = User.builder().lastSeenPatchNoteAt(OffsetDateTime.now()).build();
        when(patchNoteRepository.findTopByOrderByPublishedAtDesc()).thenReturn(Optional.empty());

        assertThat(service.hasUnseen(user)).isFalse();
    }

    @Test
    void shouldTreatMissingLastSeenAsNeverSeenAnything() {
        User user = User.builder().lastSeenPatchNoteAt(null).build();
        PatchNote latest = PatchNote.builder().publishedAt(OffsetDateTime.now()).build();
        when(patchNoteRepository.findTopByOrderByPublishedAtDesc()).thenReturn(Optional.of(latest));

        assertThat(service.hasUnseen(user)).isTrue();
    }

    @Test
    void shouldListNotesPublishedAfterLastSeen() {
        OffsetDateTime lastSeen = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        User user = User.builder().lastSeenPatchNoteAt(lastSeen).build();
        List<PatchNote> unseen = List.of(PatchNote.builder().title("Novidades").build());
        when(patchNoteRepository.findByPublishedAtAfterOrderByPublishedAtAsc(lastSeen)).thenReturn(unseen);

        assertThat(service.findUnseen(user)).isEqualTo(unseen);
    }

    @Test
    void shouldListAllNotesWhenUserNeverSawAny() {
        User user = User.builder().lastSeenPatchNoteAt(null).build();
        List<PatchNote> all = List.of(PatchNote.builder().title("Lançamento").build());
        when(patchNoteRepository.findAllByOrderByPublishedAtAsc()).thenReturn(all);

        assertThat(service.findUnseen(user)).isEqualTo(all);
    }
}
