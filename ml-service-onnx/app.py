import base64
import io
import os

import cv2
import numpy as np
import onnxruntime as ort
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image

IMG_SIZE = (224, 224)

app = FastAPI(title="Pneumonia ML Service (ONNX)")
app.add_middleware(CORSMiddleware, allow_origins=["*"],
                   allow_methods=["*"], allow_headers=["*"])

opts = ort.SessionOptions()
opts.intra_op_num_threads = 1          # économise la RAM/CPU sur l'hébergement gratuit
session = ort.InferenceSession("pneumonia.onnx", opts,
                               providers=["CPUExecutionProvider"])
INPUT_NAME = session.get_inputs()[0].name
# Poids de la couche Dense finale (1280 valeurs) : servent à calculer le Grad-CAM.
# Pour une tête GlobalAveragePooling -> Dense(sigmoid), Grad-CAM == relu(sum_c w_c * F_c).
DENSE_W = np.load("dense_weights.npy")


def overlay(pil_img: Image.Image, cam: np.ndarray) -> str:
    img = np.array(pil_img.resize(IMG_SIZE).convert("RGB"))
    heat = cv2.applyColorMap(np.uint8(255 * cv2.resize(cam, IMG_SIZE)),
                             cv2.COLORMAP_JET)
    heat = cv2.cvtColor(heat, cv2.COLOR_BGR2RGB)
    mixed = cv2.addWeighted(img, 0.6, heat, 0.4, 0)
    _, buf = cv2.imencode(".png", cv2.cvtColor(mixed, cv2.COLOR_RGB2BGR))
    return base64.b64encode(buf).decode()


@app.get("/")
def health():
    return {"status": "ok"}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    try:
        img = Image.open(io.BytesIO(await file.read())).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="Image invalide")

    x = np.expand_dims(np.array(img.resize(IMG_SIZE), dtype="float32") / 255.0, 0)
    prob, feats = session.run(None, {INPUT_NAME: x})
    p = float(prob[0, 0])

    cam = np.maximum((feats[0] * DENSE_W).sum(-1), 0)
    cam = cam / (cam.max() + 1e-8)

    return {
        "label": "PNEUMONIA" if p >= 0.5 else "NORMAL",
        "probability_pneumonia": round(p, 4),
        "probability_normal": round(1 - p, 4),
        "heatmap_base64": overlay(img, cam),
    }
