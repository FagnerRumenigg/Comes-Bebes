from pathlib import Path

import pytest
from PIL import Image

from validator.image_validator import ImageValidationError, validate_and_normalize


def test_normalizes_valid_jpeg_to_webp(tmp_path: Path) -> None:
    input_path = tmp_path / "dish.jpg"
    output_directory = tmp_path / "normalized"
    Image.new("RGB", (1200, 800), "#d97706").save(input_path, format="JPEG")

    result = validate_and_normalize(input_path, output_directory)

    assert result.status == "TECHNICALLY_VALID"
    assert result.source_format == "JPEG"
    assert result.format == "WEBP"
    assert (result.width, result.height) == (1200, 800)
    assert Path(result.output_path).suffix == ".webp"
    assert Path(result.output_path).stat().st_size == result.size_bytes


def test_rejects_non_image(tmp_path: Path) -> None:
    input_path = tmp_path / "payload.bin"
    input_path.write_bytes(b"not an image")

    with pytest.raises(ImageValidationError) as error:
        validate_and_normalize(input_path, tmp_path / "normalized")

    assert error.value.code == "INVALID_IMAGE"


def test_rejects_image_with_unsupported_format(tmp_path: Path) -> None:
    input_path = tmp_path / "dish.gif"
    Image.new("RGB", (100, 100), "white").save(input_path, format="GIF")

    with pytest.raises(ImageValidationError) as error:
        validate_and_normalize(input_path, tmp_path / "normalized")

    assert error.value.code == "UNSUPPORTED_FORMAT"


def test_rejects_file_over_size_limit(tmp_path: Path) -> None:
    input_path = tmp_path / "large.bin"
    with input_path.open("wb") as file:
        file.truncate(15 * 1024 * 1024 + 1)

    with pytest.raises(ImageValidationError) as error:
        validate_and_normalize(input_path, tmp_path / "normalized")

    assert error.value.code == "FILE_TOO_LARGE"
