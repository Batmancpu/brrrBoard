"""Execute the two pinned ONNX graphs on CPU, not an accuracy benchmark."""
from pathlib import Path
import numpy as np
import onnxruntime as ort

root = Path(__file__).resolve().parents[1] / "app/src/main/assets/voice"
opts = ort.SessionOptions()
opts.intra_op_num_threads = 1
opts.inter_op_num_threads = 1
vad = ort.InferenceSession(str(root / "silero_vad_v6_2_3.onnx"), opts, providers=["CPUExecutionProvider"])
state = np.zeros((2, 1, 128), dtype=np.float32)
sr_input = next(x for x in vad.get_inputs() if x.name == "sr")
sr = np.array(16000, dtype=np.int64) if not sr_input.shape else np.array([16000], dtype=np.int64)
for _ in range(10):
    probability, state = vad.run(None, {"input": np.zeros((1, 576), dtype=np.float32), "state": state, "sr": sr})
    assert np.isfinite(probability).all() and 0 <= probability.item() <= 1
    assert state.shape == (2, 1, 128) and np.isfinite(state).all()
assert probability.item() < 0.5, probability
turn = ort.InferenceSession(str(root / "smart-turn-v3.2-cpu.onnx"), opts, providers=["CPUExecutionProvider"])
out = turn.run(None, {"input_features": np.full((1, 80, 800), -1.5, dtype=np.float32)})[0]
assert out.size == 1 and np.isfinite(out).all() and 0 <= out.item() <= 1
print("Pinned Silero and Smart Turn CPU graph execution passed.")
