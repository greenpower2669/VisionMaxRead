#!/usr/bin/env python3
"""VMR-021: score a verified manual transcription against OCR output, locally.

Example: python3 scripts/score_ocr.py reference.txt surya.txt
No image, transcription, names or OCR strings are sent to any network endpoint.
The JSON report contains numerical metrics only.
"""
import argparse
import json
import unicodedata
from pathlib import Path

def distance(reference, prediction):
    """Levenshtein with O(min(n,m)) working memory; substitution/insert/delete cost 1."""
    if len(reference) < len(prediction):
        reference, prediction = prediction, reference
    previous = list(range(len(prediction) + 1))
    for i, left in enumerate(reference, 1):
        current = [i]
        for j, right in enumerate(prediction, 1):
            current.append(min(previous[j]+1, current[j-1]+1,
                               previous[j-1]+(left != right)))
        previous = current
    return previous[-1]

def measure(reference: str, candidate: str):
    # NFC normalizes equivalent accented characters without deleting accents,
    # numbers, punctuation or silently correcting text.
    r = unicodedata.normalize("NFC", reference).strip()
    c = unicodedata.normalize("NFC", candidate).strip()
    rw, cw = r.split(), c.split()
    if not r:
        raise ValueError("Human reference transcription must not be empty")
    ce, we = distance(r, c), distance(rw, cw)
    return {"reference_characters": len(r), "candidate_characters": len(c),
            "character_edits": ce, "cer": round(ce / len(r), 6),
            "reference_words": len(rw), "candidate_words": len(cw),
            "word_edits": we, "wer": round(we / len(rw), 6)}

def main():
    parser = argparse.ArgumentParser(description="Local CER/WER OCR scoring")
    parser.add_argument("reference", type=Path)
    parser.add_argument("candidate", type=Path)
    a = parser.parse_args()
    print(json.dumps(measure(a.reference.read_text(encoding="utf-8"),
                             a.candidate.read_text(encoding="utf-8")),
                     ensure_ascii=False, indent=2))
if __name__ == "__main__":
    main()
