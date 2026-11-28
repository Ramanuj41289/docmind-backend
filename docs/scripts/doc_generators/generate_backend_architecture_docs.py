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
BACKEND_DOCX_DIR = DOCS_ROOT / "docx" / "backend"
BACKEND_IMG_DIR = BACKEND_DOCX_DIR / "images"

OUTPUT_DOCX = BACKEND_DOCX_DIR / "DocMind_Architecture_Backend_v1_1.docx"
IMG_COMPONENT = BACKEND_IMG_DIR / "Backend_Component_Map_v1_1.png"
IMG_FLOW = BACKEND_IMG_DIR / "Backend_End_to_End_Request_Flows_v1_1.png"
IMG_DATA = BACKEND_IMG_DIR / "Backend_Data_and_Memory_Architecture_v1_1.jpg"
IMG_EVAL = BACKEND_IMG_DIR / "Backend_Eval_Pipeline_v1_1.png"
IMG_OBS = BACKEND_IMG_DIR / "Backend_Observability_Tracing_v1_1.png"
IMG_HITL = BACKEND_IMG_DIR / "Backend_HITL_Checkpoints_v1_1.png"


@dataclass
class Box:
    x1: int
    y1: int
    x2: int
    y2: int
    text: str
    fill: str
    outline: str = "#e5e7eb"


def load_font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    names: list[str] = []
    if bold:
        names.extend(["arialbd.ttf", "segoeuib.ttf"])
    names.extend(["arial.ttf", "segoeui.ttf"])
    for name in names:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_box(draw: ImageDraw.ImageDraw, box: Box, font: ImageFont.ImageFont) -> None:
    draw.rounded_rectangle((box.x1, box.y1, box.x2, box.y2), radius=18, fill=box.fill, outline=box.outline, width=2)
    y = box.y1 + 16
    for line in box.text.split("\n"):
        draw.text((box.x1 + 16, y), line, font=font, fill="#f8fafc")
        y += 30


def draw_arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color: str = "#93c5fd") -> None:
    draw.line((start, end), fill=color, width=6)
    ex, ey = end
    sx, _ = start
    if ex >= sx:
        tip = [(ex, ey), (ex - 14, ey - 8), (ex - 14, ey + 8)]
    else:
        tip = [(ex, ey), (ex + 14, ey - 8), (ex + 14, ey + 8)]
    draw.polygon(tip, fill=color)


