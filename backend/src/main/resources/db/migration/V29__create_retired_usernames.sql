-- Fase 2 do plano de redefinição de UX (docs/PLANO_BACKEND_UX.md, item 2.2).
-- Ao trocar de @usuário, o antigo fica reservado por 30 dias (impl10.md v10 §19.4)
-- antes de poder ser usado por outra pessoa. Sem chave única em username: a mesma
-- string pode passar por retirada mais de uma vez ao longo do tempo (dono A retira,
-- depois de liberado dono B pega e também retira).
CREATE TABLE application.retired_usernames (
    id uuid PRIMARY KEY,
    username varchar(30) NOT NULL,
    previous_owner_id uuid NOT NULL REFERENCES application.users(id),
    retired_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX retired_usernames_username_idx ON application.retired_usernames (username);
