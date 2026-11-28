from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Inches, Pt


ROOT = Path(__file__).resolve().parent
DOCS_ROOT = ROOT.parents[1]
FULLSTACK_DOCX_DIR = DOCS_ROOT / "docx" / "fullstack"
FULLSTACK_IMG_DIR = FULLSTACK_DOCX_DIR / "images"

DOCX_PATH = FULLSTACK_DOCX_DIR / "DocMind_Architecture_Fullstack_v1_1.docx"
FLOW_PNG = FULLSTACK_IMG_DIR / "Fullstack_End_to_End_Flow_v1_1.png"
LAYER_PNG = FULLSTACK_IMG_DIR / "Fullstack_Layered_Architecture_v1_1.png"
SEQUENCE_JPG = FULLSTACK_IMG_DIR / "Fullstack_Request_Response_Sequence_v1_1.jpg"
ORCH_GAP_PNG = FULLSTACK_IMG_DIR / "Fullstack_Multi_Agent_Orchestration_Gap_v1_1.png"


@dataclass
class Box:
    x1: int
    y1: int
    x2: int
    y2: int
    text: str
    fill: str
    outline: str = "#e5e7eb"


def _font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    names = ["arialbd.ttf", "segoeuib.ttf"] if bold else []
    names.extend(["arial.ttf", "segoeui.ttf"])
    for name in names:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def _box(draw: ImageDraw.ImageDraw, box: Box, font: ImageFont.ImageFont) -> None:
    draw.rounded_rectangle((box.x1, box.y1, box.x2, box.y2), radius=16, fill=box.fill, outline=box.outline, width=2)
    y = box.y1 + 14
    for line in box.text.split("\n"):
        draw.text((box.x1 + 14, y), line, font=font, fill="#f8fafc")
        y += 24


def _arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color: str = "#93c5fd") -> None:
    draw.line((start, end), fill=color, width=6)
    ex, ey = end
    sx, _ = start
    if ex >= sx:
        head = [(ex, ey), (ex - 14, ey - 8), (ex - 14, ey + 8)]
    else:
        head = [(ex, ey), (ex + 14, ey - 8), (ex + 14, ey + 8)]
    draw.polygon(head, fill=color)


def make_end_to_end_flow(path: Path) -> None:
    img = Image.new("RGB", (3200, 2000), "#0b1220")
    draw = ImageDraw.Draw(img)

    title = _font(56, bold=True)
    text = _font(30)

    draw.text((70, 40), "DocMind End-to-End Full Stack Flow", font=title, fill="#f8fafc")

    boxes = [
        Box(80, 170, 560, 610, "1. User Interface\nReact + TypeScript\nAuth, Upload, Chat\nAdmin controls", "#1d4ed8"),
        Box(650, 170, 1130, 610, "2. API Gateway Layer\nJWT interceptor\nAxios REST calls\nSSE stream client", "#0369a1"),
        Box(1220, 170, 1740, 610, "3. Spring Controllers\nAuthController\nDocumentController\nChatController\nAdminController", "#0f766e"),
        Box(1830, 170, 2380, 610, "4. Service Orchestration\nAuthService\nDocumentMetadataService\nRagService\nConversationService", "#166534"),
        Box(2470, 170, 3120, 610, "5. AI + Retrieval\nOpenAI embeddings\nLLM completion\nVector similarity\nSession advisor", "#7c2d12"),
        Box(120, 760, 1220, 1820, "6. Relational Data Plane\nusers\ndocument_metadata\nconversations\nchat_messages\nai_session\nai_session_event\nai_session_index", "#4c1d95"),
        Box(1320, 760, 2300, 1820, "7. Semantic Data Plane\nvector_store\npgvector(1536)\nHNSW cosine index\nJSONB metadata filters\n(userId/documentId)", "#7f1d1d"),
        Box(2400, 760, 3120, 1820, "8. Security Boundary\nJwtAuthenticationFilter\nRole checks\nTenant isolation\nScoped repository queries", "#334155"),
    ]

    for b in boxes:
        _box(draw, b, text)

    _arrow(draw, (560, 390), (650, 390))
    _arrow(draw, (1130, 390), (1220, 390))
    _arrow(draw, (1740, 390), (1830, 390))
    _arrow(draw, (2380, 390), (2470, 390))
    _arrow(draw, (2100, 610), (2100, 760))
    _arrow(draw, (1480, 610), (760, 760))
    _arrow(draw, (2780, 610), (2780, 760))
    _arrow(draw, (2300, 1290), (2400, 1290))

    img.save(path, format="PNG", dpi=(300, 300))


