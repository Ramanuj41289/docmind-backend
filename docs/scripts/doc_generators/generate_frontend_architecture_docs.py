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
FRONTEND_DOCX_DIR = DOCS_ROOT / "docx" / "frontend"
FRONTEND_IMG_DIR = FRONTEND_DOCX_DIR / "images"

OUTPUT_DOCX = FRONTEND_DOCX_DIR / "DocMind_Architecture_Frontend_v1_1.docx"
OUTPUT_PNG_1 = FRONTEND_IMG_DIR / "Frontend_System_Architecture_v1_1.png"
OUTPUT_PNG_2 = FRONTEND_IMG_DIR / "Frontend_Component_Layers_v1_1.png"
OUTPUT_JPG_1 = FRONTEND_IMG_DIR / "Frontend_User_Flows_v1_1.jpg"


@dataclass
class Box:
    x1: int
    y1: int
    x2: int
    y2: int
    text: str
    fill: str = "#1f2937"
    outline: str = "#e5e7eb"


def load_font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    candidates: list[str] = []
    if bold:
        candidates.extend(["arialbd.ttf", "segoeuib.ttf"])
    candidates.extend(["arial.ttf", "segoeui.ttf"])

    for name in candidates:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_box(draw: ImageDraw.ImageDraw, box: Box, title_font: ImageFont.ImageFont) -> None:
    draw.rounded_rectangle((box.x1, box.y1, box.x2, box.y2), radius=18, fill=box.fill, outline=box.outline, width=2)
    margin = 16
    lines = box.text.split("\n")
    line_height = 26
    y = box.y1 + margin
    for line in lines:
        draw.text((box.x1 + margin, y), line, font=title_font, fill="#f8fafc")
        y += line_height


def draw_arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color: str = "#93c5fd") -> None:
    draw.line((start, end), fill=color, width=6)
    ex, ey = end
    sx, sy = start
    if ex >= sx:
        tip = [(ex, ey), (ex - 14, ey - 8), (ex - 14, ey + 8)]
    else:
        tip = [(ex, ey), (ex + 14, ey - 8), (ex + 14, ey + 8)]
    draw.polygon(tip, fill=color)


