from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path

from .food_classifier import FoodClassifier


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path("validator/data"))
    args = parser.parse_args()

    classifier = FoodClassifier()
    totals = Counter()
    correct = 0
    scores: list[tuple[bool, float]] = []

    for expected, directory in (("FOOD", args.data / "food"), ("NOT_FOOD", args.data / "not_food")):
        image_paths = sorted(
            path for path in directory.iterdir()
            if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
        )
        for start in range(0, len(image_paths), 16):
            batch_paths = image_paths[start:start + 16]
            results = classifier.classify_batch(batch_paths)
            for result in results:
                predicted_is_food = result.decision == "FOOD"
                expected_is_food = expected == "FOOD"
                correct += predicted_is_food == expected_is_food
                totals[(expected, result.decision)] += 1
                scores.append((expected_is_food, result.food_score))

    total = sum(totals.values())
    print(f"images: {total}")
    print(f"accuracy: {correct / total:.3f}")
    for key, value in sorted(totals.items()):
        print(f"{key[0]} -> {key[1]}: {value}")
    for threshold in (0.80, 0.85, 0.90, 0.95):
        food_total = sum(expected for expected, _ in scores)
        food_approved = sum(expected and score >= threshold for expected, score in scores)
        non_food_approved = sum((not expected) and score >= threshold for expected, score in scores)
        print(
            f"threshold={threshold:.2f} "
            f"food_recall={food_approved / food_total:.3f} "
            f"non_food_false_positive={non_food_approved / (len(scores) - food_total):.3f}"
        )


if __name__ == "__main__":
    main()