def build_component_map(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#0b1220")
    draw = ImageDraw.Draw(image)

    title = load_font(56, bold=True)
    text = load_font(32)

    draw.text((70, 40), "DocMind Backend Component Architecture", font=title, fill="#f8fafc")

    boxes = [
        Box(90, 180, 760, 760, "Security Boundary\nJwtAuthenticationFilter\nSecurityFilterChain\nRole checks + auth context", "#334155"),
        Box(860, 180, 1540, 760, "Controller Layer\nAuthController\nDocumentController\nChatController\nAdminController", "#0f766e"),
        Box(1640, 180, 2450, 760, "Service Layer\nAuthService + UserService\nDocumentMetadataService\nRagService + ConversationService\nAdminService", "#166534"),
        Box(2550, 180, 3120, 760, "AI Runtime Layer\nChatClient\nSessionAdvisors\nVectorStore similaritySearch\nStreaming Flux", "#7c2d12"),
        Box(90, 930, 1400, 1860, "Persistence Layer\nJPA repositories\nusers\ndocument_metadata\nconversations\nchat_messages", "#4c1d95"),
        Box(1500, 930, 2420, 1860, "Semantic + Session Memory\nvector_store (pgvector 1536)\nai_session\nai_session_event\nai_session_index", "#7f1d1d"),
        Box(2520, 930, 3120, 1860, "External AI\nEmbeddings provider\nChat completion provider", "#1d4ed8"),
    ]

    for box in boxes:
        draw_box(draw, box, text)

    draw_arrow(draw, (760, 460), (860, 460))
    draw_arrow(draw, (1540, 460), (1640, 460))
    draw_arrow(draw, (2450, 460), (2550, 460))
    draw_arrow(draw, (2050, 760), (2050, 930))
    draw_arrow(draw, (1280, 760), (800, 930))
    draw_arrow(draw, (2840, 760), (2840, 930))
    draw_arrow(draw, (2420, 1400), (2520, 1400))

    image.save(path, format="PNG", dpi=(300, 300))


def build_request_flows(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#0f172a")
    draw = ImageDraw.Draw(image)

    title = load_font(52, bold=True)
    text = load_font(28)

    draw.text((70, 40), "DocMind Backend End-to-End Request Flows", font=title, fill="#f8fafc")

    flows = [
        Box(90, 190, 1030, 820, "1) Authentication\nPOST /api/v1/auth/login\nAuthenticate user\nIssue JWT token\nReturn user profile", "#1d4ed8"),
        Box(1120, 190, 2080, 820, "2) Document Ingestion\nPOST /api/v1/documents/upload\nParse + chunk + embed\nPersist metadata + vectors\nStatus: UPLOADING->INDEXED", "#0f766e"),
        Box(2170, 190, 3120, 820, "3) Chat and Retrieval\nPOST /api/v1/chat/query|stream\nVector search with user filter\nSession advisor adds memory\nPersist response + citations", "#7c2d12"),
        Box(90, 960, 1470, 1860, "4) Conversation Lifecycle\nGET /chat/conversations\nGET /chat/conversations/{id}/messages\nDELETE /chat/conversations/{id}\nCascade delete messages", "#4c1d95"),
        Box(1570, 960, 3120, 1860, "5) Admin and Governance\nGET /api/v1/admin/users\nGET /api/v1/admin/status\nPATCH /api/v1/admin/users/{id}/role\nRBAC + audit logging", "#166534"),
    ]

    for flow in flows:
        draw_box(draw, flow, text)

    draw_arrow(draw, (1030, 500), (1120, 500))
    draw_arrow(draw, (2080, 500), (2170, 500))
    draw_arrow(draw, (2640, 820), (2640, 960))
    draw_arrow(draw, (1650, 820), (780, 960))

    image.save(path, format="PNG", dpi=(300, 300))


def build_data_architecture(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#111827")
    draw = ImageDraw.Draw(image)

    title = load_font(50, bold=True)
    text = load_font(28)

    draw.text((70, 40), "DocMind Backend Data and Memory Architecture", font=title, fill="#f8fafc")

    boxes = [
        Box(100, 220, 920, 940, "Relational Tenant Hub\nusers\ndocument_metadata\nconversations\nchat_messages\nFK + cascade policy", "#1e3a8a"),
        Box(1020, 220, 1980, 940, "Semantic Retrieval Plane\nvector_store\nembedding VECTOR(1536)\nJSONB metadata (userId, documentId)\nHNSW cosine index", "#7f1d1d"),
        Box(2080, 220, 3120, 940, "Session Memory Plane\nai_session\nai_session_event\nai_session_index\nSliding context window", "#4c1d95"),
        Box(280, 1120, 1540, 1860, "Data Consistency Controls\nuser-scoped repository methods\nRagService filter expressions\nconversation ownership checks\ntransactional writes", "#14532d"),
        Box(1660, 1120, 3020, 1860, "Performance Controls\nB-Tree indexes on tenant keys\nGIN/JSONB filters\nVector ANN search\nresponse-time observability", "#7c2d12"),
    ]

    for box in boxes:
        draw_box(draw, box, text)

    draw_arrow(draw, (920, 560), (1020, 560))
    draw_arrow(draw, (1980, 560), (2080, 560))
    draw_arrow(draw, (1450, 940), (1100, 1120))
    draw_arrow(draw, (2500, 940), (2300, 1120))

    image.save(path, format="JPEG", quality=96, dpi=(300, 300))


def build_eval_pipeline(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#0f172a")
    draw = ImageDraw.Draw(image)

    title = load_font(50, bold=True)
    text = load_font(28)
    draw.text((70, 40), "Backend Eval Pipeline and Continuous Improvement", font=title, fill="#f8fafc")

    boxes = [
        Box(120, 260, 980, 860, "1) Prompt + Tool Plan\nRagService + ChatClient\nInitial response generation", "#1d4ed8"),
        Box(1120, 260, 2060, 860, "2) Eval Checks\nAnswer quality\nCitation coverage\nPolicy compliance\nLatency threshold", "#0f766e"),
        Box(2200, 260, 3080, 860, "3) Decision\nPass -> return response\nFail -> fallback/regenerate\nFlag for review", "#7c2d12"),
        Box(280, 1040, 1480, 1820, "4) Feedback Signals\nUser feedback\nAdmin quality review\nError traces", "#4c1d95"),
        Box(1640, 1040, 2940, 1820, "5) Improvement Loop\nTune prompts/topK\nAdjust thresholds\nUpdate guidance docs", "#166534"),
    ]

    for b in boxes:
        draw_box(draw, b, text)

    draw_arrow(draw, (980, 560), (1120, 560))
    draw_arrow(draw, (2060, 560), (2200, 560))
    draw_arrow(draw, (2640, 860), (2640, 1040))
    draw_arrow(draw, (1840, 860), (1080, 1040))

    image.save(path, format="PNG", dpi=(300, 300))


def build_observability(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#111827")
    draw = ImageDraw.Draw(image)

    title = load_font(50, bold=True)
    text = load_font(28)
    draw.text((70, 40), "Backend Observability and Tracing Stack", font=title, fill="#f8fafc")

    boxes = [
        Box(100, 250, 1000, 900, "Signal Sources\nController/service logs\nAPI errors\nStream completion events", "#1e3a8a"),
        Box(1140, 250, 2140, 900, "Collection Layer\nStructured JSON logging\nTrace id propagation\nMetrics counters/timers", "#075985"),
        Box(2280, 250, 3120, 900, "Observability Platform\nLog store\nMetrics dashboard\nTrace explorer", "#7c2d12"),
        Box(260, 1080, 1500, 1840, "Deployment Evidence (Current)\nSLF4J logger coverage in services\nError and lifecycle logging in RagService\nController-level API envelope patterns", "#4c1d95"),
        Box(1640, 1080, 2960, 1840, "Deployment Target\nOpenTelemetry collector\nCentral dashboard SLOs\nAlerting for latency/error budgets", "#14532d"),
    ]

    for b in boxes:
        draw_box(draw, b, text)

    draw_arrow(draw, (1000, 560), (1140, 560))
    draw_arrow(draw, (2140, 560), (2280, 560))
    draw_arrow(draw, (2500, 900), (2500, 1080))
    draw_arrow(draw, (1600, 900), (960, 1080))

    image.save(path, format="PNG", dpi=(300, 300))


def build_hitl(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#0b1220")
    draw = ImageDraw.Draw(image)

    title = load_font(50, bold=True)
    text = load_font(28)
    draw.text((70, 40), "Backend Human-in-the-Loop Checkpoints", font=title, fill="#f8fafc")

    boxes = [
        Box(110, 250, 980, 880, "High-Risk Request\nAdmin role change\nBulk deletion\nSensitive export", "#1d4ed8"),
        Box(1110, 250, 2050, 880, "Checkpoint Gate\nPolicy check\nPermission check\nRisk score", "#0f766e"),
        Box(2180, 250, 3090, 880, "Human Approval\nReviewer decision\nApprove or reject\nAudit note", "#7c2d12"),
        Box(340, 1040, 1450, 1820, "Execute Path\nProceed with API action\nPersist event and actor\nReturn controlled response", "#166534"),
        Box(1600, 1040, 2920, 1820, "Rejection Path\nBlock action\nReturn reason code\nRecord policy event", "#7f1d1d"),
    ]

    for b in boxes:
        draw_box(draw, b, text)

    draw_arrow(draw, (980, 560), (1110, 560))
    draw_arrow(draw, (2050, 560), (2180, 560))
    draw_arrow(draw, (2560, 880), (2560, 1040))
    draw_arrow(draw, (2340, 880), (1040, 1040))

    image.save(path, format="PNG", dpi=(300, 300))


def add_heading(doc: Document, text: str, level: int) -> None:
    heading = doc.add_heading(text, level=level)
    heading.alignment = WD_ALIGN_PARAGRAPH.LEFT


def add_paragraph(doc: Document, text: str, indent: int = 0) -> None:
    p = doc.add_paragraph(text)
    p.paragraph_format.left_indent = Pt(indent)
    p.paragraph_format.space_after = Pt(8)


def add_bullets(doc: Document, items: Iterable[str]) -> None:
    for item in items:
        p = doc.add_paragraph(item, style="List Bullet")
        p.paragraph_format.left_indent = Pt(18)
        p.paragraph_format.space_after = Pt(4)


def add_image(doc: Document, path: Path, caption: str) -> None:
    doc.add_picture(str(path), width=Inches(9.6))
    c = doc.add_paragraph(caption)
    c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    c.paragraph_format.space_after = Pt(12)


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
    run = title.add_run("DocMind Backend Architecture Handbook")
    run.bold = True
    run.font.size = Pt(22)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER

    sub = doc.add_paragraph(
        f"Integrated backend analysis across frontend contracts and database design | Generated on {datetime.now().strftime('%d %B %Y')}"
    )
    sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub.paragraph_format.space_after = Pt(20)

    add_heading(doc, "1. End-to-End Analysis Summary", 1)
    add_paragraph(
        doc,
        "The backend architecture acts as the integration hub between the React frontend and the PostgreSQL plus pgvector data platform. It enforces tenant isolation, orchestrates document ingestion and retrieval-augmented generation, and persists both audit-grade conversation history and low-latency session memory.",
        12,
    )
    add_bullets(
        doc,
        [
            "Frontend contract entry points: auth, documents, chat query/stream, similarity search, conversation lifecycle, and admin operations.",
            "Core orchestration: controller -> service -> repository/vector-store pipeline with explicit authenticated-user scoping.",
            "Database integration: relational ownership model plus semantic search and Spring AI session memory tables.",
            "Security model: JWT authentication filter, RBAC checks, and query-level tenant filtering.",
        ],
    )

    add_heading(doc, "2. Backend Components", 1)
    add_bullets(
        doc,
        [
            "Controllers: AuthController, DocumentController, ChatController, AdminController.",
            "Services: AuthService, UserService, DocumentMetadataService, RagService, ConversationService, AdminService, SessionHistoryMigrationService, ConversationHistoryWriter.",
            "Persistence: UserRepository, DocumentMetadataRepository, ConversationRepository, ChatMessageRepository, VectorStore.",
            "Runtime: Spring AI ChatClient with SessionAdvisors and streamed response support.",
        ],
    )

    add_heading(doc, "3. End-to-End Request Analysis", 1)
    add_bullets(
        doc,
        [
            "Authentication flow validates credentials and issues JWT used by all protected frontend API calls.",
            "Upload flow parses, chunks, embeds, and stores vectors with user and document metadata tags.",
            "Query flow retrieves user-scoped chunks, composes context, and generates responses in streaming or non-streaming modes.",
            "Conversation flow persists user and assistant turns and supports historical fetch and deletion with ownership checks.",
            "Admin flow provides status visibility and role management under backend-enforced authorization.",
        ],
    )

    add_heading(doc, "4. Data and Multi-Tenancy Boundaries", 1)
    table = doc.add_table(rows=1, cols=3)
    table.style = "Table Grid"
    header = table.rows[0].cells
    header[0].text = "Data Domain"
    header[1].text = "Primary Tables"
    header[2].text = "Isolation or Access Rule"

    rows = [
        ("Identity", "users", "Authenticated principal determines tenant scope"),
        ("Documents", "document_metadata", "All operations filtered by user ownership"),
        ("Conversations", "conversations, chat_messages", "Conversation id must belong to current user"),
        ("Semantic Search", "vector_store", "FilterExpression enforces userId and optional documentId"),
        ("Session Memory", "ai_session, ai_session_event, ai_session_index", "Conversation session scoped to authenticated user context"),
    ]

    for domain, tables, rule in rows:
        cells = table.add_row().cells
        cells[0].text = domain
        cells[1].text = tables
        cells[2].text = rule

    add_heading(doc, "5. Architecture Diagrams", 1)
    add_image(doc, IMG_COMPONENT, "Figure 1 - Backend component architecture map")
    add_image(doc, IMG_FLOW, "Figure 2 - Backend end-to-end request flows")
    add_image(doc, IMG_DATA, "Figure 3 - Backend data and memory architecture")
    add_image(doc, IMG_EVAL, "Figure 4 - Backend eval pipeline and continuous improvement")
    add_image(doc, IMG_OBS, "Figure 5 - Backend observability and tracing stack")
    add_image(doc, IMG_HITL, "Figure 6 - Backend human-in-the-loop checkpoints")

    add_heading(doc, "6. BL-001/BL-003/BL-004 Closure Notes", 1)
    add_bullets(
        doc,
        [
            "BL-001: Explicit evaluation pipeline is now documented with pass/fail decision points and feedback loop.",
            "BL-003: Observability stack and deployment evidence are documented with current signals and target telemetry platform.",
            "BL-004: Human approval checkpoints are defined for high-risk workflows with approve/reject branches.",
        ],
    )

    add_heading(doc, "7. Diagram Validation Against Runtime", 1)
    add_bullets(
        doc,
        [
            "Frontend to backend contracts are explicitly represented via API endpoint domains.",
            "Backend to database layers are separated into relational, vector, and session memory responsibilities.",
            "Streaming and non-streaming paths are both represented for chat requests.",
            "Tenant isolation is documented at security, service filtering, and persistence levels.",
        ],
    )

    doc.save(str(OUTPUT_DOCX))


def main() -> None:
    BACKEND_DOCX_DIR.mkdir(parents=True, exist_ok=True)
    BACKEND_IMG_DIR.mkdir(parents=True, exist_ok=True)

    build_component_map(IMG_COMPONENT)
    build_request_flows(IMG_FLOW)
    build_data_architecture(IMG_DATA)
    build_eval_pipeline(IMG_EVAL)
    build_observability(IMG_OBS)
    build_hitl(IMG_HITL)
    build_docx()

    print(f"Generated: {OUTPUT_DOCX}")
    print(f"Generated: {IMG_COMPONENT}")
    print(f"Generated: {IMG_FLOW}")
    print(f"Generated: {IMG_DATA}")
    print(f"Generated: {IMG_EVAL}")
    print(f"Generated: {IMG_OBS}")
    print(f"Generated: {IMG_HITL}")


if __name__ == "__main__":
    main()