def make_layered_arch(path: Path) -> None:
    img = Image.new("RGB", (3200, 2000), "#111827")
    draw = ImageDraw.Draw(img)

    title = _font(54, bold=True)
    text = _font(30)

    draw.text((70, 40), "DocMind Full Stack Layered Architecture", font=title, fill="#f8fafc")

    layers = [
        Box(180, 180, 3020, 390, "Experience Layer: Web SPA (Auth, Documents, Chat, Search, Admin)", "#1e3a8a"),
        Box(180, 460, 3020, 670, "Edge Layer: API client, JWT propagation, request/response normalization", "#075985"),
        Box(180, 740, 3020, 950, "Application Layer: Spring controllers + services + validation + exception handling", "#14532d"),
        Box(180, 1020, 3020, 1230, "AI Runtime Layer: RAG orchestration, embedding generation, prompt assembly, streaming", "#7c2d12"),
        Box(180, 1300, 3020, 1510, "Data Layer: PostgreSQL relational schema + pgvector index + Spring AI session memory tables", "#4c1d95"),
        Box(180, 1580, 3020, 1860, "Control Layer: Security filters, RBAC, tenant isolation, auditing and observability", "#334155"),
    ]

    for layer in layers:
        _box(draw, layer, text)

    for y in (390, 670, 950, 1230, 1510):
        _arrow(draw, (1600, y), (1600, y + 70), color="#d1d5db")

    img.save(path, format="PNG", dpi=(300, 300))


def make_sequence(path: Path) -> None:
    img = Image.new("RGB", (3200, 2000), "#0f172a")
    draw = ImageDraw.Draw(img)

    title = _font(48, bold=True)
    text = _font(28)

    draw.text((70, 40), "DocMind Request/Response Sequence (Upload -> Ask -> Retrieve -> Answer)", font=title, fill="#f8fafc")

    actors = [
        (300, "User"),
        (780, "Frontend"),
        (1260, "Backend API"),
        (1740, "RAG Service"),
        (2220, "Data + Vector"),
        (2860, "LLM"),
    ]

    for x, label in actors:
        draw.text((x - 70, 140), label, font=text, fill="#f1f5f9")
        draw.line((x, 210, x, 1880), fill="#475569", width=3)

    def msg(y: int, x1: int, x2: int, label: str, color: str = "#60a5fa") -> None:
        _arrow(draw, (x1, y), (x2, y), color=color)
        draw.text((min(x1, x2) + 10, y - 28), label, font=text, fill="#e2e8f0")

    msg(320, 300, 780, "Sign in / Upload / Ask")
    msg(440, 780, 1260, "POST /auth, /documents, /chat")
    msg(560, 1260, 1740, "Route + validate + invoke service")
    msg(680, 1740, 2220, "Store metadata + vectors / query vectors")
    msg(800, 1740, 2860, "Embeddings + completion request", "#f59e0b")
    msg(920, 2860, 1740, "Token stream / full answer", "#f59e0b")
    msg(1040, 1740, 2220, "Persist chat, session, citations")
    msg(1160, 1740, 1260, "ChatResponseDto / SSE chunks")
    msg(1280, 1260, 780, "HTTP response + headers")
    msg(1400, 780, 300, "Render answer + citations + history")

    img.save(path, format="JPEG", quality=96, dpi=(300, 300))


