from __future__ import annotations

import os
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

import psycopg
from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / "infra" / ".env"
IMAGE_ROOT = ROOT / "uploads" / "images"
PASSWORD_HASH = "$2b$10$FH9iJ2AV.QYz/12cXg5KS.ui1xJMWM/yIBGo9o6HkKcZI7tNVDNX2"
DEMO_USERS = [
    ("admin_mikasa", "Mikasa Ackerman", "admin_mikasa@example.test", "ADMIN"),
    ("demo_tanjiro", "Tanjiro Kamado", "demo_tanjiro@example.test", "USER"),
    ("demo_nezuko", "Nezuko Kamado", "demo_nezuko@example.test", "USER"),
    ("demo_luffy", "Monkey D. Luffy", "demo_luffy@example.test", "USER"),
    ("demo_sanji", "Sanji Vinsmoke", "demo_sanji@example.test", "USER"),
    ("demo_hinata", "Hinata Hyuga", "demo_hinata@example.test", "USER"),
    ("demo_edward", "Edward Elric", "demo_edward@example.test", "USER"),
    ("demo_usagi", "Usagi Tsukino", "demo_usagi@example.test", "USER"),
    ("demo_goku", "Son Goku", "demo_goku@example.test", "USER"),
    ("demo_levi", "Levi Ackerman", "demo_levi@example.test", "USER"),
    ("demo_chihiro", "Chihiro Ogino", "demo_chihiro@example.test", "USER"),
]
LEGACY_DEMO_USERNAMES = {"admin_demo", "demo_ana", "demo_bruno", "demo_carla", "demo_diego", "demo_elaine", "demo_felipe", "demo_gabi", "demo_hugo", "demo_isis", "demo_joao"}
POST_TYPES = ("DISH", "DISH", "DISH", "RECIPE", "RECIPE", "RECIPE", "RECIPE", "MY_VERSION", "MY_VERSION", "MY_VERSION")
STATUSES = ("ACTIVE",) * 88 + ("PENDING_VALIDATION",) * 9 + ("UNDER_REVIEW",) * 6 + ("HIDDEN",) * 4 + ("REJECTED",) * 3
POST_CONTENT = (
    ("Lasanha de domingo", "Camadas de massa, molho de tomate e queijo gratinado para um almoço especial.", ("massa de lasanha", "carne moída", "molho de tomate", "muçarela", "parmesão"), "Monte as camadas, cubra com queijo e asse até gratinar."),
    ("Ramen com legumes", "Um caldo reconfortante com macarrão, legumes crocantes e ovo marinado.", ("macarrão para ramen", "caldo de legumes", "cenoura", "cogumelo", "ovo"), "Aqueça o caldo, cozinhe o macarrão e finalize com os legumes e o ovo."),
    ("Pão de queijo mineiro", "Pãezinhos dourados por fora e macios por dentro para acompanhar o café.", ("polvilho doce", "queijo meia-cura", "leite", "ovo", "manteiga"), "Misture os ingredientes, modele as bolinhas e asse até dourar."),
    ("Bolo de chocolate fofinho", "Bolo úmido de chocolate com cobertura simples para dividir com a família.", ("farinha de trigo", "cacau em pó", "açúcar", "ovos", "chocolate"), "Bata a massa, asse em forno médio e cubra depois de frio."),
    ("Yakissoba caseiro", "Macarrão salteado com molho encorpado, frango e muitos vegetais.", ("macarrão", "frango", "repolho", "cenoura", "shoyu"), "Salteie o frango e os legumes, junte o macarrão e envolva no molho."),
    ("Tacos de frango", "Tortilhas crocantes recheadas com frango temperado, pico de gallo e limão.", ("tortilhas", "frango", "tomate", "cebola roxa", "limão"), "Tempere e grelhe o frango, monte os tacos e finalize com o pico de gallo."),
    ("Risoto de cogumelos", "Risoto cremoso com cogumelos dourados e parmesão no ponto certo.", ("arroz arbóreo", "cogumelos", "caldo de legumes", "vinho branco", "parmesão"), "Refogue o arroz e adicione o caldo aos poucos, finalizando com cogumelos e parmesão."),
    ("Curry de grão-de-bico", "Prato aromático, cremoso e cheio de especiarias para um jantar sem carne.", ("grão-de-bico", "leite de coco", "tomate", "cebola", "curry"), "Refogue os temperos, junte os ingredientes e cozinhe até o molho encorpar."),
    ("Panquecas com frutas", "Panquecas macias com frutas frescas e mel para começar bem o dia.", ("farinha", "leite", "ovo", "banana", "mel"), "Prepare a massa, doure as panquecas em frigideira e sirva com as frutas."),
    ("Feijoada de sábado", "Uma versão caseira e caprichada do clássico brasileiro, servida com arroz e couve.", ("feijão preto", "carne seca", "linguiça", "cebola", "couve"), "Cozinhe o feijão com as carnes e sirva com arroz, couve e farofa."),
)


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key] = value
    return values


