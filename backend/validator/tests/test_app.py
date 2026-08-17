from io import BytesIO

from fastapi.testclient import TestClient
from PIL import Image

from validator.app import app
from validator.csat_classifier import CsatClassificationResult


class FakeApprovingClassifier:
    def classify(self, image_path, threshold: float) -> CsatClassificationResult:
        return CsatClassificationResult(food_score=0.91, decision="FOOD")


class FakeRejectingClassifier:
    def classify(self, image_path, threshold: float) -> CsatClassificationResult:
        return CsatClassificationResult(food_score=0.12, decision="NOT_FOOD")


def _jpeg_bytes(size=(1200, 800), color="#d97706") -> BytesIO:
    buffer = BytesIO()
    Image.new("RGB", size, color).save(buffer, format="JPEG")
    buffer.seek(0)
    return buffer


def test_validate_returns_processed_webp_image_and_metadata_headers(monkeypatch) -> None:
    monkeypatch.setattr("validator.app.get_classifier", lambda: FakeApprovingClassifier())
    client = TestClient(app)

    response = client.post(
        "/validate",
        files={"file": ("dish.jpg", _jpeg_bytes(), "image/jpeg")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/webp"
    assert response.headers["x-validation-status"] == "APPROVED"
    assert response.headers["x-source-format"] == "JPEG"
    assert response.headers["x-image-format"] == "WEBP"
    assert response.headers["x-image-width"] == "1200"
    assert response.headers["x-image-height"] == "800"
    assert response.headers["x-food-score"] == "0.91"
    assert response.headers["x-food-threshold"] == "0.5"
    assert response.headers["x-photo-taken-at"] == ""

    with Image.open(BytesIO(response.content)) as image:
        assert image.format == "WEBP"
        assert image.size == (1200, 800)


def test_validate_returns_photo_taken_at_header_when_exif_has_it(monkeypatch) -> None:
    monkeypatch.setattr("validator.app.get_classifier", lambda: FakeApprovingClassifier())
    client = TestClient(app)

    base = Image.new("RGB", (1200, 800), "#d97706")
    exif = base.getexif()
    exif[36867] = "2026:08:15 14:32:07"  # DateTimeOriginal
    buffer = BytesIO()
    base.save(buffer, format="JPEG", exif=exif)
    buffer.seek(0)

    response = client.post(
        "/validate",
        files={"file": ("dish.jpg", buffer, "image/jpeg")},
    )

    assert response.status_code == 200
    assert response.headers["x-photo-taken-at"] == "2026-08-15T14:32:07"


def test_validate_rejects_image_that_is_not_food(monkeypatch) -> None:
    monkeypatch.setattr("validator.app.get_classifier", lambda: FakeRejectingClassifier())
    client = TestClient(app)

    response = client.post(
        "/validate",
        files={"file": ("dish.jpg", _jpeg_bytes(), "image/jpeg")},
    )

    assert response.status_code == 422
    detail = response.json()["detail"]
    assert detail["code"] == "IMAGE_NOT_FOOD"
    assert detail["food_score"] == 0.12


def test_validate_rejects_non_image_payload(monkeypatch) -> None:
    monkeypatch.setattr("validator.app.get_classifier", lambda: FakeApprovingClassifier())
    client = TestClient(app)

    response = client.post(
        "/validate",
        files={"file": ("payload.bin", BytesIO(b"not an image"), "application/octet-stream")},
    )

    assert response.status_code == 422
    assert response.json()["detail"]["code"] == "INVALID_IMAGE"