def make_orchestration_gap(path: Path) -> None:
    img = Image.new("RGB", (3200, 2000), "#101826")
    draw = ImageDraw.Draw(img)

    title = _font(46, bold=True)
    text = _font(28)

    draw.text((70, 40), "Full Stack Orchestration: Current vs Target Multi-Agent Model", font=title, fill="#f8fafc")

    boxes = [
        Box(120, 220, 1480, 900, "Current State\nSingle orchestration path\nChatController -> RagService\nShared service logic\nStrong tenant isolation", "#1d4ed8"),
        Box(1700, 220, 3080, 900, "Target State\nOrchestrator + specialist agents\nPlanner | Retriever | Policy/Eval | Summarizer\nShared context bus + guardrails", "#0f766e"),
        Box(300, 1080, 1500, 1840, "Gap Items\nAgent role contracts\nTask routing protocol\nEval checkpoints\nAgent telemetry spans", "#7c2d12"),
        Box(1700, 1080, 3080, 1840, "Transition Plan\nPhase 1: role abstractions\nPhase 2: split retrieval and policy agents\nPhase 3: full orchestrator runtime", "#166534"),
    ]

    for b in boxes:
        _box(draw, b, text)

    _arrow(draw, (1480, 560), (1700, 560))
    _arrow(draw, (900, 900), (900, 1080))
    _arrow(draw, (2400, 900), (2400, 1080))
    _arrow(draw, (1500, 1460), (1700, 1460))

    img.save(path, format="PNG", dpi=(300, 300))


def add_heading(doc: Document, text: str, level: int) -> None:
    h = doc.add_heading(text, level=level)
    h.alignment = WD_ALIGN_PARAGRAPH.LEFT


def add_bullets(doc: Document, items: Iterable[str]) -> None:
    for item in items:
        p = doc.add_paragraph(item, style="List Bullet")
        p.paragraph_format.left_indent = Pt(18)
        p.paragraph_format.space_after = Pt(4)


def add_para(doc: Document, text: str, indent: int = 0) -> None:
    p = doc.add_paragraph(text)
    p.paragraph_format.left_indent = Pt(indent)
    p.paragraph_format.space_after = Pt(8)


def add_image(doc: Document, path: Path, caption: str) -> None:
    doc.add_picture(str(path), width=Inches(9.6))
    cap = doc.add_paragraph(caption)
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_after = Pt(12)


