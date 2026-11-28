from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DOCS_ROOT = ROOT.parents[1]
DOCX_ROOT = DOCS_ROOT / "docx"

REQUIRED_DIRS = [
    DOCX_ROOT,
    DOCX_ROOT / "frontend",
    DOCX_ROOT / "frontend" / "images",
    DOCX_ROOT / "backend",
    DOCX_ROOT / "backend" / "images",
    DOCX_ROOT / "fullstack",
    DOCX_ROOT / "fullstack" / "images",
    DOCX_ROOT / "reference",
    DOCX_ROOT / "archive" / "v1_0",
    DOCS_ROOT / "scripts" / "doc_generators",
]

REQUIRED_FILES = [
    DOCX_ROOT / "frontend" / "DocMind_Architecture_Frontend_v1_1.docx",
    DOCX_ROOT / "frontend" / "images" / "Frontend_System_Architecture_v1_1.png",
    DOCX_ROOT / "frontend" / "images" / "Frontend_Component_Layers_v1_1.png",
    DOCX_ROOT / "frontend" / "images" / "Frontend_User_Flows_v1_1.jpg",
    DOCX_ROOT / "backend" / "DocMind_Architecture_Backend_v1_1.docx",
    DOCX_ROOT / "backend" / "images" / "Backend_Component_Map_v1_1.png",
    DOCX_ROOT / "backend" / "images" / "Backend_End_to_End_Request_Flows_v1_1.png",
    DOCX_ROOT / "backend" / "images" / "Backend_Data_and_Memory_Architecture_v1_1.jpg",
    DOCX_ROOT / "backend" / "images" / "Backend_Eval_Pipeline_v1_1.png",
    DOCX_ROOT / "backend" / "images" / "Backend_Observability_Tracing_v1_1.png",
    DOCX_ROOT / "backend" / "images" / "Backend_HITL_Checkpoints_v1_1.png",
    DOCX_ROOT / "fullstack" / "DocMind_Architecture_Fullstack_v1_1.docx",
    DOCX_ROOT / "fullstack" / "DocMind_Updation_Traceability_v1_1.docx",
    DOCX_ROOT / "fullstack" / "DocMind_Release_Checklist_v1_1.docx",
    DOCX_ROOT / "fullstack" / "images" / "Fullstack_End_to_End_Flow_v1_1.png",
    DOCX_ROOT / "fullstack" / "images" / "Fullstack_Layered_Architecture_v1_1.png",
    DOCX_ROOT / "fullstack" / "images" / "Fullstack_Request_Response_Sequence_v1_1.jpg",
    DOCX_ROOT / "fullstack" / "images" / "Fullstack_Multi_Agent_Orchestration_Gap_v1_1.png",
    DOCX_ROOT / "reference" / "DocMind_Enterprise_Database_Architecture_Specification.docx",
    DOCX_ROOT / "reference" / "Important Documentations.docx",
    DOCS_ROOT / "docx" / "README.md",
    DOCS_ROOT / "scripts" / "doc_generators" / "README.md",
]

ARCHIVE_REQUIRED_FILES = [
    DOCX_ROOT / "archive" / "v1_0" / "frontend" / "DocMind_Architecture_Frontend_v1_0.docx",
    DOCX_ROOT / "archive" / "v1_0" / "backend" / "DocMind_Architecture_Backend_v1_0.docx",
    DOCX_ROOT / "archive" / "v1_0" / "fullstack" / "DocMind_Architecture_Fullstack_v1_0.docx",
    DOCX_ROOT / "archive" / "v1_0" / "fullstack" / "DocMind_Updation_Traceability_v1_0.docx",
]


def main() -> int:
    missing_dirs: list[Path] = [p for p in REQUIRED_DIRS if not p.exists()]
    missing_files: list[Path] = [p for p in REQUIRED_FILES if not p.exists()]
    missing_archive: list[Path] = [p for p in ARCHIVE_REQUIRED_FILES if not p.exists()]

    if missing_dirs or missing_files or missing_archive:
        print("Documentation artifact validation: FAILED")
        if missing_dirs:
            print("\nMissing directories:")
            for path in missing_dirs:
                print(f"- {path}")
        if missing_files:
            print("\nMissing required files:")
            for path in missing_files:
                print(f"- {path}")
        if missing_archive:
            print("\nMissing archive files:")
            for path in missing_archive:
                print(f"- {path}")
        return 1

    print("Documentation artifact validation: PASSED")
    print(f"Checked {len(REQUIRED_DIRS)} required directories")
    print(f"Checked {len(REQUIRED_FILES)} required files")
    print(f"Checked {len(ARCHIVE_REQUIRED_FILES)} archive files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
