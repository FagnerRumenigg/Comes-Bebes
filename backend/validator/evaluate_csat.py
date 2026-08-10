from __future__ import annotations

import time
from collections import Counter
from pathlib import Path

from .csat_classifier import CsatFoodClassifier


def image_paths(directory: Path) -> list[Path]:
    return sorted(
        path for path in directory.iterdir()
        if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
    )


def main() -> None:
    data_directory = Path("validator/data")
    food_paths = image_paths(data_directory / "food")
    not_food_paths = image_paths(data_directory / "not_food")
    all_paths = [(path, True) for path in food_paths] + [(path, False) for path in not_food_paths]
    classifier = CsatFoodClassifier()

    sample_paths = [path for path, _ in all_paths[:16]]
    classifier.classify_batch(sample_paths)
    single_start = time.perf_counter()
    for path in sample_paths[:5]:
        classifier.classify(path)
    single_seconds = (time.perf_counter() - single_start) / 5

    batch_start = time.perf_counter()
    scores: list[tuple[bool, float]] = []
    for start in range(0, len(all_paths), 16):
        batch = all_paths[start:start + 16]
        results = classifier.classify_batch([path for path, _ in batch])
        scores.extend((expected, result.food_score) for (_, expected), result in zip(batch, results))
    batch_seconds = time.perf_counter() - batch_start

    print(f"images: {len(scores)}")
    print(f"single_image_seconds: {single_seconds:.3f}")
    print(f"batch_total_seconds: {batch_seconds:.1f}")
    print(f"batch_images_per_second: {len(scores) / batch_seconds:.2f}")
    for threshold in (0.50, 0.60, 0.70, 0.80):
        totals = Counter()
        for expected, score in scores:
            predicted = score >= threshold
            totals[(expected, predicted)] += 1
        food_total = sum(expected for expected, _ in scores)
        not_food_total = len(scores) - food_total
        food_recall = totals[(True, True)] / food_total
        false_positive = totals[(False, True)] / not_food_total
        accuracy = (totals[(True, True)] + totals[(False, False)]) / len(scores)
        print(
            f"threshold={threshold:.2f} accuracy={accuracy:.3f} "
            f"food_recall={food_recall:.3f} "
            f"non_food_false_positive={false_positive:.3f}"
        )


if __name__ == "__main__":
    main()
