# UMR-Arbeitsregeln

Diese Regeln gelten für jede Arbeit an diesem Repository.

## Verbindlicher Projekt-Wahrheitscheck

1. Vor Aussagen oder Änderungen an Modellen, Texturen, Animationen, Renderern oder der Laufzeitintegration ausführen:
   `python tools/verify_umr_project_truth.py`
2. Danach `docs/UMR_ACTIVE_PROJECT_STATE.md` vollständig lesen.
3. Die Dateien im aktuellen Worktree und die tatsächlich geladenen Laufzeitressourcen sind massgebend. Chat-Erinnerungen, Screenshots, alte Branches, andere Worktrees und Vorschauen sind keine Laufzeit-Wahrheit.
4. Wenn der Prüfer fehlschlägt, keine Modellfakten raten und nichts aus einem anderen Worktree übernehmen. Zuerst den aktuellen Worktree, Branch, Git-Status und die Ressourcenpfade klären.
5. Bestehende uncommittete Arbeit nicht löschen, zurücksetzen oder überschreiben.

Für UMR-Modellarbeit zusätzlich die lokale Skill-Anweisung unter
`.agents/skills/umr-project-truth/SKILL.md` verwenden.
