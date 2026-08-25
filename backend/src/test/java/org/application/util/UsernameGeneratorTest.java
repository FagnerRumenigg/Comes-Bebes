package org.application.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameGeneratorTest {
    private final UsernameGenerator generator = new UsernameGenerator();

    @Test
    void shouldLowercaseAndStripAccents() {
        assertThat(generator.slugify("João")).isEqualTo("joao");
    }

    @Test
    void shouldTurnSpacesIntoUnderscores() {
        assertThat(generator.slugify("Maria Silva")).isEqualTo("maria_silva");
    }

    @Test
    void shouldRemoveInvalidCharacters() {
        assertThat(generator.slugify("João Editado!!")).isEqualTo("joao_editado");
    }

    @Test
    void shouldTruncateAtTwentyCharacters() {
        assertThat(generator.slugify("um nome de exibicao bem comprido mesmo"))
                .hasSize(20)
                .isEqualTo("um_nome_de_exibicao_");
    }

    @Test
    void shouldPadUpToMinimumOfThreeCharacters() {
        assertThat(generator.slugify("A")).isEqualTo("a00");
        assertThat(generator.slugify("")).isEqualTo("000");
    }

    @Test
    void shouldNotPadWhenAlreadyAtMinimumLength() {
        assertThat(generator.slugify("ana")).isEqualTo("ana");
    }

    @Test
    void shouldFlagReservedWords() {
        assertThat(generator.isReserved("admin")).isTrue();
        assertThat(generator.isReserved("comesebebes")).isTrue();
        assertThat(generator.isReserved("maria")).isFalse();
    }
}
