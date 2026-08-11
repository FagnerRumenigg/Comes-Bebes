from __future__ import annotations

import shutil
import tempfile
from functools import lru_cache
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, Response, UploadFile

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

    output_path = Path(result.output_path)
    try:
        try:
            classification = get_classifier().classify(output_path, threshold=DEFAULT_FOOD_THRESHOLD)
        except (ImportError, OSError, RuntimeError) as error:
            raise HTTPException(
                status_code=503,
                detail={"code": "CLASSIFIER_UNAVAILABLE", "message": "O classificador não está disponível."},
            ) from error

        if classification.decision != "FOOD":
            raise HTTPException(
                status_code=422,
                detail={
                    "code": "IMAGE_NOT_FOOD",
                    "message": "A imagem precisa apresentar uma comida.",
                    "food_score": classification.food_score,
                    "threshold": DEFAULT_FOOD_THRESHOLD,
                },
            )

        image_bytes = output_path.read_bytes()
        return Response(
            content=image_bytes,
            media_type="image/webp",
            headers={
                "X-Validation-Status": "APPROVED",
                "X-Technical-Status": result.status,
                "X-Source-Format": result.source_format,
                "X-Image-Format": result.format,
                "X-Image-Width": str(result.width),
                "X-Image-Height": str(result.height),
                "X-Size-Bytes": str(result.size_bytes),
                "X-Sha256": result.sha256,
                "X-Food-Score": str(classification.food_score),
                "X-Food-Threshold": str(DEFAULT_FOOD_THRESHOLD),
            },
        )
    finally:
        output_path.unlink(missing_ok=True)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("validator.app:app", host="127.0.0.1", port=8001)
