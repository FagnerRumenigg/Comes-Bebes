from io import BytesIO

from fastapi.testclient import TestClient
from PIL import Image

from validator.app import app
from validator.csat_classifier import CsatClassificationResult


class FakeClassifier:
    def classify(self, image_path, threshold: float) -> CsatClassificationResult:
        return CsatClassificationResult(food_score=0.91, decision="FOOD")


def test_validate_returns_technical_and_classification_results(monkeypatch) -> None:
    monkeypatch.setattr("validator.app.get_classifier", lambda: FakeClassifier())
    client = TestClient(app)
    image_buffer = BytesIO()
    Image.new("RGB", (1200, 800), "#d97706").save(image_buffer, format="JPEG")
    image_buffer.seek(0)

    response = client.post(
        "/validate",
        files={"file": ("dish.jpg", image_buffer, "image/jpeg")},
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "APPROVED"
    assert payload["technical_status"] == "TECHNICALLY_VALID"
    assert payload["classification"] == {
        "decision": "FOOD",
        "food_score": 0.91,
        "threshold": 0.5,
    }