def create_image(source_path: Path, path: Path) -> tuple[int, int, int]:
    path.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source_path) as source_image:
        image = ImageOps.exif_transpose(source_image).convert("RGB")
        width, height = image.size
        image.save(path, format="WEBP", quality=85, method=6)
    return width, height, path.stat().st_size


def connect_config() -> dict[str, str | int]:
    env = load_env()
    return {
        "host": "localhost",
        "port": int(env.get("SHARED_DB_PORT", "5432")),
        "dbname": env["COMESEBEBES_DB_NAME"],
        "user": env["SHARED_DB_USERNAME"],
        "password": env["SHARED_DB_PASSWORD"],
    }


def main() -> None:
    connection_config = connect_config()
    food_images = sorted((ROOT / "validator" / "data" / "food").glob("*.jpg"))
    if len(food_images) < len(DEMO_USERS) * len(POST_TYPES):
        raise RuntimeError("O dataset de comida não possui imagens suficientes para o seed demo.")
    old_image_paths: list[Path] = []
    with psycopg.connect(**connection_config) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT id, code FROM application.reaction_types")
            reaction_types = {code: reaction_id for reaction_id, code in cursor.fetchall()}
            cursor.execute("SELECT id, code FROM application.report_reasons")
            report_reasons = {code: reason_id for reason_id, code in cursor.fetchall()}
            if not reaction_types or not report_reasons:
                raise RuntimeError("As tabelas de tipos de reação e motivos de denúncia ainda não foram populadas.")

            demo_usernames = [user[0] for user in DEMO_USERS] + list(LEGACY_DEMO_USERNAMES)
            cursor.execute("SELECT id FROM application.users WHERE username = ANY(%s)", (demo_usernames,))
            existing_user_ids = [row[0] for row in cursor.fetchall()]
            if existing_user_ids:
                cursor.execute(
                    """
                    DELETE FROM application.user_notifications
                    WHERE user_id = ANY(%s)
                       OR moderation_case_id IN (
                           SELECT moderation_cases.id
                           FROM application.moderation_cases
                           JOIN application.publications
                             ON publications.id = moderation_cases.publication_id
                           WHERE publications.author_id = ANY(%s)
                       )
                    """,
                    (existing_user_ids, existing_user_ids),
                )
                cursor.execute("SELECT gcs_object_name FROM application.publications WHERE author_id = ANY(%s)", (existing_user_ids,))
                old_image_paths = [IMAGE_ROOT / object_name for (object_name,) in cursor.fetchall() if object_name.startswith("images/demo-")]
                cursor.execute("DELETE FROM application.publications WHERE author_id = ANY(%s)", (existing_user_ids,))
                cursor.execute("DELETE FROM application.users WHERE id = ANY(%s)", (existing_user_ids,))

            user_ids: list[uuid.UUID] = []
            for username, display_name, email, role in DEMO_USERS:
                user_id = uuid.uuid4()
                user_ids.append(user_id)
                cursor.execute(
                    """
                    INSERT INTO application.users
                        (id, email, password_hash, username, display_name, role, status, show_reaction_counts)
                    VALUES (%s, %s, %s, %s, %s, %s, 'ACTIVE', TRUE)
                    """,
                    (user_id, email, PASSWORD_HASH, username, display_name, role),
                )

            admin_id = user_ids[0]
            now = datetime.now(timezone.utc)
            publication_ids: list[tuple[uuid.UUID, uuid.UUID, str, str]] = []
            recipe_ids: list[uuid.UUID] = []
            post_index = 0

            for user_index, user_id in enumerate(user_ids):
                user_recipe_ids: list[uuid.UUID] = []
                for post_number, post_type in enumerate(POST_TYPES, start=1):
                    publication_id = uuid.uuid4()
                    status = STATUSES[post_index]
                    content_title, base_description, ingredients, instructions = POST_CONTENT[post_number - 1]
                    title = content_title if post_type == "RECIPE" else f"{content_title} da cozinha do {DEMO_USERS[user_index][1]}"
                    if post_type == "MY_VERSION":
                        title = f"Minha versão: {content_title}"
                    description = f"{base_description} Publicado por {DEMO_USERS[user_index][1]} para testar o frontend."
                    object_name = f"images/demo-{publication_id}.webp"
                    image_path = IMAGE_ROOT / object_name
                    width, height, image_size = create_image(food_images[post_index], image_path)
                    published_at = None if status in {"PENDING_VALIDATION", "REJECTED"} else now - timedelta(days=post_index)
                    visibility = "INTERNAL" if post_index % 13 == 0 else "PUBLIC"
                    cursor.execute(
                        """
                        INSERT INTO application.publications
                            (id, author_id, type, visibility, title, description, gcs_bucket,
                             gcs_object_name, gcs_generation, image_format, image_size_bytes,
                             image_width, image_height, status, published_at)
                        VALUES (%s, %s, %s, %s, %s, %s, 'comesebebes-local-images', %s, 1,
                                'webp', %s, %s, %s, %s, %s)
                        """,
                        (publication_id, user_id, post_type, visibility, title, description,
                         object_name, image_size, width, height, status, published_at),
                    )
                    publication_ids.append((publication_id, user_id, post_type, status))

                    if post_type in {"RECIPE", "MY_VERSION"}:
                        recipe_id = publication_id
                        recipe_ids.append(recipe_id)
                        user_recipe_ids.append(recipe_id)
                        cursor.execute(
                            """
                            INSERT INTO application.recipes
                                (publication_id, yield_quantity, yield_unit, instructions)
                            VALUES (%s, %s, 'porções', %s)
                            """,
                            (recipe_id, 2 + (post_number % 5), instructions),
                        )
                        for position in range(1, 6):
                            cursor.execute(
                                """
                                INSERT INTO application.recipe_ingredients
                                    (id, recipe_id, position, name, quantity, unit, note)
                                VALUES (%s, %s, %s, %s, %s, 'unidade', %s)
                                """,
                                (uuid.uuid4(), recipe_id, position, ingredients[position - 1], position * 50, "a gosto" if position == 5 else None),
                            )

                    check_status = "APPROVED" if status == "ACTIVE" else "UNCERTAIN" if status == "PENDING_VALIDATION" else "REJECTED"
                    cursor.execute(
                        """
                        INSERT INTO application.publication_image_checks
                            (id, publication_id, check_type, provider, attempt_number, status, confidence)
                        VALUES (%s, %s, 'FOOD', 'demo-seed', 1, %s, %s)
                        """,
                        (uuid.uuid4(), publication_id, check_status, 0.99 if check_status == "APPROVED" else 0.35),
                    )
                    post_index += 1

                for derived_publication_id, _, source_type, _ in publication_ids[-10:]:
                    if source_type == "MY_VERSION":
                        cursor.execute(
                            """
                            INSERT INTO application.publication_origins
                                (derived_publication_id, source_recipe_id, source_title_snapshot, title_suffix, change_summary)
                            VALUES (%s, %s, %s, %s, %s)
                            """,
                            (derived_publication_id, user_recipe_ids[0],
                             POST_CONTENT[3][0], "com toque especial", "Troquei alguns ingredientes e ajustei o tempero para criar uma versão da receita base."),
                        )

            for index, (publication_id, user_id, _, status) in enumerate(publication_ids):
                if status == "ACTIVE" and index % 3 == 0:
                    reaction_code = list(reaction_types)[index % len(reaction_types)]
                    cursor.execute(
                        """
                        INSERT INTO application.publication_reactions
                            (publication_id, user_id, reaction_type_id)
                        VALUES (%s, %s, %s)
                        ON CONFLICT DO NOTHING
                        """,
                        (publication_id, user_ids[(index + 1) % len(user_ids)], reaction_types[reaction_code]),
                    )
                if status == "ACTIVE" and index % 5 == 0:
                    cursor.execute(
                        """
                        INSERT INTO application.saved_publications (user_id, publication_id)
                        VALUES (%s, %s)
                        ON CONFLICT DO NOTHING
                        """,
                        (user_ids[(index + 2) % len(user_ids)], publication_id),
                    )

            for index, (publication_id, user_id, _, status) in enumerate(publication_ids):
                if status not in {"UNDER_REVIEW", "HIDDEN", "REJECTED"}:
                    continue
                case_id = uuid.uuid4()
                case_status = "PENDING" if status == "UNDER_REVIEW" else "REMOVED"
                reviewed_values = (None, None, None) if case_status == "PENDING" else (admin_id, now, "Caso resolvido para teste do frontend.")
                cursor.execute(
                    """
                    INSERT INTO application.moderation_cases
                        (id, publication_id, status, report_count_at_open, reviewed_by, reviewed_at, decision_note)
                    VALUES (%s, %s, %s, 3, %s, %s, %s)
                    """,
                    (case_id, publication_id, case_status, *reviewed_values),
                )
                cursor.execute(
                    """
                    INSERT INTO application.reports
                        (id, publication_id, reporter_id, reason_id, description, moderation_case_id)
                    VALUES (%s, %s, %s, %s, 'Denúncia fictícia para teste do frontend.', %s)
                    """,
                    (uuid.uuid4(), publication_id, user_ids[(index + 1) % len(user_ids)], report_reasons["NOT_FOOD"], case_id),
                )

            cursor.execute(
                """
                INSERT INTO application.user_notifications
                    (id, user_id, type, moderation_case_id, publication_id)
                SELECT %s, %s, 'REPORT_REJECTED_WARNING', id, publication_id
                FROM application.moderation_cases
                WHERE status = 'REMOVED'
                ORDER BY opened_at DESC
                LIMIT 1
                """,
                (uuid.uuid4(), user_ids[1]),
            )

        connection.commit()

    removed_images = 0
    for old_image_path in old_image_paths:
        if old_image_path.is_file():
            old_image_path.unlink()
            removed_images += 1

    print("Seed concluído: 11 usuários, 110 publicações e 110 fotos reais de comida convertidas para WebP.")
    print("Senha dos usuários demo: Teste123!")
    print(f"Imagens demo antigas removidas: {removed_images}.")


if __name__ == "__main__":
    main()
