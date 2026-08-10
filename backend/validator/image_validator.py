from __future__ import annotations

import hashlib
import os
import tempfile
import warnings
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageOps, UnidentifiedImageError


MAX_INPUT_BYTES = 15 * 1024 * 1024
MAX_WIDTH = 3840
MAX_HEIGHT = 2160
MAX_PIXELS = MAX_WIDTH * MAX_HEIGHT
WEBP_QUALITY = 85
ALLOWED_FORMATS = {"JPEG", "PNG", "WEBP"}


class ImageValidationError(ValueError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


@dataclass(frozen=True)
class ValidationResult:
    status: str
    source_format: str
    format: str
    width: int
    height: int
    size_bytes: int
    sha256: str
    output_path: str


def validate_and_normalize(input_path: Path, output_directory: Path) -> ValidationResult:
    input_path = input_path.resolve()
    output_directory = output_directory.resolve()

    if not input_path.is_file():
        raise ImageValidationError("FILE_NOT_FOUND", "Arquivo de entrada não encontrado.")

    input_size = input_path.stat().st_size
    if input_size == 0:
        raise ImageValidationError("EMPTY_FILE", "O arquivo está vazio.")
    if input_size > MAX_INPUT_BYTES:
        raise ImageValidationError("FILE_TOO_LARGE", "O arquivo excede o limite permitido.")

    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(input_path) as source_image:
                source_format = source_image.format or "UNKNOWN"
                if source_format not in ALLOWED_FORMATS:
                    raise ImageValidationError("UNSUPPORTED_FORMAT", "Formato de imagem não permitido.")
                source_image.verify()

            with Image.open(input_path) as image:
                normalized_image = ImageOps.exif_transpose(image)
                width, height = normalized_image.size
                if width <= 0 or height <= 0 or width > MAX_WIDTH or height > MAX_HEIGHT:
                    raise ImageValidationError("DIMENSIONS_TOO_LARGE", "As dimensões da imagem excedem o limite permitido.")
                if width * height > MAX_PIXELS:
                    raise ImageValidationError("PIXELS_TOO_MANY", "A quantidade de pixels excede o limite permitido.")

                normalized_image.load()
                rgb_image = normalized_image.convert("RGB")
                output_directory.mkdir(parents=True, exist_ok=True)
                with tempfile.NamedTemporaryFile(
                    dir=output_directory,
                    prefix="normalized-",
                    suffix=".webp",
                    delete=False,
                ) as temporary_output:
                    temporary_output_path = Path(temporary_output.name)

                try:
                    rgb_image.save(
                        temporary_output_path,
                        format="WEBP",
                        quality=WEBP_QUALITY,
                        method=6,
                    )
                    output_size = temporary_output_path.stat().st_size
                    digest = _sha256(temporary_output_path)
                except Exception:
                    temporary_output_path.unlink(missing_ok=True)
                    raise

    except ImageValidationError:
        raise
    except (UnidentifiedImageError, OSError, SyntaxError, Image.DecompressionBombError) as error:
        raise ImageValidationError("INVALID_IMAGE", "O arquivo não é uma imagem válida.") from error

    return ValidationResult(
        status="TECHNICALLY_VALID",
        source_format=source_format,
        format="WEBP",
        width=width,
        height=height,
        size_bytes=output_size,
        sha256=digest,
        output_path=os.fspath(temporary_output_path),
    )


def _sha256(file_path: Path) -> str:
    digest = hashlib.sha256()
    with file_path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()