def create_system_architecture_diagram(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#0b1020")
    draw = ImageDraw.Draw(image)

    title_font = load_font(56, bold=True)
    text_font = load_font(34)

    draw.text((70, 40), "DocMind Frontend System Architecture", font=title_font, fill="#f1f5f9")

    boxes = [
        Box(90, 190, 700, 760, "User Browser\n- React SPA\n- Router navigation\n- Auth session state", "#1d4ed8"),
        Box(820, 190, 1540, 760, "UI and State Layer\n- App.tsx orchestration\n- Navbar, Sidebar, ChatArea\n- Zustand stores", "#0f766e"),
        Box(1660, 190, 2370, 760, "API and Transport Layer\n- Axios REST client\n- SSE stream parser\n- JWT interceptors", "#7c2d12"),
        Box(90, 920, 1250, 1790, "Backend Service Layer\n- Auth, Documents, Chat, Search\n- Conversation endpoints\n- Admin endpoints", "#4c1d95"),
        Box(1360, 920, 2380, 1790, "Persistence and Retrieval\n- PostgreSQL relational tables\n- pgvector semantic index\n- chat_messages + ai_session", "#065f46"),
        Box(2480, 920, 3120, 1790, "External AI Provider\n- Embeddings\n- Chat completion\n- Retrieval-augmented answer", "#7f1d1d"),
    ]

    for box in boxes:
        draw_box(draw, box, text_font)

    draw_arrow(draw, (700, 460), (820, 460))
    draw_arrow(draw, (1540, 460), (1660, 460))
    draw_arrow(draw, (1200, 760), (1200, 920))
    draw_arrow(draw, (2000, 760), (1880, 920))
    draw_arrow(draw, (2380, 1350), (2480, 1350))

    image.save(path, format="PNG", dpi=(300, 300))


def create_component_layers_diagram(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#0a1226")
    draw = ImageDraw.Draw(image)

    title_font = load_font(54, bold=True)
    text_font = load_font(32)

    draw.text((70, 40), "DocMind Frontend Component Layers", font=title_font, fill="#f8fafc")

    layer_boxes = [
        Box(180, 210, 3020, 460, "Presentation Layer\nNavbar | Sidebar | ChatArea | UploadModal | SearchModal | AuthModal | AdminPanel | Toast", "#1e40af"),
        Box(180, 530, 3020, 780, "Application Layer\nApp.tsx event orchestration | Route-aware admin guard | keyboard shortcuts | toast management", "#166534"),
        Box(180, 850, 3020, 1100, "State Layer\nauthStore (identity/session) | documentStore (catalog/selection) | chatStore (messages/conversations/stream tokens)", "#9a3412"),
        Box(180, 1170, 3020, 1420, "Integration Layer\ndocmindApi.ts | axios interceptors | fetch-based SSE stream | upload progress handlers", "#4c1d95"),
        Box(180, 1490, 3020, 1860, "Backend Contracts\n/api/v1/auth/* | /api/v1/documents/* | /api/v1/chat/* | /api/v1/admin/*", "#0f766e"),
    ]

    for box in layer_boxes:
        draw_box(draw, box, text_font)

    for y in (460, 780, 1100, 1420):
        draw_arrow(draw, (1600, y), (1600, y + 70), color="#cbd5e1")

    image.save(path, format="PNG", dpi=(300, 300))


def create_user_flows_diagram(path: Path) -> None:
    image = Image.new("RGB", (3200, 2000), "#111827")
    draw = ImageDraw.Draw(image)

    title_font = load_font(54, bold=True)
    text_font = load_font(30)

    draw.text((70, 40), "DocMind Frontend Core User Journeys", font=title_font, fill="#f9fafb")

    boxes = [
        Box(120, 220, 980, 860, "1) Authentication Flow\n- AuthModal collects credentials\n- POST /api/v1/auth/login\n- Persist token + user in localStorage\n- Load docs + conversations", "#1d4ed8"),
        Box(1120, 220, 1980, 860, "2) Document Flow\n- Upload single/multiple files\n- Track progress callbacks\n- Refresh document catalog\n- Select scope for RAG chat", "#0f766e"),
        Box(2120, 220, 3080, 860, "3) Chat Flow\n- Start or resume conversation\n- Stream tokens via SSE OR full response\n- Persist conversationId\n- Save citations (non-streaming)", "#7c2d12"),
        Box(420, 1020, 1480, 1840, "4) Conversation Governance\n- GET conversation list\n- Load historical messages\n- Delete stale conversation\n- Export markdown transcript", "#4c1d95"),
        Box(1720, 1020, 2920, 1840, "5) Admin Operations\n- Open protected admin route\n- Read platform stats and user list\n- Patch user role\n- Surface backend authorization errors", "#166534"),
    ]

    for box in boxes:
        draw_box(draw, box, text_font)

    draw_arrow(draw, (980, 520), (1120, 520))
    draw_arrow(draw, (1980, 520), (2120, 520))
    draw_arrow(draw, (2600, 860), (2600, 1020))
    draw_arrow(draw, (1700, 520), (1200, 1020))

    image.save(path, format="JPEG", quality=96, dpi=(300, 300))


def add_heading(doc: Document, text: str, level: int = 1) -> None:
    heading = doc.add_heading(text, level=level)
    heading.alignment = WD_ALIGN_PARAGRAPH.LEFT


def add_bullets(doc: Document, items: Iterable[str]) -> None:
    for item in items:
        paragraph = doc.add_paragraph(item, style="List Bullet")
        paragraph.paragraph_format.left_indent = Pt(18)
        paragraph.paragraph_format.space_after = Pt(4)


def add_paragraph(doc: Document, text: str, indent_pt: int = 0, space_after: int = 8) -> None:
    paragraph = doc.add_paragraph(text)
    paragraph.paragraph_format.left_indent = Pt(indent_pt)
    paragraph.paragraph_format.space_after = Pt(space_after)


def insert_image_with_caption(doc: Document, path: Path, caption: str) -> None:
    doc.add_picture(str(path), width=Inches(9.6))
    cap = doc.add_paragraph(caption)
    cap.paragraph_format.space_after = Pt(12)
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER


def create_docx_report() -> None:
    doc = Document()
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width, section.page_height = section.page_height, section.page_width
    section.left_margin = Inches(0.6)
    section.right_margin = Inches(0.6)
    section.top_margin = Inches(0.6)
    section.bottom_margin = Inches(0.6)

    style = doc.styles["Normal"]
    style.font.name = "Calibri"
    style.font.size = Pt(11)

    title = doc.add_paragraph()
    run = title.add_run("DocMind Frontend Architecture Handbook")
    run.bold = True
    run.font.size = Pt(22)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER

    subtitle = doc.add_paragraph(
        f"Prepared for Engineering Leadership | Generated on {datetime.now().strftime('%d %B %Y')}"
    )
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle.paragraph_format.space_after = Pt(20)

    add_heading(doc, "1. Executive Summary", level=1)
    add_paragraph(
        doc,
        "The DocMind frontend is a React 19 and Vite single-page application that provides secure document ingestion, semantic retrieval, and conversational AI experiences. The architecture is intentionally layered to separate UI concerns, state transitions, transport concerns, and backend contracts.",
        indent_pt=12,
    )
    add_bullets(
        doc,
        [
            "Supports authenticated multi-tenant interactions with JWT-based access control.",
            "Implements both streaming and non-streaming chat paths for operational flexibility.",
            "Encapsulates backend communication in a dedicated API client with centralized error handling.",
            "Uses focused Zustand stores for auth, documents, and conversation state consistency.",
        ],
    )

    add_heading(doc, "2. Technology and Runtime Baseline", level=1)
    add_bullets(
        doc,
        [
            "Frontend runtime: React 19.3.0 + TypeScript + Vite 8.3.8",
            "State management: Zustand (authStore, documentStore, chatStore)",
            "Transport: Axios for REST + Fetch for SSE streaming",
            "Rendering: React Markdown with GFM support for answer formatting",
            "Build and proxy: Vite dev server on port 3000, /api proxied to backend on port 8081",
        ],
    )

    add_heading(doc, "3. Component Inventory and Responsibilities", level=1)
    table = doc.add_table(rows=1, cols=3)
    table.style = "Table Grid"
    hdr = table.rows[0].cells
    hdr[0].text = "Component"
    hdr[1].text = "Primary Responsibility"
    hdr[2].text = "Critical Interactions"

    rows = [
        ("App.tsx", "Top-level orchestration and cross-component workflows", "Store coordination, auth gating, modal lifecycle, toast signaling"),
        ("Navbar", "User controls, backend status, and admin access entry", "Auth modal, search modal, admin route open"),
        ("Sidebar", "Document and conversation navigation", "Select document, load/delete conversation, open upload"),
        ("ChatArea", "Message rendering and prompt submission", "Streaming token append, export, clear conversation"),
        ("UploadModal", "Single/multi-file ingestion workflow", "Upload progress callbacks, post-upload refresh"),
        ("SearchModal", "Semantic chunk search and guided chat entry", "Similarity search endpoint, chunk-to-chat transition"),
        ("DocumentChunkModal", "Document-level chunk inspection", "Detail retrieval, delete, scope-to-chat action"),
        ("AuthModal", "Login and registration entry point", "authStore login flow and refresh bootstrap"),
        ("AdminPanel", "Operational controls for privileged users", "Stats, users, role updates"),
    ]

    for component, responsibility, interactions in rows:
        cells = table.add_row().cells
        cells[0].text = component
        cells[1].text = responsibility
        cells[2].text = interactions

    add_heading(doc, "4. State Management Strategy", level=1)
    add_bullets(
        doc,
        [
            "authStore: governs current user identity, authenticated status, and unauthorized reset behavior.",
            "documentStore: maintains document catalog, selected document context, and deletion synchronization.",
            "chatStore: tracks messages, conversation metadata, active conversation id, token streaming append operations, and loading flags.",
            "The stores are intentionally bounded to avoid broad coupling and to keep testability straightforward.",
        ],
    )

    add_heading(doc, "5. API Contract Mapping", level=1)
    api_table = doc.add_table(rows=1, cols=3)
    api_table.style = "Table Grid"
    api_hdr = api_table.rows[0].cells
    api_hdr[0].text = "Domain"
    api_hdr[1].text = "Endpoint"
    api_hdr[2].text = "Client Use"

    api_rows = [
        ("Authentication", "POST /api/v1/auth/login | POST /api/v1/auth/register", "User sign-in and account creation"),
        ("Documents", "GET /api/v1/documents | GET /api/v1/documents/{id}", "Document listing and detail retrieval"),
        ("Uploads", "POST /api/v1/documents/upload | POST /api/v1/documents/upload-multiple", "Document ingestion with progress visualization"),
        ("Chat", "POST /api/v1/chat/query | POST /api/v1/chat/stream", "Non-streaming and streaming RAG question answering"),
        ("Similarity", "POST /api/v1/chat/search/similarity", "Chunk retrieval for scoped context and search modal"),
        ("Conversations", "GET /api/v1/chat/conversations | GET /api/v1/chat/conversations/{id}/messages | DELETE /api/v1/chat/conversations/{id}", "History navigation and lifecycle control"),
        ("Administration", "GET /api/v1/admin/status | GET /api/v1/admin/users | PATCH /api/v1/admin/users/{userId}/role", "Platform visibility and role governance"),
    ]

    for domain, endpoint, use in api_rows:
        row = api_table.add_row().cells
        row[0].text = domain
        row[1].text = endpoint
        row[2].text = use

    add_heading(doc, "6. Security, Reliability, and Error Handling", level=1)
    add_bullets(
        doc,
        [
            "JWT token is attached to all protected requests through request interceptors.",
            "401 responses trigger a centralized unauthorized callback that clears session state and re-prompts authentication.",
            "REST and SSE paths both return user-facing errors through a unified notification strategy.",
            "Admin route access is validated in the UI, while backend authorization remains the final source of truth.",
        ],
    )

    add_heading(doc, "7. Architecture Diagrams", level=1)
    insert_image_with_caption(doc, OUTPUT_PNG_1, "Figure 1 - End-to-end frontend system architecture")
    insert_image_with_caption(doc, OUTPUT_PNG_2, "Figure 2 - Component layering and responsibilities")
    insert_image_with_caption(doc, OUTPUT_JPG_1, "Figure 3 - Core user journeys and data flow")

    add_heading(doc, "8. Engineering Recommendations", level=1)
    add_bullets(
        doc,
        [
            "Add integration tests for auth bootstrap, streaming chat token assembly, and conversation deletion flows.",
            "Instrument client telemetry for endpoint latency, streaming completion rates, and upload failure patterns.",
            "Introduce route-level code splitting metrics and bundle budgets for sustained performance governance.",
            "Formalize API schema validation on the client boundary to protect against contract drift.",
        ],
    )

    doc.save(str(OUTPUT_DOCX))


def main() -> None:
    FRONTEND_DOCX_DIR.mkdir(parents=True, exist_ok=True)
    FRONTEND_IMG_DIR.mkdir(parents=True, exist_ok=True)
    create_system_architecture_diagram(OUTPUT_PNG_1)
    create_component_layers_diagram(OUTPUT_PNG_2)
    create_user_flows_diagram(OUTPUT_JPG_1)
    create_docx_report()

    print(f"Generated: {OUTPUT_DOCX}")
    print(f"Generated: {OUTPUT_PNG_1}")
    print(f"Generated: {OUTPUT_PNG_2}")
    print(f"Generated: {OUTPUT_JPG_1}")


if __name__ == "__main__":
    main()
