-- V15 criou user_device, mas em produção essa migração acabou sendo executada por uma
-- conexão administrativa (platform_admin) em vez da própria aplicação (comesebebes_app),
-- então a tabela ficou de propriedade do admin e a aplicação não tinha nenhum privilégio
-- sobre ela — login quebrava com "permission denied for table user_device" assim que
-- tentava consultar o dispositivo. Já foi corrigido manualmente em produção; esta migração
-- só torna o conserto reproduzível (idempotente: se comesebebes_app já for dono da tabela,
-- como acontece num ambiente novo, isto é um no-op inofensivo).
--
-- Condicional a "role existe": em ambientes gerenciados (ex.: Azure Database for
-- PostgreSQL Flexible Server) a aplicação conecta com o próprio login administrador
-- (não existe um role separado "comesebebes_app"), e nesses ambientes a tabela já
-- nasce com o dono certo — GRANT pra um role inexistente falharia a migration inteira.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'comesebebes_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER
            ON application.user_device TO comesebebes_app;
    END IF;
END
$$;
