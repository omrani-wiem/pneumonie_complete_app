import base64
import io
import os

import cv2
import numpy as np
import onnxruntime as ort
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image

from typing_logic import interpret

IMG_SIZE = (224, 224)
MAX_BYTES = 10 * 1024 * 1024
Image.MAX_IMAGE_PIXELS = 40_000_000          # refuse les "decompression bombs"
TYPE_MIN_CONF = float(os.getenv("TYPE_MIN_CONF", "0.75"))
TYPE_MODEL_PATH = os.getenv("TYPE_MODEL_PATH", "type.onnx")

app = FastAPI(title="Pneumonia ML Service (ONNX)")

# CORS : ce service est appelé par le backend Java (serveur à serveur), aucun navigateur
# n'a besoin d'y accéder. Fermé par défaut ; ALLOWED_ORIGINS="https://a.com,https://b.com"
# pour ouvrir explicitement des origines précises.
_origins = [o.strip() for o in os.getenv("ALLOWED_ORIGINS", "").split(",") if o.strip()]
if _origins:
    app.add_middleware(CORSMiddleware, allow_origins=_origins,
                       allow_methods=["GET", "POST"], allow_headers=["Content-Type"])

opts = ort.SessionOptions()
opts.intra_op_num_threads = 1          # économise la RAM/CPU sur l'hébergement gratuit

# 1) Modèle de détection (existant) : pneumonie ou non + cartes de caractéristiques pour le Grad-CAM
session = ort.InferenceSession("pneumonia.onnx", opts, providers=["CPUExecutionProvider"])
INPUT_NAME = session.get_inputs()[0].name
# Poids de la couche Dense finale (1280 valeurs) : servent à calculer le Grad-CAM.
# Pour une tête GlobalAveragePooling -> Dense(sigmoid), Grad-CAM == relu(sum_c w_c * F_c).
DENSE_W = np.load("dense_weights.npy").reshape(-1)

# 2) Modèle de type (bactérienne / virale), OPTIONNEL.
# S'il est absent, le service fonctionne comme avant et le type reste "non évalué" (null).
type_session = None
TYPE_INPUT_NAME = None
if os.path.exists(TYPE_MODEL_PATH):
    type_session = ort.InferenceSession(TYPE_MODEL_PATH, opts, providers=["CPUExecutionProvider"])
    TYPE_INPUT_NAME = type_session.get_inputs()[0].name


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
    return {"status": "ok", "typing": type_session is not None}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    data = await file.read(MAX_BYTES + 1)
    if len(data) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="Fichier trop volumineux")
    try:
        img = Image.open(io.BytesIO(data))
        if img.format not in ("PNG", "JPEG"):
            raise ValueError("format")
        img = img.convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="Image invalide")

    x = np.expand_dims(np.array(img.resize(IMG_SIZE), dtype="float32") / 255.0, 0)

    prob, feats = session.run(None, {INPUT_NAME: x})
    p_pneu = float(prob[0, 0])

    # Le type n'est évalué que s'il y a pneumonie ET si le modèle de type est déployé.
    p_viral = None
    if type_session is not None and p_pneu >= 0.5:
        p_viral = float(type_session.run(None, {TYPE_INPUT_NAME: x})[0].reshape(-1)[0])
    r = interpret(p_pneu, p_viral, TYPE_MIN_CONF)

    cam = np.maximum((feats[0] * DENSE_W).sum(-1), 0)
    cam = cam / (cam.max() + 1e-8)

    return {
        "label": r["label"],
        "probability_pneumonia": round(r["probability_pneumonia"], 4),
        "probability_normal": round(1 - r["probability_pneumonia"], 4),
        "pneumonia_type": r["pneumonia_type"],
        "type_confidence": None if r["type_confidence"] is None else round(r["type_confidence"], 4),
        "heatmap_base64": overlay(img, cam),
    }