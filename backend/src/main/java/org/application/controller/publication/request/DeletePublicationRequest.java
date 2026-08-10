package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "DeletePublicationRequest", description = "A identidade do autor vem do token.")
public record DeletePublicationRequest(@Schema(hidden = true) java.util.UUID authorId) {
}