def build_docx() -> None:
    doc = Document()
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width, section.page_height = section.page_height, section.page_width
    section.left_margin = Inches(0.6)
    section.right_margin = Inches(0.6)
    section.top_margin = Inches(0.6)
    section.bottom_margin = Inches(0.6)
    normal = doc.styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(11)

    title = doc.add_paragraph()
    run = title.add_run("DocMind Full Stack End-to-End Architecture")
    run.bold = True
    run.font.size = Pt(22)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER

    sub = doc.add_paragraph(
        f"Authoring baseline: DocMind enterprise architecture specification | Generated on {datetime.now().strftime('%d %B %Y')}"
    )
    sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub.paragraph_format.space_after = Pt(20)

    add_heading(doc, "1. Executive Architecture Overview", level=1)
    add_para(
        doc,
        "DocMind is a secured multi-tenant full stack platform that combines a React web experience, Spring Boot orchestration, PostgreSQL relational persistence, pgvector similarity retrieval, and LLM-driven response generation through a retrieval-augmented generation pipeline.",
        indent=12,
    )

    add_heading(doc, "2. Core Platform Components", level=1)
    add_bullets(
        doc,
        [
            "Frontend: React SPA with Zustand stores for identity, documents, and conversation state.",
            "Backend: Spring controllers and services for auth, documents, chat, similarity search, and administration.",
            "Data: PostgreSQL tables for users, documents, conversations, chat history, and AI session memory.",
            "Semantic retrieval: pgvector-backed vector_store with metadata-driven tenant filters.",
            "Security: JWT authentication filter, role-based authorization, and strict tenant scoped data access.",
        ],
    )

    add_heading(doc, "3. End-to-End Operational Flows", level=1)
    add_heading(doc, "3.1 Authentication", level=2)
    add_bullets(
        doc,
        [
            "User authenticates through frontend auth modal and receives a JWT token.",
            "Frontend stores token and attaches it on protected API calls through interceptors.",
            "Backend filter validates token and builds security context for all downstream calls.",
        ],
    )

    add_heading(doc, "3.2 Document Upload and Indexing", level=2)
    add_bullets(
        doc,
        [
            "Frontend submits multipart uploads and tracks progress at the UI layer.",
            "Backend stores document metadata and transitions status from uploading to processing to indexed or failed.",
            "Pipeline chunks content, generates embeddings, and writes vectors with user and document metadata tags.",
        ],
    )

    add_heading(doc, "3.3 Chat Query and Retrieval", level=2)
    add_bullets(
        doc,
        [
            "Frontend submits question with optional conversation and document scope.",
            "RAG service performs vector similarity retrieval constrained by tenant metadata.",
            "Prompt assembly combines system policy, retrieved context, session memory, and user question.",
            "Response is returned in non-streaming form or token streaming mode over SSE.",
        ],
    )

    add_heading(doc, "3.4 Conversation and Session Persistence", level=2)
    add_bullets(
        doc,
        [
            "Conversation and chat messages are persisted for audit and replay.",
            "Spring AI session tables preserve low-latency context windows for ongoing chat continuity.",
            "Conversation deletion cascades to child messages by relational constraints.",
        ],
    )

    add_heading(doc, "3.5 Administrative Operations", level=2)
    add_bullets(
        doc,
        [
            "Admin endpoints provide user listing, role updates, and platform status visibility.",
            "Role checks are enforced in backend security controls, not only in UI controls.",
        ],
    )

    add_heading(doc, "4. Data Entities and Boundaries", level=1)
    table = doc.add_table(rows=1, cols=3)
    table.style = "Table Grid"
    head = table.rows[0].cells
    head[0].text = "Domain"
    head[1].text = "Entity"
    head[2].text = "Purpose"

    rows = [
        ("Identity", "users", "Tenant identity, credentials, and role authority"),
        ("Documents", "document_metadata", "Upload metadata, lifecycle status, and ownership"),
        ("Conversations", "conversations / chat_messages", "Persistent dialog history and chronological replay"),
        ("Session Memory", "ai_session / ai_session_event / ai_session_index", "Low-latency chat memory state and event journal"),
        ("Semantic Retrieval", "vector_store", "Chunk embeddings and metadata filters for scoped retrieval"),
    ]

    for d, e, p in rows:
        cells = table.add_row().cells
        cells[0].text = d
        cells[1].text = e
        cells[2].text = p

    add_heading(doc, "5. End-to-End Diagrams", level=1)
    add_image(doc, FLOW_PNG, "Figure 1 - Full stack end-to-end flow map")
    add_image(doc, LAYER_PNG, "Figure 2 - Layered architecture decomposition")
    add_image(doc, SEQUENCE_JPG, "Figure 3 - Request/response sequence across core components")
    add_image(doc, ORCH_GAP_PNG, "Figure 4 - Multi-agent orchestration target and current-state gap")

    add_heading(doc, "6. BL-002 Orchestration Gap Closure", level=1)
    add_bullets(
        doc,
        [
            "Current runtime is single-service orchestration centered on RagService.",
            "Target design introduces orchestrator-mediated specialist agent roles.",
            "Gap is documented with phased transition items for role contracts, routing, eval, and telemetry.",
        ],
    )

    add_heading(doc, "7. Engineering Governance Notes", level=1)
    add_bullets(
        doc,
        [
            "Preserve tenant isolation by enforcing user-scoped predicates at repository and vector filter layers.",
            "Track stream completion rates, retrieval latency, and upload/indexing failures through telemetry.",
            "Keep API contracts versioned and validated to avoid frontend/backend drift during iterative releases.",
        ],
    )

    doc.save(str(DOCX_PATH))


def main() -> None:
    FULLSTACK_DOCX_DIR.mkdir(parents=True, exist_ok=True)
    FULLSTACK_IMG_DIR.mkdir(parents=True, exist_ok=True)
    make_end_to_end_flow(FLOW_PNG)
    make_layered_arch(LAYER_PNG)
    make_sequence(SEQUENCE_JPG)
    make_orchestration_gap(ORCH_GAP_PNG)
    build_docx()

    print(f"Generated: {DOCX_PATH}")
    print(f"Generated: {FLOW_PNG}")
    print(f"Generated: {LAYER_PNG}")
    print(f"Generated: {SEQUENCE_JPG}")
    print(f"Generated: {ORCH_GAP_PNG}")


if __name__ == "__main__":
    main()
