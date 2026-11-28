"""
DocMind Database Schema & ERD Architecture Specification
Generates an executive-standard Microsoft Word (.docx) document centered
directly on the 8 DBeaver schema diagrams provided by the user, with exhaustive
column-level detailing, relationship semantics, and clean spacing/indentation.
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

DOC_PATH = 'docs/DocMind_Enterprise_Database_Architecture_Specification.docx'
BACKUP_PATH = 'docs/ConsolidatedBackendDiagrams.docx'
CRISP_IMG_DIR = 'docs/diagrams/screenshots/crisp'

# Typography & Color Palette
HEX_NAVY = "1E3A8A"        # Deep Navy Header
HEX_DARK = "0F172A"        # Slate 900
HEX_SLATE = "334155"       # Slate 700
HEX_TEXT = "1E293B"        # Slate 800
HEX_MUTED = "64748B"       # Slate 500
HEX_LIGHT_BG = "F8FAFC"    # Subtle zebra stripe
HEX_BORDER = "CBD5E1"      # Subtle border
HEX_AMBER = "B45309"       # Warning / note
HEX_TEAL = "0F766E"        # Vector / Ingestion
HEX_PURPLE = "6D28D9"      # Spring AI Session

COLOR_NAVY = RGBColor(30, 58, 138)
COLOR_DARK = RGBColor(15, 23, 42)
COLOR_SLATE = RGBColor(51, 65, 85)
COLOR_TEXT = RGBColor(30, 41, 59)
COLOR_MUTED = RGBColor(100, 116, 139)


def set_cell_margins(cell, top=120, bottom=120, left=140, right=140):
    """Set inner cell padding in twips (20 twips = 1 pt)."""
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)


def set_cell_shading(cell, color_hex):
    """Set cell background color."""
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color_hex}"/>')
    tcPr.append(shd)


def set_table_borders(table, color="CBD5E1", sz="4", val="single"):
    """Set clean subtle horizontal table borders."""
    tblPr = table._tbl.tblPr
    borders = parse_xml(f'''
        <w:tblBorders {nsdecls("w")}>
            <w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:left w:val="none"/>
            <w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:right w:val="none"/>
            <w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:insideV w:val="none"/>
        </w:tblBorders>
    ''')
    tblPr.append(borders)


def add_section_heading(doc, text, level):
    h = doc.add_heading(text, level=level)
    h.paragraph_format.keep_with_next = True
    run = h.runs[0]
    run.font.name = "Segoe UI"
    if level == 1:
        run.font.size = Pt(16)
        run.font.bold = True
        run.font.color.rgb = COLOR_NAVY
        h.paragraph_format.space_before = Pt(16)
        h.paragraph_format.space_after = Pt(4)
    elif level == 2:
        run.font.size = Pt(12.5)
        run.font.bold = True
        run.font.color.rgb = COLOR_DARK
        h.paragraph_format.space_before = Pt(12)
        h.paragraph_format.space_after = Pt(3)
    elif level == 3:
        run.font.size = Pt(10.5)
        run.font.bold = True
        run.font.color.rgb = COLOR_SLATE
        h.paragraph_format.space_before = Pt(8)
        h.paragraph_format.space_after = Pt(2)
    return h


def add_p(doc, text, bold_prefix="", space_before=2, space_after=5):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = 1.15
    if bold_prefix:
        rb = p.add_run(bold_prefix)
        rb.font.name = "Segoe UI"
        rb.font.bold = True
        rb.font.size = Pt(9.5)
        rb.font.color.rgb = COLOR_DARK
    r = p.add_run(text)
    r.font.name = "Segoe UI"
    r.font.size = Pt(9.5)
    r.font.color.rgb = COLOR_TEXT
    return p


def add_bullet(doc, text, bold_prefix=""):
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.space_before = Pt(1)
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.12
    if bold_prefix:
        rb = p.add_run(bold_prefix)
        rb.font.name = "Segoe UI"
        rb.font.bold = True
        rb.font.size = Pt(9.5)
        rb.font.color.rgb = COLOR_DARK
    r = p.add_run(text)
    r.font.name = "Segoe UI"
    r.font.size = Pt(9.5)
    r.font.color.rgb = COLOR_TEXT
    return p


def add_callout(doc, text, title="ARCHITECTURAL INVARIANT", color_hex=HEX_NAVY):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_shading(cell, "F0F4F8")
    set_cell_margins(cell, top=140, bottom=140, left=180, right=140)
    
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(f'''
        <w:tcBorders {nsdecls("w")}>
            <w:top w:val="none"/>
            <w:left w:val="single" w:sz="32" w:space="0" w:color="{color_hex}"/>
            <w:bottom w:val="none"/>
            <w:right w:val="none"/>
        </w:tcBorders>
    ''')
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.12
    
    r_title = p.add_run(f"[{title}] ")
    r_title.font.name = "Segoe UI"
    r_title.font.bold = True
    r_title.font.size = Pt(9.5)
    r_title.font.color.rgb = COLOR_NAVY
    
    r_text = p.add_run(text)
    r_text.font.name = "Segoe UI"
    r_text.font.size = Pt(9.0)
    r_text.font.color.rgb = COLOR_TEXT
    
    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(2)
    sp.paragraph_format.space_after = Pt(4)


def add_diagram_image(doc, filename, caption, width_inches=4.5):
    """Embeds screenshot cleanly with center alignment, crisp sizing, and figure caption."""
    img_path = os.path.join(CRISP_IMG_DIR, filename)
    if not os.path.exists(img_path):
        print(f"Warning: {img_path} not found.")
        return
        
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.paragraph_format.space_before = Pt(8)
    p_img.paragraph_format.space_after = Pt(3)
    p_img.paragraph_format.keep_with_next = True
    
    run_img = p_img.add_run()
    run_img.add_picture(img_path, width=Inches(width_inches))
    
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(2)
    p_cap.paragraph_format.space_after = Pt(8)
    p_cap.paragraph_format.keep_with_next = True
    
    rc = p_cap.add_run(caption)
    rc.font.name = "Segoe UI"
    rc.font.size = Pt(8.5)
    rc.font.italic = True
    rc.font.color.rgb = COLOR_MUTED


def build_schema_table(doc, headers, rows_data, col_widths=None, header_bg=HEX_NAVY):
    table = doc.add_table(rows=len(rows_data) + 1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)
    
    # Header Row
    hdr_cells = table.rows[0].cells
    for i, header_text in enumerate(headers):
        hdr_cells[i].text = header_text
        set_cell_shading(hdr_cells[i], header_bg)
        set_cell_margins(hdr_cells[i], top=120, bottom=120, left=120, right=120)
        p = hdr_cells[i].paragraphs[0]
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        for run in p.runs:
            run.font.name = "Segoe UI"
            run.font.bold = True
            run.font.size = Pt(9.0)
            run.font.color.rgb = RGBColor(255, 255, 255)
            
    # Data Rows
    for row_idx, data_row in enumerate(rows_data):
        row_cells = table.rows[row_idx + 1].cells
        is_even = (row_idx % 2 == 1)
        bg_color = HEX_LIGHT_BG if is_even else "FFFFFF"
        for col_idx, cell_value in enumerate(data_row):
            row_cells[col_idx].text = str(cell_value)
            set_cell_shading(row_cells[col_idx], bg_color)
            set_cell_margins(row_cells[col_idx], top=80, bottom=80, left=120, right=120)
            p = row_cells[col_idx].paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            for run in p.runs:
                run.font.name = "Segoe UI"
                run.font.size = Pt(8.5)
                run.font.color.rgb = COLOR_TEXT
                
    if col_widths:
        for row in table.rows:
            for idx, width in enumerate(col_widths):
                row.cells[idx].width = Inches(width)
                
    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(2)
    sp.paragraph_format.space_after = Pt(6)


def generate_specification():
    doc = docx.Document()
    
    # Page setup
    for sec in doc.sections:
        sec.top_margin = Inches(0.75)
        sec.bottom_margin = Inches(0.75)
        sec.left_margin = Inches(0.75)
        sec.right_margin = Inches(0.75)
        
        # Header / Footer
        hdr = sec.header.paragraphs[0]
        hdr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        rh = hdr.add_run("DocMind Database Architecture & ERD Specification | Engineering Reference")
        rh.font.name = "Segoe UI"
        rh.font.size = Pt(8)
        rh.font.color.rgb = COLOR_MUTED
        
        ftr = sec.footer.paragraphs[0]
        ftr.alignment = WD_ALIGN_PARAGRAPH.LEFT
        rf = ftr.add_run("Confidential • DocMind Engineering Architecture • Schema & ERD Specifications")
        rf.font.name = "Segoe UI"
        rf.font.size = Pt(8)
        rf.font.color.rgb = COLOR_MUTED

    # ==========================================
    # TITLE & METADATA BLOCK
    # ==========================================
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(12)
    p_title.paragraph_format.space_after = Pt(2)
    r_tag = p_title.add_run("DOCMIND PLATFORM ARCHITECTURE SPECIFICATION\n")
    r_tag.font.name = "Segoe UI"
    r_tag.font.size = Pt(10)
    r_tag.font.bold = True
    r_tag.font.color.rgb = RGBColor(194, 65, 12)
    
    r_head = p_title.add_run("Database Schema & ERD Architecture Specification")
    r_head.font.name = "Segoe UI"
    r_head.font.size = Pt(20)
    r_head.font.bold = True
    r_head.font.color.rgb = COLOR_NAVY
    
    p_desc = doc.add_paragraph()
    p_desc.paragraph_format.space_before = Pt(2)
    p_desc.paragraph_format.space_after = Pt(12)
    r_sub = p_desc.add_run("Detailed Schema Definition, Column-Level Attributes, Foreign Key Topology, and Operational Mechanics for All 8 Production Tables")
    r_sub.font.name = "Segoe UI"
    r_sub.font.size = Pt(11)
    r_sub.font.color.rgb = COLOR_MUTED
    
    meta_headers = ["Metadata Attribute", "Engineering Specification Details"]
    meta_rows = [
        ["Document Purpose", "Definitive schema reference and ERD analysis for Senior Engineering Leadership and backend teams."],
        ["Database Engine", "PostgreSQL 16.x + pgvector Extension | Spring Data JPA / Hibernate | Spring AI JDBC Session Repository."],
        ["Analyzed ERD Diagrams", "8 DBeaver schema diagrams covering Relational Multi-Tenancy, Conversational Audit, and AI Session State."],
        ["Total Production Tables", "8 Tables: users, document_metadata, vector_store, conversations, chat_messages, ai_session, ai_session_event, ai_session_index."],
        ["Author / Reviewer", "Senior Engineering Manager & Core Platform Engineering Team."]
    ]
    build_schema_table(doc, meta_headers, meta_rows, col_widths=[2.0, 4.5], header_bg=HEX_DARK)
    
    # ==========================================
    # PART 1: CORE RELATIONAL HUB (USERS, DOCUMENTS, CHATS)
    # ==========================================
    add_section_heading(doc, "1. Core Multi-Tenant Relational Hub", level=1)
    
    add_p(doc, 
        "The DocMind relational core manages tenant identity, document metadata cataloging, and user conversation history. "
        "It enforces strict relational multi-tenancy where all assets fan out from the authenticated tenant primary key in users.id.",
        bold_prefix="Overview: ")
        
    # --- DIAGRAM 1: Central Hub ---
    add_section_heading(doc, "1.1 Multi-Tenant Central Hub: users, document_metadata & conversations", level=2)
    add_p(doc, 
        "Figure 1.1 displays the central DBeaver ER diagram showing the users table as the primary tenant anchor. "
        "It connects outward via one-to-many foreign key relationships to both document_metadata (ingested files) and conversations (chat sessions):")
        
    add_diagram_image(doc, "07_users_documents_conversations_hub.png", 
        "Figure 1.1: DBeaver ERD — Multi-Tenant Central Hub (users ||--o{ document_metadata and users ||--o{ conversations)",
        width_inches=3.4)
        
    add_p(doc, 
        "Relational Topology Analysis:\n"
        "• users ||--o{ document_metadata: A single user owns zero or more uploaded documents. The link terminates on document_metadata.user_id.\n"
        "• users ||--o{ conversations: A single user owns zero or more conversation threads. The link terminates on conversations.user_id.\n"
        "• Cascade Deletion Rule: Both child tables enforce ON DELETE CASCADE. Deleting a user automatically purges all their files and chat history.",
        bold_prefix="Relationship Links: ")

    # users table detailing
    add_section_heading(doc, "Table Specification: users", level=3)
    u_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    u_rows = [
        ["id", "123 (Key)", "BIGINT", "PK (Identity)", "Unique 64-bit integer assigned to each registered user. Anchor for multi-tenancy."],
        ["username", "A-Z", "VARCHAR(50)", "NOT NULL, UNIQUE", "Unique login handle; alphanumeric validation enforced in AuthRequestDto."],
        ["email", "A-Z", "VARCHAR(255)", "NOT NULL, UNIQUE", "RFC-5322 primary user email. Used for authentication and notifications."],
        ["password", "A-Z", "VARCHAR(255)", "NOT NULL", "Cryptographic salted BCrypt hash ($2a$12$). Plaintext is rejected at DTO layer."],
        ["role", "A-Z", "VARCHAR(20)", "NOT NULL", "RBAC authority: ROLE_USER (standard tenant) or ROLE_ADMIN (platform administrator)."]
    ]
    build_schema_table(doc, u_headers, u_rows, col_widths=[1.1, 0.9, 1.2, 1.3, 2.0], header_bg=HEX_NAVY)

    # --- DIAGRAM 2: Users & Documents ---
    add_section_heading(doc, "1.2 Document Ingestion & Cataloging: document_metadata <-> users", level=2)
    add_p(doc, 
        "Figure 1.2 isolates the document ingestion relationship between users and document_metadata. "
        "Every file uploaded via /api/v1/documents/upload creates an immutable entry in document_metadata.")
        
    add_diagram_image(doc, "06_users_document_metadata.png", 
        "Figure 1.2: DBeaver ERD — Document Ingestion Catalog (document_metadata linked to users)",
        width_inches=3.8)

    add_section_heading(doc, "Table Specification: document_metadata", level=3)
    d_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    d_rows = [
        ["id", "ID Card / Key", "UUID", "PK", "Cryptographically generated UUIDv4 uniquely identifying the uploaded document."],
        ["user_id", "123", "BIGINT", "FK -> users(id)", "Owner reference. Guaranteed non-null; enforces strict tenant boundaries."],
        ["filename", "A-Z", "VARCHAR(255)", "NOT NULL", "Sanitized source filename as uploaded by the user."],
        ["content_type", "A-Z", "VARCHAR(100)", "NOT NULL", "MIME type of document (e.g., application/pdf, text/plain)."],
        ["file_size", "123", "BIGINT", "NOT NULL", "Physical payload size in bytes."],
        ["total_pages", "123", "INT", "NULLABLE", "Total physical pages detected in paginated documents (e.g., PDF)."],
        ["total_chunks", "123", "INT", "NOT NULL", "Total number of semantic chunks generated during tokenization (default 0)."],
        ["status", "A-Z", "VARCHAR(20)", "NOT NULL", "Pipeline state machine: UPLOADING -> PROCESSING -> INDEXED | FAILED."],
        ["error_message", "A-Z", "VARCHAR(1000)", "NULLABLE", "Parser stack trace or failure diagnostic note if processing fails."],
        ["created_at", "Clock", "TIMESTAMP", "NOT NULL", "UTC timestamp of document submission."],
        ["updated_at", "Clock", "TIMESTAMP", "NOT NULL", "UTC timestamp of last status transition or chunk index completion."]
    ]
    build_schema_table(doc, d_headers, d_rows, col_widths=[1.2, 0.9, 1.2, 1.2, 2.0], header_bg=HEX_TEAL)

    # --- DIAGRAM 3: Users, Conversations & Messages ---
    add_section_heading(doc, "1.3 User Conversational Audit Trail: chat_messages -> conversations -> users", level=2)
    add_p(doc, 
        "Figure 1.3 details the complete relational chat hierarchy. "
        "A user owns conversations, and each conversation contains a sequential log of chat_messages.")
        
    add_diagram_image(doc, "05_users_conversations_messages.png", 
        "Figure 1.3: DBeaver ERD — Complete Chat Hierarchy (chat_messages -> conversations -> users)",
        width_inches=5.2)

    add_p(doc, 
        "Relational Flow & Cascade Rules:\n"
        "• chat_messages -> conversations: conversation_id references conversations.id (ON DELETE CASCADE).\n"
        "• conversations -> users: user_id references users.id (ON DELETE CASCADE).\n"
        "• When a user deletes a conversation in the UI, all associated chat_messages are atomically removed by the database engine.",
        bold_prefix="Hierarchy & Cascades: ")

    # --- DIAGRAM 4: Conversations & Messages ---
    add_diagram_image(doc, "04_conversations_chat_messages.png", 
        "Figure 1.4: DBeaver ERD — Focused Conversation to Message Relational Link",
        width_inches=3.8)

    add_section_heading(doc, "Table Specification: conversations", level=3)
    c_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    c_rows = [
        ["id", "A-Z (Key)", "VARCHAR(255)", "PK", "Coordinated UUID string. Identical to Spring AI ai_session.id."],
        ["user_id", "123", "BIGINT", "FK -> users(id)", "Tenant ownership key. Ensures users can only access their own threads."],
        ["title", "A-Z", "VARCHAR(255)", "NOT NULL", "Thread display title in the UI, auto-initialized from opening user prompt."],
        ["created_at", "Clock", "TIMESTAMP", "NOT NULL", "UTC creation timestamp of the conversation thread."],
        ["updated_at", "Clock", "TIMESTAMP", "NOT NULL", "Timestamp of most recent message exchange (indexed for sidebar sorting)."]
    ]
    build_schema_table(doc, c_headers, c_rows, col_widths=[1.1, 0.9, 1.2, 1.3, 2.0], header_bg=HEX_SLATE)

    add_section_heading(doc, "Table Specification: chat_messages", level=3)
    m_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    m_rows = [
        ["id", "123 (Key)", "BIGINT", "PK (Identity)", "Monotonically increasing message ID for chronological UI rendering."],
        ["conversation_id", "A-Z", "VARCHAR(255)", "FK -> conv(id)", "Parent thread identifier. Indexed for fast sequential retrieval."],
        ["message_type", "A-Z", "VARCHAR(20)", "NOT NULL", "Role classification: USER (question), ASSISTANT (AI reply), SYSTEM."],
        ["content", "A-Z", "TEXT", "NOT NULL", "Full raw text content of user query or LLM generated response."],
        ["created_at", "Clock", "TIMESTAMP", "NOT NULL", "Message insertion timestamp (UTC)."]
    ]
    build_schema_table(doc, m_headers, m_rows, col_widths=[1.2, 0.9, 1.2, 1.2, 2.0], header_bg=HEX_SLATE)

    # ==========================================
    # PART 2: SPRING AI SESSION SUBSYSTEM
    # ==========================================
    add_section_heading(doc, "2. Spring AI Low-Latency Session Memory Subsystem", level=1)
    
    add_p(doc, 
        "In addition to the JPA chat audit trail, DocMind deploys Spring AI's official JDBC Session Repository schema. "
        "This tier acts as a stateful, high-throughput context buffer that feeds the active sliding window (last 10 turns) "
        "directly into LLM prompts via MessageChatMemoryAdvisor, completely decoupling prompt assembly from relational JPA overhead.",
        bold_prefix="Dual-Memory Strategy: ")

    # --- DIAGRAM 5: AI Session Ecosystem ---
    add_section_heading(doc, "2.1 Spring AI Session Ecosystem: ai_session, ai_session_event & ai_session_index", level=2)
    add_p(doc, 
        "Figure 2.1 displays the full DBeaver ER diagram for the Spring AI session subsystem. "
        "The ai_session table serves as the session root, flanked by ai_session_event (the event stream) and ai_session_index (attribute lookup):")
        
    add_diagram_image(doc, "01_ai_session_ecosystem.png", 
        "Figure 2.1: DBeaver ERD — Spring AI Session Ecosystem (ai_session Root, ai_session_event Journal & ai_session_index)",
        width_inches=5.2)

    # --- DIAGRAM 6: Session Events ---
    add_section_heading(doc, "2.2 Event Journaling & Turn Tracking: ai_session_event <-> ai_session", level=2)
    add_p(doc, 
        "Figure 2.2 focuses on the event journaling mechanism between ai_session_event and ai_session:")
        
    add_diagram_image(doc, "02_ai_session_events.png", 
        "Figure 2.2: DBeaver ERD — Spring AI Session Event Journal (ai_session_event linked to ai_session)",
        width_inches=4.2)

    # --- DIAGRAM 7: Session Index ---
    add_section_heading(doc, "2.3 Dynamic Attribute Indexing: ai_session_index <-> ai_session", level=2)
    add_p(doc, 
        "Figure 2.3 focuses on the attribute lookup index linking ai_session_index to ai_session:")
        
    add_diagram_image(doc, "03_ai_session_index.png", 
        "Figure 2.3: DBeaver ERD — Spring AI Session Attribute Index (ai_session_index linked to ai_session)",
        width_inches=3.8)

    add_section_heading(doc, "Table Specification: ai_session", level=3)
    ais_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    ais_rows = [
        ["id", "A-Z (Key)", "VARCHAR(36)", "PK", "Session UUID string matching conversations.id."],
        ["created_at", "Clock", "TIMESTAMP", "NOT NULL", "Session initialization timestamp."],
        ["expires_at", "Clock", "TIMESTAMP", "NULLABLE", "TTL threshold for automated background cache expiration."],
        ["metadata", "A-Z", "TEXT", "NULLABLE", "Serialized JSON session configuration and runtime parameters."],
        ["event_version", "123", "INT", "NOT NULL", "Optimistic locking version counter preventing concurrent write conflicts."]
    ]
    build_schema_table(doc, ais_headers, ais_rows, col_widths=[1.2, 0.9, 1.2, 1.2, 2.0], header_bg=HEX_PURPLE)

    add_section_heading(doc, "Table Specification: ai_session_event", level=3)
    aie_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    aie_rows = [
        ["id", "A-Z (Key)", "VARCHAR(36)", "PK", "Unique event identifier UUID."],
        ["seq", "123", "INT", "NOT NULL", "Strictly increasing sequence number defining event order within session."],
        ["session_id", "A-Z", "VARCHAR(36)", "FK -> ai_session(id)", "Parent session reference (ON DELETE CASCADE)."],
        ["timestamp", "Clock", "TIMESTAMP", "NOT NULL", "UTC timestamp of the interaction."],
        ["event_type", "A-Z", "VARCHAR(50)", "NOT NULL", "Event tag: MESSAGE, TURN_START, TOOL_EXECUTION."],
        ["message_type", "A-Z", "VARCHAR(50)", "NULLABLE", "Spring AI message type: USER, ASSISTANT, SYSTEM."],
        ["message_content", "A-Z", "TEXT", "NULLABLE", "Passage text injected into the LLM context buffer."],
        ["payload", "A-Z", "TEXT", "NULLABLE", "Serialized JSON execution state, function calling parameters, or tokens."],
        ["synthetic", "Checkbox", "BOOLEAN", "NOT NULL", "True if message was synthesized by RAG advisor; False if raw user input."],
        ["metadata", "A-Z", "TEXT", "NULLABLE", "Supplemental telemetry, token usage counts, and finish reasons."]
    ]
    build_schema_table(doc, aie_headers, aie_rows, col_widths=[1.2, 0.9, 1.2, 1.2, 2.0], header_bg=HEX_PURPLE)

    add_section_heading(doc, "Table Specification: ai_session_index", level=3)
    aii_headers = ["Column", "DBeaver Icon", "SQL Type", "Constraint", "Business & Operational Role"]
    aii_rows = [
        ["session_id", "A-Z (Key)", "VARCHAR(36)", "FK -> ai_session(id)", "Session reference key."],
        ["attr_name", "A-Z (Key)", "VARCHAR(100)", "NOT NULL", "Session attribute name/key for fast lookup."],
        ["attr_value", "A-Z", "VARCHAR(255)", "NULLABLE", "Session attribute string value."]
    ]
    build_schema_table(doc, aii_headers, aii_rows, col_widths=[1.2, 0.9, 1.2, 1.2, 2.0], header_bg=HEX_PURPLE)

    # ==========================================
    # PART 3: VECTOR STORE (PGVECTOR)
    # ==========================================
    add_section_heading(doc, "3. High-Dimensional Semantic Vector Space (pgvector)", level=1)
    
    add_p(doc, 
        "The vector_store table is the semantic foundation of DocMind's RAG pipeline. "
        "It stores tokenized document chunk passages alongside 1536-dimensional embeddings generated by OpenAI's text-embedding-3-small model.",
        bold_prefix="Overview: ")

    # --- DIAGRAM 8: Vector Store ---
    add_section_heading(doc, "3.1 Vector Store Schema: vector_store", level=2)
    add_diagram_image(doc, "08_vector_store_schema.png", 
        "Figure 3.1: DBeaver Table Schema — vector_store (id, content, metadata JSONB, embedding VECTOR)",
        width_inches=2.4)

    add_section_heading(doc, "Table Specification: vector_store", level=3)
    v_headers = ["Column", "DBeaver Icon", "SQL Type", "Indexing Strategy", "Business & Operational Role"]
    v_rows = [
        ["id", "ID Card / Key", "UUID", "PRIMARY KEY (B-Tree)", "Unique UUID assigned to the individual text chunk passage."],
        ["content", "A-Z", "TEXT", "GIN Full-Text (Optional)", "The raw textual chunk passage extracted from the document for LLM context injection."],
        ["metadata", "{ } (JSON)", "JSONB", "GIN (jsonb_path_ops)", "JSON payload containing userId, documentId, fileName, chunkIndex, pageNumber."],
        ["embedding", "Bar List", "VECTOR(1536)", "HNSW (vector_cosine_ops)", "1536-dimensional dense embedding vector produced by OpenAI text-embedding-3-small."]
    ]
    build_schema_table(doc, v_headers, v_rows, col_widths=[1.1, 0.9, 1.2, 1.3, 2.0], header_bg=HEX_TEAL)

    add_section_heading(doc, "3.2 JSONB Metadata Schema Breakdown", level=3)
    add_p(doc, 
        "The metadata column stores structured document context enabling composite filtering without altering table DDL:", bold_prefix="JSONB Attributes: ")
        
    add_bullet(doc, "userId (VARCHAR): Crucial multi-tenant discriminator ensuring users can never search other tenants' embeddings.")
    add_bullet(doc, "documentId (VARCHAR): UUID of parent document_metadata record; enables scoped single-document chat.")
    add_bullet(doc, "fileName (VARCHAR): Original uploaded file name for transparent source citation in AI answers.")
    add_bullet(doc, "contentType (VARCHAR): MIME type of original document (e.g., application/pdf).")
    add_bullet(doc, "chunkIndex (INT): 0-based index reflecting chronological position of the chunk in the document.")
    add_bullet(doc, "pageNumber (INT): 1-based physical page number where the passage appears (for PDF citations).")

    add_callout(doc,
        "Mathematical Distance & HNSW Configuration: DocMind computes vector similarity using cosine distance (<=>). "
        "Production HNSW index: CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64). "
        "At query time, RagService enforces: WHERE (metadata->>'userId' = :userId AND metadata->>'documentId' = :docId) ORDER BY embedding <=> :queryVector LIMIT 5. "
        "This guarantees strict tenant isolation and sub-15ms ANN retrieval latency.",
        title="PGVECTOR PRODUCTION SPECIFICATION",
        color_hex=HEX_TEAL)

    # ==========================================
    # PART 4: CONSOLIDATED MASTER DATA DICTIONARY
    # ==========================================
    add_section_heading(doc, "4. Consolidated Master Data Dictionary", level=1)
    
    add_p(doc, 
        "The following master reference consolidates all 8 tables across the DocMind enterprise database catalog:", bold_prefix="Catalog Summary: ")

    cat_headers = ["Table Name", "Domain Subsystem", "Primary Key", "Foreign Keys", "Total Columns", "Operational Role"]
    cat_rows = [
        ["users", "Identity & Multi-Tenancy", "id (BIGINT)", "None", "5", "Tenant anchor, authentication, BCrypt credentials, RBAC."],
        ["document_metadata", "Document Ingestion", "id (UUID)", "user_id -> users(id)", "11", "File catalog, byte sizes, chunk counters, state machine."],
        ["vector_store", "pgvector Semantic Search", "id (UUID)", "None (JSONB documentId)", "4", "1536-dim embeddings, text chunks, HNSW cosine index."],
        ["conversations", "Conversational Audit", "id (VARCHAR)", "user_id -> users(id)", "5", "Chat thread metadata, user ownership, UI sidebar display."],
        ["chat_messages", "Conversational Audit", "id (BIGINT)", "conversation_id -> conv(id)", "5", "Persistent message ledger, chronological chat history."],
        ["ai_session", "Spring AI Session Memory", "id (VARCHAR)", "None (Shared UUID)", "5", "Active session root, TTL expiration, optimistic locking."],
        ["ai_session_event", "Spring AI Session Memory", "id (VARCHAR)", "session_id -> ai_session(id)", "10", "Turn-by-turn event stream, pre-serialized prompt context."],
        ["ai_session_index", "Spring AI Session Memory", "(session_id, attr_name)", "session_id -> ai_session(id)", "3", "Dynamic session attribute key-value index."]
    ]
    build_schema_table(doc, cat_headers, cat_rows, col_widths=[1.3, 1.4, 1.1, 1.2, 0.6, 0.9], header_bg=HEX_DARK)

    # ==========================================
    # PART 5: PRODUCTION OPERATIONS & INDEXING
    # ==========================================
    add_section_heading(doc, "5. Production Indexing & Maintenance Guidelines", level=1)
    
    add_p(doc, 
        "To achieve sub-20ms query latency under high concurrent load, the database schema implements the following indexing standards:", bold_prefix="Performance Standards: ")

    idx_headers = ["Index Name", "Target Table", "Columns & Index Type", "Query Acceleration Target"]
    idx_rows = [
        ["idx_users_username", "users", "username (B-Tree, UNIQUE)", "Accelerates login lookup by username."],
        ["idx_users_email", "users", "email (B-Tree, UNIQUE)", "Enforces unique registration and email authentication."],
        ["idx_doc_meta_user_created", "document_metadata", "user_id, created_at DESC (Composite B-Tree)", "Powers user document catalog dashboard: WHERE user_id = ? ORDER BY created_at DESC."],
        ["idx_vector_embedding_hnsw", "vector_store", "embedding (HNSW, vector_cosine_ops)", "Accelerates approximate nearest neighbor vector search (<=>)."],
        ["idx_vector_metadata_gin", "vector_store", "metadata (GIN, jsonb_path_ops)", "Accelerates JSONB tenant containment queries: metadata @> '{\"userId\":\"1\"}'."],
        ["idx_conv_user_updated", "conversations", "user_id, updated_at DESC (Composite B-Tree)", "Optimizes user conversation sidebar: WHERE user_id = ? ORDER BY updated_at DESC."],
        ["idx_chat_msg_conv_created", "chat_messages", "conversation_id, created_at ASC (Composite B-Tree)", "Enables sequential message retrieval for chat history rendering."],
        ["idx_ai_session_event_lookup", "ai_session_event", "session_id, seq ASC (Composite B-Tree)", "Powers sliding window retrieval for Spring AI LLM prompts."]
    ]
    build_schema_table(doc, idx_headers, idx_rows, col_widths=[1.6, 1.2, 1.7, 2.0], header_bg=HEX_NAVY)

    # Save documents
    os.makedirs(os.path.dirname(DOC_PATH), exist_ok=True)
    doc.save(DOC_PATH)
    doc.save(BACKUP_PATH)
    print(f"Successfully generated {DOC_PATH} and {BACKUP_PATH}")


if __name__ == '__main__':
    generate_specification()
