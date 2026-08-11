from pathlib import Path
from typing import Dict, Tuple

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


# EXIF orientation transform expected by each of the 8 possible Orientation tag
# values, matching Pillow's own ImageOps.exif_transpose implementation.
ORIENTATION_TRANSFORMS = {
    1: None,
    2: Image.Transpose.FLIP_LEFT_RIGHT,
    3: Image.Transpose.ROTATE_180,
    4: Image.Transpose.FLIP_TOP_BOTTOM,
    5: Image.Transpose.TRANSPOSE,
    6: Image.Transpose.ROTATE_270,
    7: Image.Transpose.TRANSVERSE,
    8: Image.Transpose.ROTATE_90,
}

EXIF_ORIENTATION_TAG = 0x0112

# A|B
# ---
# C|D
QUADRANT_COLORS = {
    "A": (255, 0, 0),
    "B": (0, 255, 0),
    "C": (0, 0, 255),
    "D": (255, 255, 0),
}


def _quadrant_image(width: int = 40, height: int = 20) -> Image.Image:
    image = Image.new("RGB", (width, height))
    pixels = image.load()
    half_w, half_h = width // 2, height // 2
    for x in range(width):
        for y in range(height):
            if x < half_w and y < half_h:
                pixels[x, y] = QUADRANT_COLORS["A"]
            elif x >= half_w and y < half_h:
                pixels[x, y] = QUADRANT_COLORS["B"]
            elif x < half_w and y >= half_h:
                pixels[x, y] = QUADRANT_COLORS["C"]
            else:
                pixels[x, y] = QUADRANT_COLORS["D"]
    return image


def _quadrant_samples(image: Image.Image) -> Dict[str, Tuple[int, int, int]]:
    width, height = image.size
    quarter_w, quarter_h = width // 4, height // 4
    return {
        "A": image.getpixel((quarter_w, quarter_h)),
        "B": image.getpixel((width - quarter_w, quarter_h)),
        "C": image.getpixel((quarter_w, height - quarter_h)),
        "D": image.getpixel((width - quarter_w, height - quarter_h)),
    }


def _assert_close(actual: Tuple[int, ...], expected: Tuple[int, ...], tolerance: int = 24) -> None:
    assert len(actual) >= 3 and len(expected) >= 3
    for channel in range(3):
        assert abs(actual[channel] - expected[channel]) <= tolerance, (actual, expected)


@pytest.mark.parametrize("orientation", sorted(ORIENTATION_TRANSFORMS))
def test_exif_orientation_is_corrected_and_baked_into_pixels(tmp_path: Path, orientation: int) -> None:
    base = _quadrant_image()
    exif = base.getexif()
    exif[EXIF_ORIENTATION_TAG] = orientation
    input_path = tmp_path / f"orientation-{orientation}.jpg"
    base.save(input_path, format="JPEG", quality=95, exif=exif)

    result = validate_and_normalize(input_path, tmp_path / "normalized")

    transform = ORIENTATION_TRANSFORMS[orientation]
    expected = base if transform is None else base.transpose(transform)

    with Image.open(result.output_path) as output_image:
        assert output_image.size == expected.size
        assert (result.width, result.height) == expected.size

        expected_samples = _quadrant_samples(expected)
        actual_samples = _quadrant_samples(output_image.convert("RGB"))
        for quadrant, expected_color in expected_samples.items():
            _assert_close(actual_samples[quadrant], expected_color)

        # The orientation is now physically applied to the pixels; no client
        # should need to interpret EXIF Orientation again.
        assert output_image.getexif().get(EXIF_ORIENTATION_TAG) is None


def test_image_without_exif_orientation_is_left_visually_unchanged(tmp_path: Path) -> None:
    base = _quadrant_image()
    input_path = tmp_path / "no-orientation.jpg"
    base.save(input_path, format="JPEG", quality=95)

    result = validate_and_normalize(input_path, tmp_path / "normalized")

    with Image.open(result.output_path) as output_image:
        assert output_image.size == base.size
        expected_samples = _quadrant_samples(base)
        actual_samples = _quadrant_samples(output_image.convert("RGB"))
        for quadrant, expected_color in expected_samples.items():
            _assert_close(actual_samples[quadrant], expected_color)


def test_normalizes_png_input(tmp_path: Path) -> None:
    input_path = tmp_path / "dish.png"
    _quadrant_image().save(input_path, format="PNG")

    result = validate_and_normalize(input_path, tmp_path / "normalized")

    assert result.source_format == "PNG"
    assert result.format == "WEBP"


def test_normalizes_webp_input(tmp_path: Path) -> None:
    input_path = tmp_path / "dish.webp"
    _quadrant_image().save(input_path, format="WEBP")

    result = validate_and_normalize(input_path, tmp_path / "normalized")

    assert result.source_format == "WEBP"
    assert result.format == "WEBP"


def test_normalizes_mpo_jpeg_from_phones(tmp_path: Path) -> None:
    # Portrait/depth-mode and dual-camera photos are saved by many phones as
    # MPO (Multi Picture Object) containers with a .jpg extension. Pillow
    # reports format == "MPO" for these, not "JPEG" — this must still be
    # accepted as a regular JPEG upload.
    input_path = tmp_path / "dish.jpg"
    first_frame = _quadrant_image()
    second_frame = _quadrant_image()
    first_frame.save(input_path, format="MPO", save_all=True, append_images=[second_frame])

    result = validate_and_normalize(input_path, tmp_path / "normalized")

    assert result.source_format == "JPEG"
    assert result.format == "WEBP"


def test_normalizes_heic_input(tmp_path: Path) -> None:
    input_path = tmp_path / "dish.heic"
    _quadrant_image().save(input_path, format="HEIF")

    result = validate_and_normalize(input_path, tmp_path / "normalized")

    assert result.source_format == "HEIF"
    assert result.format == "WEBP"
    with Image.open(result.output_path) as output_image:
        assert output_image.format == "WEBP"


def test_rejects_corrupted_image(tmp_path: Path) -> None:
    input_path = tmp_path / "dish.jpg"
    Image.new("RGB", (100, 100), "white").save(input_path, format="JPEG")
    with input_path.open("r+b") as file:
        file.seek(20)
        file.write(b"\x00" * 40)

    with pytest.raises(ImageValidationError) as error:
        validate_and_normalize(input_path, tmp_path / "normalized")

    assert error.value.code == "INVALID_IMAGE"
