package org.application.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PublicationReactionId implements Serializable {
    private UUID publicationId;
    private UUID userId;
    private Short reactionTypeId;
}
