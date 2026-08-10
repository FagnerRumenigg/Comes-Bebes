from __future__ import annotations

import shutil
import tempfile
from functools import lru_cache
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile

from .image_validator import MAX_INPUT_BYTES, ImageValidationError, validate_and_normalize


app = FastAPI(title="ComeSebebes Image Validator", docs_url=None, redoc_url=None)
BASE_DIRECTORY = Path(__file__).resolve().parent
NORMALIZED_DIRECTORY = BASE_DIRECTORY / "normalized"
DEFAULT_FOOD_THRESHOLD = 0.5


@lru_cache(maxsize=1)
def get_classifier():
    from .csat_classifier import CsatFoodClassifier

    return CsatFoodClassifier()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/validate")
def validate(file: UploadFile = File(...)) -> dict[str, object]:
    with tempfile.TemporaryDirectory(prefix="upload-", dir=BASE_DIRECTORY) as temporary_directory:
        input_path = Path(temporary_directory) / "input"
        with input_path.open("wb") as destination:
            total_bytes = 0
            while chunk := file.file.read(1024 * 1024):
                total_bytes += len(chunk)
                if total_bytes > MAX_INPUT_BYTES:
                    raise HTTPException(status_code=413, detail={"code": "FILE_TOO_LARGE"})
                destination.write(chunk)

        try:
            result = validate_and_normalize(input_path, NORMALIZED_DIRECTORY)
        except ImageValidationError as error:
            raise HTTPException(status_code=422, detail={"code": error.code, "message": error.message}) from error
        finally:
            file.file.close()

    try:
        classification = get_classifier().classify(Path(result.output_path), threshold=DEFAULT_FOOD_THRESHOLD)
    except (ImportError, OSError, RuntimeError) as error:
        Path(result.output_path).unlink(missing_ok=True)
        raise HTTPException(
            status_code=503,
            detail={"code": "CLASSIFIER_UNAVAILABLE", "message": "O classificador não está disponível."},
        ) from error

    return {
        "status": "APPROVED" if classification.decision == "FOOD" else "REJECTED",
        "technical_status": result.status,
        "source_format": result.source_format,
        "format": result.format,
        "width": result.width,
        "height": result.height,
        "size_bytes": result.size_bytes,
        "sha256": result.sha256,
        "output_path": result.output_path,
        "classification": {
            "decision": classification.decision,
            "food_score": classification.food_score,
            "threshold": DEFAULT_FOOD_THRESHOLD,
        },
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("validator.app:app", host="127.0.0.1", port=8001)
