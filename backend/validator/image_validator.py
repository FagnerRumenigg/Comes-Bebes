from __future__ import annotations

import hashlib
import os
import tempfile
import warnings
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from PIL import Image, ImageOps, UnidentifiedImageError

import pillow_heif

pillow_heif.register_heif_opener()


MAX_INPUT_BYTES = 20 * 1024 * 1024
MAX_WIDTH = 3840
MAX_HEIGHT = 2160
MAX_PIXELS = MAX_WIDTH * MAX_HEIGHT
WEBP_QUALITY = 85

# MPO ("Multi Picture Object") is the container format Pillow reports for the
# multi-frame JPEGs many phones save (portrait/depth mode, Live Photos, dual
# camera). It is a JPEG for every practical purpose here; without this alias
# those uploads are wrongly rejected as an unsupported format.
ALLOWED_FORMATS = {"JPEG", "MPO", "PNG", "WEBP", "HEIC", "HEIF"}
SOURCE_FORMAT_ALIASES = {"MPO": "JPEG"}

# DateTimeOriginal, DateTimeDigitized, DateTime — nessa ordem de preferência.
_EXIF_DATETIME_TAGS = (36867, 36868, 306)
_EXIF_DATETIME_FORMAT = "%Y:%m:%d %H:%M:%S"


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
    photo_taken_at: str | None = None


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
                detected_format = source_image.format or "UNKNOWN"
                if detected_format not in ALLOWED_FORMATS:
                    raise ImageValidationError("UNSUPPORTED_FORMAT", "Formato de imagem não permitido.")
                source_image.verify()
            source_format = SOURCE_FORMAT_ALIASES.get(detected_format, detected_format)

            with Image.open(input_path) as image:
                photo_taken_at = _extract_photo_taken_at(image)
                normalized_image = ImageOps.exif_transpose(image)
                normalized_image = normalized_image.copy()
                width, height = normalized_image.size
                if width <= 0 or height <= 0:
                    raise ImageValidationError("INVALID_IMAGE", "O arquivo não é uma imagem válida.")
                if width > MAX_WIDTH or height > MAX_HEIGHT or width * height > MAX_PIXELS:
                    normalized_image = ImageOps.contain(normalized_image, (MAX_WIDTH, MAX_HEIGHT))
                    width, height = normalized_image.size

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
        photo_taken_at=photo_taken_at,
    )


def _extract_photo_taken_at(image: Image.Image) -> str | None:
    """Lê a data/hora em que a foto foi tirada a partir do EXIF, se existir.

    A maioria dos prints de tela, imagens reencaminhadas pelo WhatsApp e PNGs
    não carrega essa informação — nesses casos retorna None e o chamador trata
    como campo opcional. O valor devolvido é ingênuo (sem fuso), porque o EXIF
    não garante um offset confiável; quem consome decide como interpretar.
    """
    try:
        exif = image.getexif()
    except Exception:
        return None
    if not exif:
        return None

    for tag in _EXIF_DATETIME_TAGS:
        raw_value = exif.get(tag)
        if not raw_value:
            continue
        try:
            parsed = datetime.strptime(str(raw_value).strip(), _EXIF_DATETIME_FORMAT)
        except ValueError:
            continue
        return parsed.isoformat()

    return None


def _sha256(file_path: Path) -> str:
    digest = hashlib.sha256()
    with file_path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()
