package org.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI comeSebebesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ComeSebebes API")
                        .version("v1")
                        .description("API RESTful da rede social de comida ComeSebebes.")
                        .contact(new Contact().name("ComeSebebes")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Use o token retornado por POST /auth/login.")))
                ;
    }

    @Bean
    public OpenApiCustomizer requiredResponseProperties() {
        return openAPI -> {
            if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
                return;
            }
            openAPI.getComponents().getSchemas().forEach((name, schema) -> {
                if (name.endsWith("Response") || name.startsWith("PageResponse")) {
                    if (schema.getProperties() != null) {
                        if ("ApiErrorResponse".equals(name)) {
                            schema.setRequired(java.util.List.of("timestamp", "status", "error", "code", "message"));
                        } else {
                            schema.setRequired(schema.getProperties().keySet().stream().toList());
                        }
                    }
                }
            });
        };
    }

    @Bean
    public OpenApiCustomizer protectedOperations() {
        Set<String> publicOperationIds = Set.of(
                "register", "login", "refresh", "getUserById", "findByUsername");
        Set<String> optionalOperationIds = Set.of(
                "feed", "search", "getPublicationById", "getPublicationRecipe", "getUserPublications");
        return openAPI -> openAPI.getPaths().forEach((path, pathItem) ->
                pathItem.readOperations().forEach(operation -> {
                    if (optionalOperationIds.contains(operation.getOperationId())) {
                        operation.setSecurity(java.util.List.of(
                                new SecurityRequirement(),
                                new SecurityRequirement().addList("bearerAuth")));
                    } else if (!publicOperationIds.contains(operation.getOperationId())) {
                        operation.setSecurity(java.util.List.of(new SecurityRequirement().addList("bearerAuth")));
                    }
                }));
    }

    @Bean
    public OpenApiCustomizer nullableReferenceSchemas() {
        return openAPI -> {
            Schema<?> publicationResponse = openAPI.getComponents().getSchemas().get("PublicationResponse");
            if (publicationResponse == null || publicationResponse.getProperties() == null) {
                return;
            }
            Schema<?> recipePreview = (Schema<?>) publicationResponse.getProperties().get("recipePreview");
            if (recipePreview != null) {
                publicationResponse.getProperties().put("recipePreview", nullableReference("RecipeResponse"));
            }

            Schema<?> updateRequest = openAPI.getComponents().getSchemas().get("UpdatePublicationRequest");
            if (updateRequest != null && updateRequest.getProperties() != null
                    && updateRequest.getProperties().containsKey("recipe")) {
                Schema<?> recipe = (Schema<?>) updateRequest.getProperties().get("recipe");
                String description = recipe == null ? null : recipe.getDescription();
                Schema<?> nullableRecipe = nullableReference("CreateRecipeRequest");
                nullableRecipe.setDescription(description);
                updateRequest.getProperties().put("recipe", nullableRecipe);
            }
        };
    }

    private Schema<?> nullableReference(String schemaName) {
        return new ComposedSchema()
                .specVersion(SpecVersion.V31)
                .anyOf(java.util.List.of(
                        new Schema<>().$ref("#/components/schemas/" + schemaName),
                        new Schema<>(SpecVersion.V31).types(java.util.Set.of("null"))
                ));
    }
}
