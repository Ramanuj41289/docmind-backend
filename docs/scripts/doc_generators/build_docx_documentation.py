"""
DocMind Enterprise Database Architecture Specification Generator
Produces a high-standard, publication-grade Microsoft Word (.docx) document
tailored for Senior Engineering Leadership and backend engineering teams.
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

DOC_PATH = 'docs/DocMind_Enterprise_Database_Architecture_Specification.docx'
SCHEMAS_DIR = 'docs/diagrams/schemas'
SCREENSHOTS_DIR = 'docs/diagrams/screenshots'

# Color Palette Constants
HEX_PRIMARY = "1E3A8A"      # Deep Navy Blue
HEX_SECONDARY = "065F46"    # Deep Emerald Green
HEX_ACCENT = "C2410C"       # Deep Amber/Orange
HEX_DARK = "0F172A"         # Slate 900
HEX_TEXT = "1E293B"         # Slate 800
HEX_MUTED = "64748B"        # Slate 500
HEX_LIGHT_BG = "F8FAFC"     # Slate 50
HEX_BORDER = "CBD5E1"       # Slate 300
HEX_NOTE_BG = "EFF6FF"      # Light Blue
HEX_NOTE_BORDER = "3B82F6"  # Blue 500
HEX_WARN_BG = "FFFBEB"      # Light Amber
HEX_WARN_BORDER = "F59E0B"  # Amber 500

COLOR_PRIMARY = RGBColor(30, 58, 138)
COLOR_SECONDARY = RGBColor(6, 95, 70)
COLOR_ACCENT = RGBColor(194, 65, 12)
COLOR_DARK = RGBColor(15, 23, 42)
COLOR_TEXT = RGBColor(30, 41, 59)
COLOR_MUTED = RGBColor(100, 116, 139)


def set_cell_margins(cell, top=120, bottom=120, left=150, right=150):
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
    """Set background color of a table cell."""
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color_hex}"/>')
    tcPr.append(shd)


def set_table_borders(table, color="CBD5E1", sz="4", val="single"):
    """Set subtle, professional borders on a table."""
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


def add_callout(doc, text, title="ARCHITECTURAL NOTE", kind="info"):
    """Creates a beautifully styled callout box with a colored left accent border."""
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    
    bg_hex = HEX_NOTE_BG if kind == "info" else HEX_WARN_BG
    border_hex = HEX_NOTE_BORDER if kind == "info" else HEX_WARN_BORDER
    
    set_cell_shading(cell, bg_hex)
    set_cell_margins(cell, top=160, bottom=160, left=200, right=160)
    
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(f'''
        <w:tcBorders {nsdecls("w")}>
            <w:top w:val="none"/>
            <w:left w:val="single" w:sz="36" w:space="0" w:color="{border_hex}"/>
            <w:bottom w:val="none"/>
            <w:right w:val="none"/>
        </w:tcBorders>
    ''')
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(4)
    run_title = p.add_run(f"[{title}] ")
    run_title.font.bold = True
    run_title.font.size = Pt(10)
    run_title.font.name = "Segoe UI"
    run_title.font.color.rgb = RGBColor(30, 64, 175) if kind == "info" else RGBColor(180, 83, 9)
    
    run_text = p.add_run(text)
    run_text.font.size = Pt(9.5)
    run_text.font.name = "Segoe UI"
    run_text.font.color.rgb = COLOR_TEXT
    
    # Add spacing after callout
    sp_p = doc.add_paragraph()
    sp_p.paragraph_format.space_before = Pt(2)
    sp_p.paragraph_format.space_after = Pt(4)


def add_styled_heading(doc, text, level):
    h = doc.add_heading(text, level=level)
    h.paragraph_format.keep_with_next = True
    run = h.runs[0]
    run.font.name = "Segoe UI"
    if level == 1:
        run.font.size = Pt(18)
        run.font.bold = True
        run.font.color.rgb = COLOR_PRIMARY
        h.paragraph_format.space_before = Pt(18)
        h.paragraph_format.space_after = Pt(6)
    elif level == 2:
        run.font.size = Pt(14)
        run.font.bold = True
        run.font.color.rgb = COLOR_SECONDARY
        h.paragraph_format.space_before = Pt(14)
        h.paragraph_format.space_after = Pt(4)
    elif level == 3:
        run.font.size = Pt(11.5)
        run.font.bold = True
        run.font.color.rgb = COLOR_ACCENT
        h.paragraph_format.space_before = Pt(10)
        h.paragraph_format.space_after = Pt(2)
    return h


def add_paragraph(doc, text, bold_prefix="", space_after=6):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = 1.15
    if bold_prefix:
        r_pre = p.add_run(bold_prefix)
        r_pre.font.bold = True
        r_pre.font.name = "Segoe UI"
        r_pre.font.size = Pt(10)
        r_pre.font.color.rgb = COLOR_DARK
    r = p.add_run(text)
    r.font.name = "Segoe UI"
    r.font.size = Pt(10)
    r.font.color.rgb = COLOR_TEXT
    return p


def add_bullet(doc, text, bold_prefix=""):
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.12
    if bold_prefix:
        r_pre = p.add_run(bold_prefix)
        r_pre.font.bold = True
        r_pre.font.name = "Segoe UI"
        r_pre.font.size = Pt(9.5)
        r_pre.font.color.rgb = COLOR_DARK
    r = p.add_run(text)
    r.font.name = "Segoe UI"
    r.font.size = Pt(9.5)
    r.font.color.rgb = COLOR_TEXT
    return p


def add_image_figure(doc, image_path, caption, width_inches=6.5):
    """Embeds an image with proper alignment and a styled figure caption."""
    if not os.path.exists(image_path):
        print(f"Warning: Image {image_path} does not exist.")
        return
        
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.paragraph_format.space_before = Pt(8)
    p_img.paragraph_format.space_after = Pt(3)
    run_img = p_img.add_run()
    run_img.add_picture(image_path, width=Inches(width_inches))
    
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(2)
    p_cap.paragraph_format.space_after = Pt(12)
    r_cap = p_cap.add_run(caption)
    r_cap.font.name = "Segoe UI"
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED


def build_table(doc, headers, rows_data, col_widths=None, header_bg=HEX_PRIMARY):
    """Builds a formatted table with header styling, zebra striping, and cell padding."""
    table = doc.add_table(rows=len(rows_data) + 1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)
    
    # Format Header Row
    hdr_cells = table.rows[0].cells
    for i, header_text in enumerate(headers):
        hdr_cells[i].text = header_text
        set_cell_shading(hdr_cells[i], header_bg)
        set_cell_margins(hdr_cells[i], top=140, bottom=140, left=140, right=140)
        p = hdr_cells[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        for run in p.runs:
            run.font.bold = True
            run.font.name = "Segoe UI"
            run.font.size = Pt(9.5)
            run.font.color.rgb = RGBColor(255, 255, 255)
            
    # Format Data Rows
    for row_idx, data_row in enumerate(rows_data):
        row_cells = table.rows[row_idx + 1].cells
        is_even = (row_idx % 2 == 1)
        bg_color = HEX_LIGHT_BG if is_even else "FFFFFF"
        for col_idx, cell_value in enumerate(data_row):
            row_cells[col_idx].text = str(cell_value)
            set_cell_shading(row_cells[col_idx], bg_color)
            set_cell_margins(row_cells[col_idx], top=100, bottom=100, left=140, right=140)
            p = row_cells[col_idx].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            for run in p.runs:
                run.font.name = "Segoe UI"
                run.font.size = Pt(9.0)
                run.font.color.rgb = COLOR_TEXT
                
    # Set explicit column widths if provided
    if col_widths:
        for row in table.rows:
            for idx, width in enumerate(col_widths):
                row.cells[idx].width = Inches(width)
                
    # Spacing after table
    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(2)
    sp.paragraph_format.space_after = Pt(8)


def create_document():
    doc = docx.Document()
    
    # Page setup - 0.75 inch margins for standard executive document layout
    for section in doc.sections:
        section.top_margin = Inches(0.75)
        section.bottom_margin = Inches(0.75)
        section.left_margin = Inches(0.75)
        section.right_margin = Inches(0.75)
        
        # Header & Footer setup
        header = section.header
        hp = header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hrun = hp.add_run("DocMind — Enterprise Database Architecture Specification | Confidential")
        hrun.font.name = "Segoe UI"
        hrun.font.size = Pt(8)
        hrun.font.color.rgb = COLOR_MUTED
        
        footer = section.footer
        fp = footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.LEFT
        frun = fp.add_run("DocMind Engineering Standards • Relational PostgreSQL + pgvector + Spring AI Session Subsystem")
        frun.font.name = "Segoe UI"
        frun.font.size = Pt(8)
        frun.font.color.rgb = COLOR_MUTED

    # ==========================================
    # COVER / HEADER BLOCK
    # ==========================================
    title_p = doc.add_paragraph()
    title_p.paragraph_format.space_before = Pt(24)
    title_p.paragraph_format.space_after = Pt(4)
    r_badge = title_p.add_run("DOCMIND PLATFORM ARCHITECTURE SPECIFICATION\n")
    r_badge.font.name = "Segoe UI"
    r_badge.font.size = Pt(11)
    r_badge.font.bold = True
    r_badge.font.color.rgb = COLOR_ACCENT
    
    r_main_title = title_p.add_run("Enterprise Database Architecture & Schema Specification")
    r_main_title.font.name = "Segoe UI"
    r_main_title.font.size = Pt(24)
    r_main_title.font.bold = True
    r_main_title.font.color.rgb = COLOR_PRIMARY
    
    sub_p = doc.add_paragraph()
    sub_p.paragraph_format.space_before = Pt(2)
    sub_p.paragraph_format.space_after = Pt(16)
    r_sub = sub_p.add_run("Comprehensive Technical Design Reference: Relational Multi-Tenancy, High-Dimensional pgvector Storage, and Dual-Layer Conversational Session Memory")
    r_sub.font.name = "Segoe UI"
    r_sub.font.size = Pt(12)
    r_sub.font.color.rgb = COLOR_MUTED
    
    # Metadata summary table
    meta_headers = ["Attribute", "Specification Details"]
    meta_rows = [
        ["Document Author / Role", "Senior Engineering Manager & Platform Architecture Team"],
        ["Target Audience", "Backend Engineers, Data Architects, AI/ML Platform Specialists"],
        ["Database Engines", "PostgreSQL 16.x with pgvector Extension, Spring Data JPA / Hibernate, Spring AI JDBC Session"],
        ["Vector Dimensions & Model", "1536-dimensional float vector (OpenAI text-embedding-3-small)"],
        ["Vector Distance & Indexing", "Cosine Distance (<=> operator), HNSW Indexing (m=16, ef_construction=64)"],
        ["Document Status", "Approved Production Engineering Standard (v2.4.0)"],
        ["Classification", "DocMind Engineering Proprietary / Strictly Confidential"]
    ]
    build_table(doc, meta_headers, meta_rows, col_widths=[2.2, 4.3], header_bg=HEX_DARK)
    
    doc.add_page_break()

    # ==========================================
    # SECTION 1: EXECUTIVE SUMMARY & ARCHITECTURAL TOPOLOGY
    # ==========================================
    add_styled_heading(doc, "1. Executive Summary & Architecture Topology", level=1)
    
    add_paragraph(doc, 
        "DocMind is an enterprise-grade AI Document Intelligence and Retrieval-Augmented Generation (RAG) platform. "
        "The underlying database persistence layer is engineered as a hybrid polyglot subsystem combining ACID relational data integrity, "
        "high-dimensional approximate nearest neighbor (ANN) vector retrieval, and high-throughput conversational session state management. "
        "This specification establishes the architectural foundation, schema definitions, indexing strategies, and multi-tenant security guarantees "
        "implemented across the 8 database tables governing the platform.")
        
    add_paragraph(doc,
        "The DocMind persistence architecture is organized into three distinct, highly cohesive subsystems:", bold_prefix="Subsystem Categorization: ")
        
    add_bullet(doc, "Subsystem 1: Identity & Access Governance (users) — Enforces role-based access control (RBAC), BCrypt credential protection, and acts as the universal tenant isolation anchor for all downstream data assets.")
    add_bullet(doc, "Subsystem 2: Document Ingestion & High-Dimensional Semantic Vector Space (document_metadata, vector_store) — Manages the lifecycle of uploaded files, physical chunking, extraction metadata, and 1536-dimensional embeddings indexed via HNSW.")
    add_bullet(doc, "Subsystem 3: Dual-Layer Conversational Memory (conversations, chat_messages, ai_session, ai_session_event, ai_session_index) — Bridges user-facing chronological chat history with low-latency LLM context window injection via Spring AI.")

    add_styled_heading(doc, "1.1 Master Database Relational & Vector Architecture (ERD)", level=2)
    add_paragraph(doc,
        "The following master Entity-Relationship Diagram illustrates the complete relational topology, foreign key constraints, "
        "and logical JSONB foreign relationships across all 8 production tables:")
        
    add_image_figure(doc, 
        os.path.join(SCHEMAS_DIR, "01_master_database_erd.png"), 
        "Figure 1.1: DocMind Master Database Architecture (ERD) — Relational Schemas, Constraints & pgvector Integration",
        width_inches=6.5)
        
    add_callout(doc,
        "Tenant Isolation Anchor Principle: Every conversational interaction, uploaded document, and embedded vector chunk is bound directly or indirectly "
        "to a verified user identifier (users.id). Queries across relational and vector domains must enforce non-bypassable predicates targeting this tenant boundary.",
        title="CORE ARCHITECTURAL INVARIANT",
        kind="info")

    # ==========================================
    # SECTION 2: SUBSYSTEM 1 - IDENTITY & MULTI-TENANCY
    # ==========================================
    add_styled_heading(doc, "2. Identity, Authentication & Tenant Isolation Boundary", level=1)
    
    add_paragraph(doc,
        "The identity subsystem is anchored by the users table. It provides single-source-of-truth authorization "
        "for JWT token minting, user verification, and relational ownership.")
        
    add_styled_heading(doc, "2.1 Schema Definition: users Table", level=2)
    add_paragraph(doc,
        "The users table stores core account credentials and assigned platform roles. Passwords are required to be salted and hashed "
        "using BCrypt with a minimum cost factor of 12 prior to persistence. Direct cleartext credentials never touch disk.")

    user_tbl_headers = ["Column Name", "SQL Data Type", "Constraints", "Description & Invariants"]
    user_tbl_rows = [
        ["id", "BIGINT", "PRIMARY KEY, GENERATED ALWAYS AS IDENTITY", "Synthetic unique identifier assigned to each registered user account."],
        ["username", "VARCHAR(50)", "NOT NULL, UNIQUE", "Unique alphanumeric login identifier selected by the user."],
        ["email", "VARCHAR(255)", "NOT NULL, UNIQUE", "RFC-5322 compliant email address utilized for communication and identity claims."],
        ["password", "VARCHAR(255)", "NOT NULL", "BCrypt salted cryptographic hash ($2a$ / $2b$). Never stored as plaintext."],
        ["role", "VARCHAR(20)", "NOT NULL", "RBAC authorization authority: ROLE_USER (standard tenant) or ROLE_ADMIN (platform administrator)."]
    ]
    build_table(doc, user_tbl_headers, user_tbl_rows, col_widths=[1.4, 1.3, 1.6, 2.2], header_bg=HEX_PRIMARY)
    
    add_paragraph(doc,
        "Multi-Tenancy Enforcement Model: In DocMind, multi-tenancy is implemented at the schema row level (Shared Database, Shared Schema) "
        "using discriminatory foreign keys. Every entity with user affinity carries a strict user_id foreign key constraint. "
        "At runtime, Spring Security's SecurityContextHolder extracts the authenticated principal from the validated JWT token, "
        "and Spring Data JPA repositories inject this user identity into all repository calls (e.g., findByUserOrderByCreatedAtDesc).",
        bold_prefix="Tenancy Implementation: ")

    # ==========================================
    # SECTION 3: SUBSYSTEM 2 - DOCUMENT INGESTION & VECTOR STORAGE
    # ==========================================
    add_styled_heading(doc, "3. Document Ingestion & High-Dimensional Vector Architecture", level=1)
    
    add_paragraph(doc,
        "The ingestion subsystem accepts unstructured documents (PDF, DOCX, TXT), decomposes them into semantic textual chunks, "
        "and writes structured metadata to PostgreSQL while simultaneously populating high-dimensional vector embeddings in pgvector.")
        
    add_styled_heading(doc, "3.1 User & Document Relationship Model", level=2)
    add_paragraph(doc,
        "Figure 3.1 details the relational link between the authenticated tenant (users) and their uploaded document metadata catalog (document_metadata), "
        "as well as the semantic linkage into the pgvector vector_store.")

    add_image_figure(doc, 
        os.path.join(SCHEMAS_DIR, "02_user_and_document_erd.png"), 
        "Figure 3.1: Document Ingestion, Ownership, and Vector Store Relationship Model",
        width_inches=6.5)

    add_styled_heading(doc, "3.2 Schema Definition: document_metadata Table", level=2)
    add_paragraph(doc,
        "The document_metadata table tracks the complete ingestion lifecycle, parse statistics, and physical attributes of uploaded files.")

    doc_meta_headers = ["Column Name", "SQL Data Type", "Constraints", "Description & Invariants"]
    doc_meta_rows = [
        ["id", "UUID", "PRIMARY KEY", "Cryptographically generated UUIDv4 assigned upon document submission."],
        ["user_id", "BIGINT", "NOT NULL, FK -> users(id) ON DELETE CASCADE", "Foreign key defining tenant ownership. Ensures automatic cleanup if an account is removed."],
        ["filename", "VARCHAR(255)", "NOT NULL", "Sanitized original filename as presented during multipart upload."],
        ["content_type", "VARCHAR(100)", "NOT NULL", "MIME type of the source file (e.g., application/pdf, text/plain)."],
        ["file_size", "BIGINT", "NOT NULL", "Physical binary payload size measured in bytes."],
        ["total_pages", "INT", "NULLABLE", "Total physical pages detected in paginated documents (e.g., PDF)."],
        ["total_chunks", "INT", "NOT NULL, DEFAULT 0", "Number of distinct semantic text chunks generated during tokenization."],
        ["status", "VARCHAR(20)", "NOT NULL", "Ingestion state machine: UPLOADING -> PROCESSING -> INDEXED | FAILED."],
        ["error_message", "VARCHAR(1000)", "NULLABLE", "Diagnostics, stack traces, or parser failure notes if status transition fails."],
        ["created_at", "TIMESTAMP", "NOT NULL, DEFAULT CURRENT_TIMESTAMP", "UTC timestamp recording initial document upload time."],
        ["updated_at", "TIMESTAMP", "NOT NULL, DEFAULT CURRENT_TIMESTAMP", "UTC timestamp tracking status transitions and chunk count updates."]
    ]
    build_table(doc, doc_meta_headers, doc_meta_rows, col_widths=[1.3, 1.4, 1.6, 2.2], header_bg=HEX_SECONDARY)

    add_styled_heading(doc, "3.3 Schema Definition: vector_store Table (pgvector)", level=2)
    add_paragraph(doc,
        "The vector_store table is provisioned via the official Spring AI pgvector schema and augmented with enterprise HNSW indexing. "
        "It stores the extracted textual passages, their high-dimensional vector embeddings, and tenant scoping metadata.")

    vec_tbl_headers = ["Column Name", "SQL Data Type", "Indexing Strategy", "Description & Invariants"]
    vec_tbl_rows = [
        ["id", "UUID", "PRIMARY KEY (B-Tree)", "Unique UUID assigned to the individual text chunk passage."],
        ["content", "TEXT", "GIN Full-Text (Optional)", "The raw textual chunk passage extracted from the document for LLM context injection."],
        ["metadata", "JSONB", "GIN Index (jsonb_path_ops)", "JSON document containing documentId, userId, fileName, chunkIndex, and pageNumber."],
        ["embedding", "VECTOR(1536)", "HNSW Index (vector_cosine_ops)", "1536-dimensional dense embedding vector produced by OpenAI text-embedding-3-small."]
    ]
    build_table(doc, vec_tbl_headers, vec_tbl_rows, col_widths=[1.2, 1.3, 1.6, 2.4], header_bg=HEX_SECONDARY)

    add_styled_heading(doc, "3.4 High-Dimensional pgvector Architecture & Search Mechanics", level=2)
    add_paragraph(doc,
        "Figure 3.2 illustrates the internal architecture of the pgvector storage engine, detailing JSONB metadata attributes, "
        "HNSW index configurations, and vector query execution mechanics:")

    add_image_figure(doc, 
        os.path.join(SCHEMAS_DIR, "05_vector_store_architecture.png"), 
        "Figure 3.2: pgvector Storage Architecture, JSONB Metadata Schema, and HNSW Cosine Indexing",
        width_inches=6.5)

    add_paragraph(doc, "Mathematical Distance Formulation: Semantic search computes cosine distance using pgvector's <=> operator. "
                       "The relationship between cosine distance d and cosine similarity s is defined as:", bold_prefix="Vector Distance Metric: ")
    add_paragraph(doc, "Similarity(A, B) = 1.0 - (embedding <=> query_vector) = (A • B) / (||A|| * ||B||)")
    
    add_paragraph(doc, 
        "HNSW Index Optimization Parameters: In production, the vector column is indexed with Hierarchical Navigable Small World graphs:\n"
        "CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);\n"
        "• m = 16: Maximum number of bidirectional links created per vector node in the graph, providing optimal recall vs build memory.\n"
        "• ef_construction = 64: Number of nearest neighbors evaluated during index creation, ensuring tight clustering of related passages.\n"
        "• ef_search = 40 (Session Parameter): Runtime candidate pool size ensuring sub-15ms ANN query latency under concurrent traffic.",
        bold_prefix="Index Tuning: ")

    add_callout(doc,
        "Cross-Tenant Data Leakage Prevention: When searching the vector store, the application NEVER performs an unbounded vector search. "
        "The Spring AI SearchRequest enforces a composite filter expression: filter.and(filter.eq('userId', currentUser.getId()), filter.eq('documentId', docId)). "
        "In SQL, this compiles to: WHERE (metadata->>'userId' = :userId AND metadata->>'documentId' = :docId). "
        "This guarantees mathematical isolation between tenant vector spaces even though they reside in a unified shared table.",
        title="SECURITY MANDATE: COMPOSITE FILTERING",
        kind="warning")

    # ==========================================
    # SECTION 4: SUBSYSTEM 3 - CONVERSATIONAL PERSISTENCE & DUAL-MEMORY
    # ==========================================
    add_styled_heading(doc, "4. Conversational History & Dual-Memory Architecture", level=1)
    
    add_paragraph(doc,
        "A critical architectural innovation in DocMind is the Dual-Memory Persistence Pattern. "
        "Traditional LLM applications often suffer from a severe architectural dilemma: either they store only raw chat history "
        "(forcing expensive context truncation and slow token serialization on every prompt), or they rely solely on transient session caches "
        "(losing user-facing conversation history across browser refreshes). DocMind solves this by cleanly separating the conversational domain into two synchronized tiers.")

    add_image_figure(doc, 
        os.path.join(SCHEMAS_DIR, "03_chat_and_session_erd.png"), 
        "Figure 4.1: Dual-Memory Architecture — JPA Relational UI History vs Spring AI Low-Latency Session Memory",
        width_inches=6.5)

    add_styled_heading(doc, "4.1 Architectural Rationale: Dual-Memory Strategy", level=2)
    add_paragraph(doc,
        "The platform coordinates two independent storage layers mapped to the identical conversationId UUID:", bold_prefix="Tier Breakdown: ")
        
    add_bullet(doc, "Layer 1: Relational UI History (conversations & chat_messages) — Managed by Spring Data JPA / Hibernate. "
                    "This tier acts as the permanent, audit-grade conversational ledger. It enables paginated message retrieval for frontend UI clients, "
                    "conversation title editing, message role tagging (USER, ASSISTANT, SYSTEM), and user-scoped conversation deletion.")
    add_bullet(doc, "Layer 2: Spring AI Low-Latency JDBC Session Memory (ai_session, ai_session_event, ai_session_index) — Managed natively by Spring AI's MessageChatMemoryAdvisor and JdbcChatMemoryRepository. "
                    "This tier acts as a stateful, high-throughput event bus and sliding context window (last N=10 turns). "
                    "It pre-serializes turns into optimized prompt payloads, implements optimistic locking (event_version) to prevent race conditions during rapid user prompt streams, "
                    "and supports automated time-to-live (TTL) expiration via expires_at.")

    add_styled_heading(doc, "4.2 Schema Definitions: UI Conversational Tier", level=2)
    
    conv_tbl_headers = ["Column Name", "SQL Data Type", "Constraints", "Description & Invariants"]
    conv_tbl_rows = [
        ["id", "VARCHAR(255)", "PRIMARY KEY", "UUID string identifying the conversation. Replicated into Spring AI ai_session.id."],
        ["user_id", "BIGINT", "NOT NULL, FK -> users(id) ON DELETE CASCADE", "Tenant identifier. Ensures users can only access their own conversation records."],
        ["title", "VARCHAR(255)", "NOT NULL", "Conversation header/title, dynamically initialized from the user's opening prompt or explicitly updated."],
        ["created_at", "TIMESTAMP", "NOT NULL, DEFAULT CURRENT_TIMESTAMP", "UTC creation timestamp of the conversation thread."],
        ["updated_at", "TIMESTAMP", "NOT NULL, DEFAULT CURRENT_TIMESTAMP", "UTC timestamp of the most recent message exchange."]
    ]
    build_table(doc, conv_tbl_headers, conv_tbl_rows, col_widths=[1.3, 1.4, 1.6, 2.2], header_bg=HEX_ACCENT)

    msg_tbl_headers = ["Column Name", "SQL Data Type", "Constraints", "Description & Invariants"]
    msg_tbl_rows = [
        ["id", "BIGINT", "PRIMARY KEY, GENERATED ALWAYS AS IDENTITY", "Sequential message identifier for chronological ordering."],
        ["conversation_id", "VARCHAR(255)", "NOT NULL, FK -> conversations(id) ON DELETE CASCADE", "Parent conversation UUID. JPA cascade delete cleans all messages on thread removal."],
        ["message_type", "VARCHAR(20)", "NOT NULL", "Originator classification: USER, ASSISTANT, or SYSTEM."],
        ["content", "TEXT", "NOT NULL", "Full raw text of the question, AI response, or system preamble."],
        ["created_at", "TIMESTAMP", "NOT NULL, DEFAULT CURRENT_TIMESTAMP", "UTC timestamp recording exact message arrival time."]
    ]
    build_table(doc, msg_tbl_headers, msg_tbl_rows, col_widths=[1.4, 1.4, 1.7, 2.0], header_bg=HEX_ACCENT)

    add_styled_heading(doc, "4.3 Schema Definitions: Spring AI Session Subsystem", level=2)
    add_paragraph(doc,
        "Figure 4.2 details the internal relational structure of Spring AI's JDBC Session Repository schema, "
        "comprising the session root (ai_session), the chronological event stream (ai_session_event), and the dynamic attribute index (ai_session_index):")

    add_image_figure(doc, 
        os.path.join(SCHEMAS_DIR, "04_spring_ai_session_erd.png"), 
        "Figure 4.2: Spring AI JDBC Session Schema — Session Root, Event Journal, and Attribute Indexing",
        width_inches=6.5)

    ai_sess_headers = ["Column Name", "SQL Data Type", "Constraints", "Description & Invariants"]
    ai_sess_rows = [
        ["id", "VARCHAR(36)", "PRIMARY KEY", "Coordinated session UUID matching conversations.id."],
        ["created_at", "TIMESTAMP", "NOT NULL", "Session initialization timestamp."],
        ["expires_at", "TIMESTAMP", "NULLABLE", "TTL timestamp for automated context pruning and garbage collection."],
        ["metadata", "TEXT", "NULLABLE", "JSON-encoded session configurations, model parameters, and tenant context."],
        ["event_version", "INT", "NOT NULL, DEFAULT 0", "Optimistic locking counter incremented on every event append to prevent dirty writes."]
    ]
    build_table(doc, ai_sess_headers, ai_sess_rows, col_widths=[1.3, 1.4, 1.5, 2.3], header_bg=HEX_DARK)

    ai_evt_headers = ["Column Name", "SQL Data Type", "Constraints", "Description & Invariants"]
    ai_evt_rows = [
        ["id", "VARCHAR(36)", "PRIMARY KEY", "Unique UUID assigned to each conversation event."],
        ["seq", "INT", "NOT NULL", "Strictly increasing sequence number defining event order within the session."],
        ["session_id", "VARCHAR(36)", "NOT NULL, FK -> ai_session(id) ON DELETE CASCADE", "Foreign key linking event to parent session root."],
        ["timestamp", "TIMESTAMP", "NOT NULL", "UTC timestamp of the interaction."],
        ["event_type", "VARCHAR(50)", "NOT NULL", "Event classification (e.g., MESSAGE, TURN_START, TOOL_EXECUTION)."],
        ["message_type", "VARCHAR(50)", "NULLABLE", "Spring AI message abstraction: USER, ASSISTANT, SYSTEM."],
        ["message_content", "TEXT", "NULLABLE", "Passage text injected into the LLM context buffer."],
        ["payload", "TEXT", "NULLABLE", "Serialized JSON execution state, function calling parameters, or model tokens."],
        ["synthetic", "BOOLEAN", "NOT NULL, DEFAULT FALSE", "Flag indicating whether the message was synthesized by a RAG advisor or directly spoken by the user."],
        ["metadata", "TEXT", "NULLABLE", "Supplemental telemetry, token usage counts, and completion finish reasons."]
    ]
    build_table(doc, ai_evt_headers, ai_evt_rows, col_widths=[1.3, 1.4, 1.6, 2.2], header_bg=HEX_DARK)

    # ==========================================
    # SECTION 5: COMPLETE DATA DICTIONARY
    # ==========================================
    add_styled_heading(doc, "5. Complete Enterprise Data Dictionary", level=1)
    
    add_paragraph(doc,
        "The following master reference consolidates all 8 tables, 43 database columns, constraints, "
        "and operational rules across the DocMind enterprise database catalog:")

    dict_headers = ["Table Name", "Column Name", "Type & Nullability", "Key / Index", "Operational Function & Constraints"]
    dict_rows = [
        # users
        ["users", "id", "BIGINT NOT NULL", "PK (Identity)", "Tenant primary key. Auto-incrementing 64-bit integer."],
        ["users", "username", "VARCHAR(50) NOT NULL", "UK (Unique)", "Unique user handle; validated against alphanumeric regex."],
        ["users", "email", "VARCHAR(255) NOT NULL", "UK (Unique)", "Primary communication email; unique index enforced."],
        ["users", "password", "VARCHAR(255) NOT NULL", "None", "BCrypt hash ($2a$12$). Plaintext is rejected by DTO validators."],
        ["users", "role", "VARCHAR(20) NOT NULL", "None", "Authorization scope: ROLE_USER or ROLE_ADMIN."],
        # document_metadata
        ["document_metadata", "id", "UUID NOT NULL", "PK", "Primary key for file metadata entity."],
        ["document_metadata", "user_id", "BIGINT NOT NULL", "FK -> users(id)", "Tenant ownership. ON DELETE CASCADE."],
        ["document_metadata", "filename", "VARCHAR(255) NOT NULL", "None", "Original sanitized filename of uploaded artifact."],
        ["document_metadata", "content_type", "VARCHAR(100) NOT NULL", "None", "MIME type (PDF, DOCX, TXT, MD)."],
        ["document_metadata", "file_size", "BIGINT NOT NULL", "None", "File size in bytes."],
        ["document_metadata", "total_pages", "INT NULL", "None", "Detected page count for paginated files."],
        ["document_metadata", "total_chunks", "INT NOT NULL", "None", "Count of tokenized chunks produced."],
        ["document_metadata", "status", "VARCHAR(20) NOT NULL", "None", "Status enum: UPLOADING, PROCESSING, INDEXED, FAILED."],
        ["document_metadata", "error_message", "VARCHAR(1000) NULL", "None", "Diagnostic error trace if parsing/indexing fails."],
        ["document_metadata", "created_at", "TIMESTAMP NOT NULL", "None", "Initial creation timestamp (UTC)."],
        ["document_metadata", "updated_at", "TIMESTAMP NOT NULL", "None", "Last modification timestamp (UTC)."],
        # vector_store
        ["vector_store", "id", "UUID NOT NULL", "PK", "Unique chunk UUID."],
        ["vector_store", "content", "TEXT NOT NULL", "None", "Textual passage chunk for context injection."],
        ["vector_store", "metadata", "JSONB NOT NULL", "GIN Index", "Metadata attributes: userId, documentId, chunkIndex, page."],
        ["vector_store", "embedding", "VECTOR(1536) NOT NULL", "HNSW Index", "Dense float vector for cosine similarity (<=>)."],
        # conversations
        ["conversations", "id", "VARCHAR(255) NOT NULL", "PK", "Conversation UUID string."],
        ["conversations", "user_id", "BIGINT NOT NULL", "FK -> users(id)", "Tenant ownership. ON DELETE CASCADE."],
        ["conversations", "title", "VARCHAR(255) NOT NULL", "None", "Display title shown in user chat sidebar."],
        ["conversations", "created_at", "TIMESTAMP NOT NULL", "None", "Conversation thread creation timestamp."],
        ["conversations", "updated_at", "TIMESTAMP NOT NULL", "B-Tree (DESC)", "Last interaction timestamp for sidebar sorting."],
        # chat_messages
        ["chat_messages", "id", "BIGINT NOT NULL", "PK (Identity)", "Auto-incrementing message ledger ID."],
        ["chat_messages", "conversation_id", "VARCHAR(255) NOT NULL", "FK -> conv(id)", "Parent thread identifier. ON DELETE CASCADE."],
        ["chat_messages", "message_type", "VARCHAR(20) NOT NULL", "None", "USER, ASSISTANT, or SYSTEM."],
        ["chat_messages", "content", "TEXT NOT NULL", "None", "Message text content."],
        ["chat_messages", "created_at", "TIMESTAMP NOT NULL", "B-Tree (ASC)", "Message insertion timestamp."],
        # ai_session
        ["ai_session", "id", "VARCHAR(36) NOT NULL", "PK", "Coordinated session UUID."],
        ["ai_session", "created_at", "TIMESTAMP NOT NULL", "None", "Session start timestamp."],
        ["ai_session", "expires_at", "TIMESTAMP NULL", "B-Tree", "TTL eviction threshold timestamp."],
        ["ai_session", "metadata", "TEXT NULL", "None", "Serialized JSON session configuration."],
        ["ai_session", "event_version", "INT NOT NULL", "None", "Optimistic concurrency locking version counter."],
        # ai_session_event
        ["ai_session_event", "id", "VARCHAR(36) NOT NULL", "PK", "Unique event log identifier."],
        ["ai_session_event", "seq", "INT NOT NULL", "None", "Chronological sequence index within session."],
        ["ai_session_event", "session_id", "VARCHAR(36) NOT NULL", "FK -> ai_sess(id)", "Parent session reference. ON DELETE CASCADE."],
        ["ai_session_event", "timestamp", "TIMESTAMP NOT NULL", "None", "Interaction timestamp."],
        ["ai_session_event", "event_type", "VARCHAR(50) NOT NULL", "None", "MESSAGE, TURN_START, etc."],
        ["ai_session_event", "message_type", "VARCHAR(50) NULL", "None", "Spring AI UserMessage / AssistantMessage tag."],
        ["ai_session_event", "message_content", "TEXT NULL", "None", "Extracted prompt or answer text."],
        ["ai_session_event", "payload", "TEXT NULL", "None", "Serialized execution payload."],
        ["ai_session_event", "synthetic", "BOOLEAN NOT NULL", "None", "True if generated by RAG pipeline."],
        ["ai_session_event", "metadata", "TEXT NULL", "None", "Telemetry and token statistics."],
        # ai_session_index
        ["ai_session_index", "session_id", "VARCHAR(36) NOT NULL", "FK -> ai_sess(id)", "Session identifier."],
        ["ai_session_index", "attr_name", "VARCHAR(100) NOT NULL", "None", "Session attribute key."],
        ["ai_session_index", "attr_value", "VARCHAR(255) NULL", "None", "Session attribute value."]
    ]
    build_table(doc, dict_headers, dict_rows, col_widths=[1.2, 1.2, 1.3, 1.1, 1.7], header_bg=HEX_DARK)

    # ==========================================
    # SECTION 6: PERFORMANCE, INDEXING & OPERATIONAL PLAYBOOK
    # ==========================================
    add_styled_heading(doc, "6. Indexing Strategy & Production Operations Playbook", level=1)
    
    add_paragraph(doc,
        "To achieve sub-20ms P99 query latency under enterprise concurrency, the DocMind database schema incorporates "
        "a rigorously engineered indexing topology across relational, JSONB, and vector storage domains.")

    add_styled_heading(doc, "6.1 Comprehensive Index Catalog", level=2)
    
    idx_headers = ["Index Name", "Target Table", "Indexed Columns & Strategy", "Purpose & Query Acceleration"]
    idx_rows = [
        ["idx_users_username", "users", "username (B-Tree, UNIQUE)", "Accelerates authentication lookup by username in CustomUserDetailsService."],
        ["idx_users_email", "users", "email (B-Tree, UNIQUE)", "Enforces unique registration and fast login lookup."],
        ["idx_doc_metadata_user_id", "document_metadata", "user_id, created_at DESC (B-Tree Composite)", "Powers user document catalog dashboard: WHERE user_id = ? ORDER BY created_at DESC."],
        ["idx_vector_store_embedding", "vector_store", "embedding USING hnsw (vector_cosine_ops)", "Accelerates approximate nearest neighbor vector similarity search (<=>)."],
        ["idx_vector_store_metadata_gin", "vector_store", "metadata USING gin (jsonb_path_ops)", "Accelerates JSONB containment queries: WHERE metadata @> '{\"userId\": \"123\"}'."],
        ["idx_conv_user_updated", "conversations", "user_id, updated_at DESC (B-Tree Composite)", "Optimizes user conversation sidebar: WHERE user_id = ? ORDER BY updated_at DESC."],
        ["idx_chat_msg_conv_created", "chat_messages", "conversation_id, created_at ASC (B-Tree Composite)", "Enables sequential message retrieval for chat history rendering."],
        ["idx_ai_session_event_lookup", "ai_session_event", "session_id, seq ASC (B-Tree Composite)", "Powers Spring AI sliding context window retrieval (last 10 turns)."],
        ["idx_ai_session_expires_at", "ai_session", "expires_at (B-Tree)", "Optimizes scheduled TTL background jobs purging expired AI sessions."]
    ]
    build_table(doc, idx_headers, idx_rows, col_widths=[1.8, 1.3, 1.7, 1.7], header_bg=HEX_PRIMARY)

    add_styled_heading(doc, "6.2 Cascade Deletion & Multi-Store Data Lifecycle", level=2)
    add_paragraph(doc,
        "Managing multi-store data consistency is paramount during document and conversation deletion:", bold_prefix="Deletion Mechanics: ")
        
    add_bullet(doc, "Document Deletion Lifecycle: When a user deletes a document via DELETE /api/v1/documents/{id}:\n"
                    "1. DocumentMetadataService verifies ownership: WHERE id = :id AND user_id = :userId.\n"
                    "2. The service deletes the document_metadata relational record (triggering JPA deletion).\n"
                    "3. An asynchronous event or direct query purges orphaned vector chunks: DELETE FROM vector_store WHERE metadata->>'documentId' = :id.\n"
                    "This guarantees zero dangling vector chunks in high-dimensional vector space.")
    add_bullet(doc, "Conversation Deletion Lifecycle: When a user deletes a conversation:\n"
                    "1. JPA removes conversations, which automatically cascades via ON DELETE CASCADE to chat_messages.\n"
                    "2. The service coordinates cleanup of Spring AI's session table: DELETE FROM ai_session WHERE id = :conversationId.\n"
                    "3. PostgreSQL foreign key constraints on ai_session_event and ai_session_index ensure complete cascading cleanup.")

    add_styled_heading(doc, "6.3 Database Maintenance & DBA Operations", level=2)
    add_bullet(doc, "VACUUM ANALYZE Schedule: Due to high-frequency vector inserts and conversation event journaling, run VACUUM ANALYZE nightly on vector_store and ai_session_event to maintain accurate statistics for the PostgreSQL query planner.")
    add_bullet(doc, "HNSW Re-Indexing Threshold: When the vector dataset expands by >500,000 embeddings, execute REINDEX INDEX CONCURRENTLY idx_vector_store_embedding during off-peak hours to defragment HNSW navigation layers.")
    add_bullet(doc, "Connection Pool Sizing (HikariCP): Set maximum-pool-size = (2 * CPU_CORES) + EFFECTIVE_SPINDLE_COUNT. For a 4-vCPU database instance, configure maximum-pool-size: 10 to prevent connection exhaustion and thread starvation.")

    # ==========================================
    # SECTION 7: ENGINEERING TEAM READINESS
    # ==========================================
    add_styled_heading(doc, "7. Engineering Team Readiness & Implementation Checklist", level=1)
    
    add_paragraph(doc,
        "For the backend engineering team leading development on DocMind, the following checklist ensures strict adherence "
        "to platform architectural standards across future sprints:")

    check_headers = ["Phase / Domain", "Requirement / Task", "Verification Standard", "Status"]
    check_rows = [
        ["Security", "Enforce Non-Bypassed User Isolation", "Every query in DocumentService and RagService must bind user_id.", "COMPLETED & VERIFIED"],
        ["Vector Store", "JSONB Multi-Tenant Metadata Tagging", "All chunks written to vector_store include userId and documentId.", "COMPLETED & VERIFIED"],
        ["AI Memory", "Dual-Memory Synchronization", "Conversations share identical UUID across JPA and Spring AI JDBC.", "COMPLETED & VERIFIED"],
        ["CI / CD", "GitHub Actions Validation", "Java 25 Maven build, automated unit tests, and React build in CI.", "COMPLETED & VERIFIED"],
        ["Database DDL", "Flyway / Liquibase Versioning", "All schema modifications managed through immutable migration scripts.", "RECOMMENDED FOR V2.5"],
        ["Observability", "Vector Latency Telemetry (Micrometer)", "Track P95 and P99 latency on pgvector ANN retrieval.", "PLANNED FOR NEXT SPRINT"]
    ]
    build_table(doc, check_headers, check_rows, col_widths=[1.2, 2.3, 2.0, 1.0], header_bg=HEX_DARK)

    # Save document
    os.makedirs(os.path.dirname(DOC_PATH), exist_ok=True)
    doc.save(DOC_PATH)
    print(f"Enterprise documentation saved successfully to {DOC_PATH}")


if __name__ == '__main__':
    create_document()
