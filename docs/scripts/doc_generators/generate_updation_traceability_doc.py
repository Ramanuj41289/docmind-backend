from __future__ import annotations

from datetime import datetime
from pathlib import Path

from docx import Document
from docx.shared import Pt


ROOT = Path(__file__).resolve().parent
DOCS_ROOT = ROOT.parents[1]
OUTPUT_DOCX = DOCS_ROOT / "docx" / "fullstack" / "DocMind_Updation_Traceability_v1_1.docx"
OCR_SOURCE = DOCS_ROOT.parent / "Updation" / "Updation_OCR_Extracted_Text.txt"


def add_heading(doc: Document, text: str, level: int = 1) -> None:
    doc.add_heading(text, level=level)


def add_bullets(doc: Document, items: list[str]) -> None:
    for item in items:
        p = doc.add_paragraph(item, style="List Bullet")
        p.paragraph_format.space_after = Pt(4)


def add_numbers(doc: Document, items: list[str]) -> None:
    for item in items:
        p = doc.add_paragraph(item, style="List Number")
        p.paragraph_format.space_after = Pt(4)


def build_doc() -> None:
    doc = Document()
    style = doc.styles["Normal"]
    style.font.name = "Calibri"
    style.font.size = Pt(11)

    title = doc.add_paragraph()
    run = title.add_run("DocMind Updation Traceability and Structure Review")
    run.bold = True
    run.font.size = Pt(20)

    doc.add_paragraph(f"Generated: {datetime.now():%Y-%m-%d %H:%M}")

    add_heading(doc, "1. Inputs Reviewed", 1)
    add_bullets(
        doc,
        [
            "Updation screenshots OCR extract: Updation_OCR_Extracted_Text.txt",
            "Frontend architecture deliverables in docs/docx/frontend and docs/docx/frontend/images",
            "Backend architecture deliverables in docs/docx/backend and docs/docx/backend/images",
            "Full stack architecture deliverables in docs/docx/fullstack and docs/docx/fullstack/images",
            "Documentation scripts under docs/scripts/doc_generators",
        ],
    )

    add_heading(doc, "2. Normalized Requirements from Updation", 1)
    requirements = [
        "REQ-001: Define harness and context engineering model for agent workflows.",
        "REQ-002: Define tool design and controlled tool execution patterns.",
        "REQ-003: Define iterative loop and evaluation pattern for continuous improvement.",
        "REQ-004: Define short-term and long-term memory architecture for agents.",
        "REQ-005: Define guardrails and permissions model.",
        "REQ-006: Define orchestration pattern for multi-agent execution.",
        "REQ-007: Define observability and tracing model.",
        "REQ-008: Define human-in-the-loop governance model.",
    ]
    add_numbers(doc, requirements)

    add_heading(doc, "3. Requirement to Evidence Mapping", 1)
    table = doc.add_table(rows=1, cols=4)
    table.style = "Table Grid"
    hdr = table.rows[0].cells
    hdr[0].text = "Requirement"
    hdr[1].text = "Status"
    hdr[2].text = "Evidence"
    hdr[3].text = "Notes"

    mappings = [
        (
            "REQ-001",
            "Implemented",
            "docs/docx/fullstack/DocMind_Architecture_Fullstack_v1_1.docx and docs/docx/fullstack/images/Fullstack_Layered_Architecture_v1_1.png",
            "Architecture layers and context orchestration documented.",
        ),
        (
            "REQ-002",
            "Implemented",
            "docs/docx/backend/DocMind_Architecture_Backend_v1_1.docx and docs/docx/backend/images/Backend_End_to_End_Request_Flows_v1_1.png",
            "Backend flow includes tool/API execution boundaries.",
        ),
        (
            "REQ-003",
            "Implemented",
            "docs/docx/backend/images/Backend_Eval_Pipeline_v1_1.png and docs/docx/backend/DocMind_Architecture_Backend_v1_1.docx",
            "Eval pipeline and service flow are now explicitly documented.",
        ),
        (
            "REQ-004",
            "Implemented",
            "docs/docx/backend/images/Backend_Data_and_Memory_Architecture_v1_1.jpg and docs/CHAT_MEMORY_DB_ARCHITECTURE.md",
            "Session and memory architecture documented with AI session tables.",
        ),
        (
            "REQ-005",
            "Implemented",
            "src/main/java/com/ramanuj/docmind/config/SecurityConfig.java and docs/docx/backend/DocMind_Architecture_Backend_v1_1.docx",
            "JWT, RBAC, and request scoping documented and implemented.",
        ),
        (
            "REQ-006",
            "Implemented",
            "docs/docx/fullstack/images/Fullstack_Multi_Agent_Orchestration_Gap_v1_1.png and docs/docx/fullstack/DocMind_Architecture_Fullstack_v1_1.docx",
            "Target multi-agent orchestration and current-state gap are documented with phased transition plan.",
        ),
        (
            "REQ-007",
            "Implemented",
            "docs/docx/backend/images/Backend_Observability_Tracing_v1_1.png and docs/docx/backend/DocMind_Architecture_Backend_v1_1.docx",
            "Tracing stack and deployment evidence are documented with current vs target observability model.",
        ),
        (
            "REQ-008",
            "Implemented",
            "docs/docx/backend/images/Backend_HITL_Checkpoints_v1_1.png and docs/docx/backend/DocMind_Architecture_Backend_v1_1.docx",
            "Human approval checkpoints and high-risk execution branches are now documented.",
        ),
    ]

    for req, status, evidence, notes in mappings:
        row = table.add_row().cells
        row[0].text = req
        row[1].text = status
        row[2].text = evidence
        row[3].text = notes

    add_heading(doc, "4. Action Backlog for Partial Items", 1)
    add_numbers(
        doc,
        [
            "BL-001 (REQ-003): Completed in v1_1 backend docs and diagrams.",
            "BL-002 (REQ-006): Completed in v1_1 full stack docs and orchestration gap diagram.",
            "BL-003 (REQ-007): Completed in v1_1 backend observability and tracing documentation.",
            "BL-004 (REQ-008): Completed in v1_1 backend HITL checkpoint documentation.",
        ],
    )

    add_heading(doc, "5. Folder Structure Verification", 1)
    add_bullets(
        doc,
        [
            "docs/docx/frontend contains one versioned docx and a dedicated images folder.",
            "docs/docx/backend contains one versioned docx and a dedicated images folder.",
            "docs/docx/fullstack contains one versioned docx and a dedicated images folder.",
            "docs/docx/reference contains source reference word documents.",
            "docs/scripts contains only doc_generators (no redundant sibling script folders).",
        ],
    )

    add_heading(doc, "6. Step-by-Step What Is Needed", 1)
    add_numbers(
        doc,
        [
            "Maintain v1_1 artifacts as current baseline and keep evidence links up to date.",
            "Keep all generated files in docs/docx/<domain>/ and images in docs/docx/<domain>/images/.",
            "Keep generation code only in docs/scripts/doc_generators/.",
            "Update docs/docx/README.md after every version bump.",
            "Run a quarterly traceability review against Updation requirements.",
        ],
    )

    add_heading(doc, "7. Compatibility", 1)
    doc.add_paragraph("This document is .docx (Office Open XML) and is readable/writable in Microsoft Word 2013 and later.")

    OUTPUT_DOCX.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(OUTPUT_DOCX))


if __name__ == "__main__":
    build_doc()
    print(OUTPUT_DOCX)
    print(OCR_SOURCE)
