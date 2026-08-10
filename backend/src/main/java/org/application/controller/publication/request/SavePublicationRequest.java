package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "SavePublicationRequest", description = "A identidade vem do token autenticado.")
public record SavePublicationRequest(@Schema(hidden = true) java.util.UUID userId) {
}
