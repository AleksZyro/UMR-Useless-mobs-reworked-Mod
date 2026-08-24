# Corrupted Silverfish v4 Visual Blockout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Einen referenznahen, texturierten Blockbench-Blockout mit vier überprüfbaren Ansichten erzeugen.

**Architecture:** Ein kleiner deterministischer Python-Generator beschreibt Cubes, Bones, Materialklassen und Kameras. Er schreibt eine separate `.bbmodel`, eine einfache 256×256-Textur und vier Renderansichten, ohne v3 oder Produktionsressourcen zu verändern.

**Tech Stack:** Python 3, Pillow, Blockbench `.bbmodel` JSON, vorhandene v3-Rendergrundlagen

---

### Task 1: Referenz und Proportionsvertrag

**Files:**
- Create: `tools/corrupted_silverfish_v4/blockout.py`
- Create: `tools/corrupted_silverfish_v4/tests/test_blockout.py`

- [ ] Referenzbild repo-relativ laden und die vier grünen Bildfelder anhand ihrer Quadranten erfassen.
- [ ] Tests für kompakte Zielproportionen schreiben: Körperlänge/Breite höchstens 2.25, Kopfbreite mindestens 85 % der Hauptpanzerbreite, Schwanz höchstens 28 % der Gesamtlänge, exakt sechs Beine und drei Hauptpanzer.
- [ ] Tests ausführen und den erwarteten RED-Zustand bestätigen.

### Task 2: Blockout-Geometrie und Materialfarben

**Files:**
- Modify: `tools/corrupted_silverfish_v4/blockout.py`
- Generate: `Modelle/Editierbar/Corrupted Silverfish v4 Blockout.bbmodel`
- Generate: `Modelle/Exports/corrupted_silverfish_v4/blockout/corrupted_silverfish_v4_blockout.png`

- [ ] Kopf, Körperkern, drei Panzer, sechs zweiteilige Beine, zwei Mandibeln, kurzen Schwanz und asymmetrische Kristallcluster als getrennte benannte Cubes/Bones erzeugen.
- [ ] Eine einfache 256×256-Textur mit Silber, dunklem Violett, Magenta und Blau erzeugen und in die `.bbmodel` einbetten.
- [ ] Struktur- und Proportionstests grün ausführen.

### Task 3: Vieransichten-Zwischenstand

**Files:**
- Modify: `tools/corrupted_silverfish_v4/blockout.py`
- Generate: `Modelle/Exports/corrupted_silverfish_v4/review/blockout_front.png`
- Generate: `Modelle/Exports/corrupted_silverfish_v4/review/blockout_right.png`
- Generate: `Modelle/Exports/corrupted_silverfish_v4/review/blockout_top.png`
- Generate: `Modelle/Exports/corrupted_silverfish_v4/review/blockout_perspective.png`
- Generate: `Modelle/Exports/corrupted_silverfish_v4/review/blockout_contact_sheet.png`

- [ ] Vier Ansichten mit identischem Weltmassstab und transparentem Hintergrund rendern.
- [ ] Kontaktblatt mit Referenz erzeugen.
- [ ] Bilder visuell öffnen und bei falscher Silhouette ausschliesslich Geometrie/Materialblockout korrigieren.
- [ ] Dem Nutzer den Zwischenstand zeigen und vor Animation, Glow oder Integration stoppen.
