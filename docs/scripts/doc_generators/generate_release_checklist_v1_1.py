from __future__ import annotations

from datetime import datetime
from pathlib import Path

from docx import Document
from docx.shared import Pt


ROOT = Path(__file__).resolve().parent
DOCS_ROOT = ROOT.parents[1]
OUTPUT_DOCX = DOCS_ROOT / "docx" / "fullstack" / "DocMind_Release_Checklist_v1_1.docx"


def add_numbers(doc: Document, items: list[str]) -> None:
    for item in items:
        p = doc.add_paragraph(item, style="List Number")
        p.paragraph_format.space_after = Pt(4)


def add_bullets(doc: Document, items: list[str]) -> None:
    for item in items:
        p = doc.add_paragraph(item, style="List Bullet")
        p.paragraph_format.space_after = Pt(4)


def build_doc() -> None:
    doc = Document()

    style = doc.styles["Normal"]
    style.font.name = "Calibri"
    style.font.size = Pt(11)

    title = doc.add_paragraph()
    run = title.add_run("DocMind Documentation Release Checklist v1.1")
    run.bold = True
    run.font.size = Pt(20)

    doc.add_paragraph(f"Generated: {datetime.now():%Y-%m-%d %H:%M}")

    doc.add_heading("1. Scope", level=1)
    add_bullets(
        doc,
        [
            "Frontend architecture package in docs/docx/frontend.",
            "Backend architecture package in docs/docx/backend.",
            "Fullstack and traceability package in docs/docx/fullstack.",
            "Reference documents in docs/docx/reference.",
            "Generator scripts in docs/scripts/doc_generators.",
        ],
    )

    doc.add_heading("2. Pre-Release Checks", level=1)
    add_numbers(
        doc,
        [
            "Install generator dependencies using requirements.txt.",
            "Regenerate backend, frontend, fullstack, and traceability documents.",
            "Verify all v1_1 output files are produced in domain folders.",
            "Confirm image assets are present only in corresponding images folders.",
            "Confirm no new redundant folders are created under docs/docx or docs/scripts.",
        ],
    )

    doc.add_heading("3. Document Quality Checks", level=1)
    add_numbers(
        doc,
        [
            "Open each v1_1 docx in Microsoft Word 2013 or later and check formatting.",
            "Validate headings, table rendering, and image readability.",
            "Validate naming convention consistency for v1_1 artifacts.",
            "Confirm references and evidence paths are valid.",
            "Confirm BL-001 to BL-004 remain marked completed in traceability doc.",
        ],
    )

    doc.add_heading("4. Folder Structure Checks", level=1)
    add_bullets(
        doc,
        [
            "Current artifacts: docs/docx/backend, docs/docx/frontend, docs/docx/fullstack, docs/docx/reference.",
            "History artifacts: docs/docx/archive/v1_0.",
            "Script location: docs/scripts/doc_generators only.",
        ],
    )

    doc.add_heading("5. Release Actions", level=1)
    add_numbers(
        doc,
        [
            "Update docs/docx/README.md with current baseline version markers.",
            "Update docs/scripts/doc_generators/README.md if outputs or generators changed.",
            "Publish generated artifacts as release bundle for engineering leadership.",
            "Record release date and owner in project tracking system.",
        ],
    )

    doc.add_heading("6. Post-Release Cadence", level=1)
    add_bullets(
        doc,
        [
            "Run quarterly traceability review against Updation requirements.",
            "Use next version suffix (v1_2) only when content materially changes.",
            "Archive superseded versions under docs/docx/archive/<version>/.",
        ],
    )

    OUTPUT_DOCX.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(OUTPUT_DOCX))


if __name__ == "__main__":
    build_doc()
    print(OUTPUT_DOCX)
