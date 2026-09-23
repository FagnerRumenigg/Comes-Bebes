package org.application.controller.feedback.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.application.model.FeedbackCategory;
import org.application.model.FeedbackStatus;

@Schema(name = "UpdateFeedbackRequest", description = "Atualização administrativa da triagem de feedback.")
public record UpdateFeedbackRequest(
        @NotNull FeedbackCategory category,
        @NotNull FeedbackStatus status
) {
}
