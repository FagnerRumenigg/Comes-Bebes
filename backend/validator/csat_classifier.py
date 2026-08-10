from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import timm
import torch
from PIL import Image
from timm.data import create_transform, resolve_data_config
from huggingface_hub import hf_hub_download
from safetensors.torch import load_file


MODEL_REPOSITORY = "mrdbourke/food-not-food-classifier-csatv2-v1"
MODEL_ARCHITECTURE = "csatv2.r512_in1k"


@dataclass(frozen=True)
class CsatClassificationResult:
    food_score: float
    decision: str


class CsatFoodClassifier:
    def __init__(self) -> None:
        self.model = timm.create_model(MODEL_ARCHITECTURE, pretrained=False, num_classes=2)
        weights_path = hf_hub_download(MODEL_REPOSITORY, "model.safetensors")
        self.model.load_state_dict(load_file(weights_path))
        self.model.eval()
        data_config = resolve_data_config(self.model.pretrained_cfg)
        data_config["input_size"] = (3, 512, 512)
        self.transform = create_transform(**data_config, is_training=False)

    def classify(self, image_path: Path, threshold: float = 0.5) -> CsatClassificationResult:
        return self.classify_batch([image_path], threshold)[0]

    def classify_batch(self, image_paths: list[Path], threshold: float = 0.5) -> list[CsatClassificationResult]:
        tensors = []
        for image_path in image_paths:
            with Image.open(image_path) as image:
                tensors.append(self.transform(image.convert("RGB")))
        batch = torch.stack(tensors)
        with torch.inference_mode():
            probabilities = torch.softmax(self.model(batch), dim=1)[:, 0]
        return [
            CsatClassificationResult(
                food_score=float(score),
                decision="FOOD" if float(score) >= threshold else "NOT_FOOD",
            )
            for score in probabilities
        ]
