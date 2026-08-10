from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import torch
from PIL import Image
from transformers import CLIPModel, CLIPProcessor


MODEL_NAME = "openai/clip-vit-base-patch32"
FOOD_PROMPT = "a photo of food or a prepared dish"
NOT_FOOD_PROMPT = "a photo that does not contain food"


@dataclass(frozen=True)
class ClassificationResult:
    decision: str
    food_score: float
    model: str


class FoodClassifier:
    def __init__(self, model_name: str = MODEL_NAME) -> None:
        self.model_name = model_name
        self.processor = CLIPProcessor.from_pretrained(model_name)
        self.model = CLIPModel.from_pretrained(model_name)
        self.model.eval()

    def classify(self, image_path: Path) -> ClassificationResult:
        return self.classify_batch([image_path])[0]

    def classify_batch(self, image_paths: list[Path]) -> list[ClassificationResult]:
        images = []
        for image_path in image_paths:
            with Image.open(image_path) as image:
                images.append(image.convert("RGB"))

        inputs = self.processor(
            text=[FOOD_PROMPT, NOT_FOOD_PROMPT],
            images=images,
            return_tensors="pt",
            padding=True,
        )

        with torch.inference_mode():
            logits = self.model(**inputs).logits_per_image
            probabilities = torch.softmax(logits, dim=1)

        return [
            ClassificationResult(
                decision=self._decision(float(probability[0])),
                food_score=float(probability[0]),
                model=self.model_name,
            )
            for probability in probabilities
        ]

    @staticmethod
    def _decision(food_score: float) -> str:
        if food_score >= 0.80:
            return "FOOD"
        if food_score <= 0.30:
            return "NOT_FOOD"
        return "UNCERTAIN"
