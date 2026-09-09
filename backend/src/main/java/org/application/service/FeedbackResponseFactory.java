package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.feedback.response.FeedbackResponse;
import org.application.dto.PageResponse;
import org.application.model.FeedbackSubmission;
import org.application.model.User;
import org.application.repository.UserRepository;
import org.application.util.DateTimeConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta {@link FeedbackResponse} em lote pro painel administrativo — mesmo
 * padrão de {@link NotificationResponseFactory}: busca quem enviou de uma vez,
 * em vez de N+1 por item da página.
 */
@Service
@RequiredArgsConstructor
public class FeedbackResponseFactory {

    private final UserRepository userRepository;

    public PageResponse<FeedbackResponse> of(Page<FeedbackSubmission> page, ZoneId zoneId) {
        Map<UUID, User> usersById = userRepository.findAllById(page.getContent().stream()
                        .map(FeedbackSubmission::getUserId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return PageResponse.of(page, item -> {
            User sender = usersById.get(item.getUserId());
            return FeedbackResponse.builder()
                    .id(item.getId())
                    .message(item.getMessage())
                    .contactEmail(item.getContactEmail())
                    .userId(item.getUserId())
                    .userDisplayName(sender == null ? null : sender.getDisplayName())
                    .username(sender == null ? null : sender.getUsername())
                    .createdAt(DateTimeConverter.toApplicationTime(item.getCreatedAt(), zoneId))
                    .build();
        });
    }
}
