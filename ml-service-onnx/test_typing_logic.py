import pytest
from typing_logic import interpret


def test_no_type_model_gives_no_type():
    r = interpret(0.96)
    assert r["label"] == "PNEUMONIA" and r["pneumonia_type"] is None and r["type_confidence"] is None


def test_normal_never_gets_a_type():
    r = interpret(0.10, p_viral=0.99)
    assert r["label"] == "NORMAL" and r["pneumonia_type"] is None and r["type_confidence"] is None


def test_viral_when_confident():
    r = interpret(0.95, p_viral=0.90)
    assert r["pneumonia_type"] == "VIRAL" and r["type_confidence"] == pytest.approx(0.90)


def test_bacterial_when_confident():
    r = interpret(0.95, p_viral=0.10)
    assert r["pneumonia_type"] == "BACTERIAL" and r["type_confidence"] == pytest.approx(0.90)


def test_uncertain_type_is_undetermined_not_forced():
    r = interpret(0.95, p_viral=0.55)
    assert r["pneumonia_type"] == "UNDETERMINED" and r["type_confidence"] == pytest.approx(0.55)


def test_threshold_is_configurable():
    assert interpret(0.95, p_viral=0.55, type_min_conf=0.5)["pneumonia_type"] == "VIRAL"


def test_detection_threshold_boundary():
    assert interpret(0.5)["label"] == "PNEUMONIA"
    assert interpret(0.4999)["label"] == "NORMAL"