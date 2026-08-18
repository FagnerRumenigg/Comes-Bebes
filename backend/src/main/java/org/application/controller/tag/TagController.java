package org.application.controller.tag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.application.controller.tag.response.TagResponse;
import org.application.service.TagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tags", description = "Busca e autocomplete de tags de alimento.")
public class TagController {

    private final TagService tagService;

    @GetMapping("/search")
    @Operation(operationId = "searchTags", summary = "Buscar tags", description = "Autocomplete por prefixo do nome, sem diferenciar acento nem maiúscula/minúscula. Tags oficiais aparecem primeiro.")
    @ApiResponse(responseCode = "200", description = "Resultados retornados (até 10).")
    public List<TagResponse> search(
            @Parameter(description = "Termo digitado pelo usuário.", example = "aca")
            @RequestParam String q
    ) {
        return tagService.search(q, 10).stream().map(TagResponse::of).toList();
    }
}
