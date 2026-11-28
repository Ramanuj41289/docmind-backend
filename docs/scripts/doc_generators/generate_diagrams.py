"""
DocMind Database Architecture Diagram Generator
Generates high-resolution, publication-grade ER diagrams in both PNG and JPEG formats.
"""

from PIL import Image, ImageDraw, ImageFont
import os

SCHEMA_DIR = 'docs/diagrams/schemas'
os.makedirs(SCHEMA_DIR, exist_ok=True)

FONT_REG_PATH = 'C:/Windows/Fonts/segoeui.ttf'
FONT_BOLD_PATH = 'C:/Windows/Fonts/segoeuib.ttf'
FONT_MONO_PATH = 'C:/Windows/Fonts/consola.ttf'

font_title = ImageFont.truetype(FONT_BOLD_PATH, 28)
font_subtitle = ImageFont.truetype(FONT_REG_PATH, 16)
font_table_hdr = ImageFont.truetype(FONT_BOLD_PATH, 18)
font_table_cat = ImageFont.truetype(FONT_REG_PATH, 12)
font_col_name = ImageFont.truetype(FONT_BOLD_PATH, 14)
font_col_type = ImageFont.truetype(FONT_MONO_PATH, 13)
font_badge = ImageFont.truetype(FONT_BOLD_PATH, 11)
font_legend = ImageFont.truetype(FONT_REG_PATH, 13)


def save_image_formats(img, base_name):
    png_path = os.path.join(SCHEMA_DIR, f"{base_name}.png")
    jpg_path = os.path.join(SCHEMA_DIR, f"{base_name}.jpg")
    img.save(png_path, quality=95)
    rgb_img = img.convert('RGB')
    rgb_img.save(jpg_path, 'JPEG', quality=95)
    print(f"Saved {base_name}.png and {base_name}.jpg")


def draw_table_card(draw, x, y, width, title, category, cols, header_bg, category_bg):
    row_height = 28
    header_height = 46
    card_height = header_height + len(cols) * row_height + 8
    
    # Shadow
    draw.rounded_rectangle([x + 3, y + 3, x + width + 3, y + card_height + 3], radius=8, fill=(215, 222, 232))
    # Main Box
    draw.rounded_rectangle([x, y, x + width, y + card_height], radius=8, fill=(255, 255, 255), outline=(180, 195, 210), width=2)
    # Header
    draw.rounded_rectangle([x, y, x + width, y + header_height], radius=8, fill=header_bg)
    draw.rectangle([x, y + header_height - 8, x + width, y + header_height], fill=header_bg)
    draw.line([x, y + header_height, x + width, y + header_height], fill=(160, 175, 195), width=2)
    
    # Header text
    draw.text((x + 14, y + 8), title, font=font_table_hdr, fill=(255, 255, 255))
    
    # Category pill
    cat_bbox = font_table_cat.getbbox(category)
    cat_w = cat_bbox[2] - cat_bbox[0] + 16
    cat_x = x + width - cat_w - 12
    draw.rounded_rectangle([cat_x, y + 12, cat_x + cat_w, y + 34], radius=6, fill=category_bg)
    draw.text((cat_x + 8, y + 15), category, font=font_table_cat, fill=(255, 255, 255))
    
    # Columns
    cur_y = y + header_height + 4
    for i, col in enumerate(cols):
        if i % 2 == 1:
            draw.rectangle([x + 2, cur_y, x + width - 2, cur_y + row_height], fill=(248, 250, 252))
            
        badge = col.get('badge')
        name = col.get('name')
        col_type = col.get('type')
        
        bx = x + 12
        if badge:
            badge_color = (234, 88, 12) if badge == 'PK' else (37, 99, 235) if badge == 'FK' else (16, 185, 129) if badge == 'UK' else (100, 116, 139)
            draw.rounded_rectangle([bx, cur_y + 4, bx + 26, cur_y + 22], radius=4, fill=badge_color)
            draw.text((bx + 4, cur_y + 5), badge, font=font_badge, fill=(255, 255, 255))
        else:
            draw.ellipse([bx + 8, cur_y + 10, bx + 14, cur_y + 16], fill=(148, 163, 184))
            
        nx = bx + 34
        draw.text((nx, cur_y + 4), name, font=font_col_name, fill=(15, 23, 42))
        
        tx = x + width - 12 - (font_col_type.getbbox(col_type)[2] - font_col_type.getbbox(col_type)[0])
        draw.text((tx, cur_y + 5), col_type, font=font_col_type, fill=(71, 85, 105))
        
        cur_y += row_height
        
    return {'x': x, 'y': y, 'width': width, 'height': card_height}


def draw_orthogonal_link(draw, x1, y1, x2, y2, color=(59, 130, 246), label="", style="solid"):
    mid_x = (x1 + x2) // 2
    if style == "dashed":
        points = [(x1, y1), (mid_x, y1), (mid_x, y2), (x2, y2)]
        for i in range(len(points) - 1):
            p1, p2 = points[i], points[i+1]
            draw.line([p1, p2], fill=color, width=2)
    else:
        draw.line([(x1, y1), (mid_x, y1)], fill=color, width=2)
        draw.line([(mid_x, y1), (mid_x, y2)], fill=color, width=2)
        draw.line([(mid_x, y2), (x2, y2)], fill=color, width=2)
        
    draw.ellipse([x1 - 4, y1 - 4, x1 + 4, y1 + 4], fill=color)
    draw.ellipse([x2 - 4, y2 - 4, x2 + 4, y2 + 4], fill=color)
    
    if label:
        lx = mid_x + 6
        ly = (y1 + y2) // 2 - 8
        bbox = font_badge.getbbox(label)
        lw = bbox[2] - bbox[0] + 10
        draw.rounded_rectangle([lx, ly, lx + lw, ly + 18], radius=4, fill=(241, 245, 249), outline=color, width=1)
        draw.text((lx + 5, ly + 2), label, font=font_badge, fill=color)


def generate_master_erd():
    img_w, img_h = 2400, 1600
    img = Image.new('RGB', (img_w, img_h), (248, 250, 252))
    draw = ImageDraw.Draw(img)
    
    draw.rectangle([0, 0, img_w, 90], fill=(15, 23, 42))
    draw.text((40, 18), "DocMind — Master Relational & Vector Database Architecture (ERD)", font=font_title, fill=(255, 255, 255))
    draw.text((40, 56), "Comprehensive Multi-Tenant Schema: Core Security, Ingestion, Conversational RAG & Spring AI Session Memory", font=font_subtitle, fill=(148, 163, 184))
    
    # 1. users
    users_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'BIGINT IDENTITY'},
        {'badge': 'UK', 'name': 'username', 'type': 'VARCHAR(50)'},
        {'badge': 'UK', 'name': 'email', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'password', 'type': 'VARCHAR(255) [bcrypt]'},
        {'badge': '', 'name': 'role', 'type': 'VARCHAR(20) [USER|ADMIN]'}
    ]
    u_box = draw_table_card(draw, 980, 140, 440, "users", "Core Identity", users_cols, (30, 58, 138), (59, 130, 246))
    
    # 2. document_metadata
    doc_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'UUID'},
        {'badge': 'FK', 'name': 'user_id', 'type': 'BIGINT -> users(id)'},
        {'badge': '', 'name': 'filename', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'content_type', 'type': 'VARCHAR(100)'},
        {'badge': '', 'name': 'file_size', 'type': 'BIGINT (bytes)'},
        {'badge': '', 'name': 'total_pages', 'type': 'INT'},
        {'badge': '', 'name': 'total_chunks', 'type': 'INT'},
        {'badge': '', 'name': 'status', 'type': 'VARCHAR(20) [ENUM]'},
        {'badge': '', 'name': 'error_message', 'type': 'VARCHAR(1000)'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP (UTC)'},
        {'badge': '', 'name': 'updated_at', 'type': 'TIMESTAMP (UTC)'}
    ]
    d_box = draw_table_card(draw, 140, 140, 460, "document_metadata", "Document Ingestion", doc_cols, (6, 95, 70), (16, 185, 129))
    
    # 3. vector_store
    vec_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'UUID'},
        {'badge': '', 'name': 'content', 'type': 'TEXT (chunk)'},
        {'badge': '', 'name': 'metadata', 'type': 'JSONB (userId, docId)'},
        {'badge': 'IX', 'name': 'embedding', 'type': 'VECTOR(1536) [HNSW]'}
    ]
    v_box = draw_table_card(draw, 140, 600, 460, "vector_store", "pgvector Extension", vec_cols, (15, 118, 110), (20, 184, 166))
    
    # 4. conversations & chat_messages
    conv_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(255) [UUID]'},
        {'badge': 'FK', 'name': 'user_id', 'type': 'BIGINT -> users(id)'},
        {'badge': '', 'name': 'title', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP (UTC)'},
        {'badge': '', 'name': 'updated_at', 'type': 'TIMESTAMP (UTC)'}
    ]
    c_box = draw_table_card(draw, 980, 480, 440, "conversations", "Chat Management", conv_cols, (194, 65, 12), (249, 115, 22))
    
    msg_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'BIGINT IDENTITY'},
        {'badge': 'FK', 'name': 'conversation_id', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'message_type', 'type': 'VARCHAR(20) [USER|AI]'},
        {'badge': '', 'name': 'content', 'type': 'TEXT'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP (UTC)'}
    ]
    m_box = draw_table_card(draw, 980, 800, 440, "chat_messages", "Chat Audit History", msg_cols, (154, 52, 18), (234, 88, 12))
    
    # 5. Spring AI Session Memory
    ai_sess_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(36)'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'expires_at', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'metadata', 'type': 'TEXT'},
        {'badge': '', 'name': 'event_version', 'type': 'INT'}
    ]
    ais_box = draw_table_card(draw, 1750, 140, 460, "ai_session", "Spring AI Session", ai_sess_cols, (91, 33, 182), (139, 92, 246))
    
    ai_event_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(36)'},
        {'badge': '', 'name': 'seq', 'type': 'INT'},
        {'badge': 'FK', 'name': 'session_id', 'type': 'VARCHAR(36) -> ai_session'},
        {'badge': '', 'name': 'timestamp', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'event_type', 'type': 'VARCHAR(50)'},
        {'badge': '', 'name': 'message_type', 'type': 'VARCHAR(50)'},
        {'badge': '', 'name': 'message_content', 'type': 'TEXT'},
        {'badge': '', 'name': 'payload', 'type': 'TEXT'},
        {'badge': '', 'name': 'synthetic', 'type': 'BOOLEAN'},
        {'badge': '', 'name': 'metadata', 'type': 'TEXT'}
    ]
    aie_box = draw_table_card(draw, 1750, 430, 460, "ai_session_event", "Spring AI Events", ai_event_cols, (109, 40, 217), (167, 139, 250))
    
    ai_index_cols = [
        {'badge': 'FK', 'name': 'session_id', 'type': 'VARCHAR(36) -> ai_session'},
        {'badge': '', 'name': 'attr_name', 'type': 'VARCHAR(100)'},
        {'badge': '', 'name': 'attr_value', 'type': 'VARCHAR(255)'}
    ]
    aii_box = draw_table_card(draw, 1750, 920, 460, "ai_session_index", "Spring AI Session Index", ai_index_cols, (126, 34, 206), (192, 132, 252))
    
    # Connecting Lines
    draw_orthogonal_link(draw, u_box['x'], u_box['y'] + 60, d_box['x'] + d_box['width'], d_box['y'] + 90, color=(16, 185, 129), label="1 : N (owns)")
    draw_orthogonal_link(draw, d_box['x'] + 230, d_box['y'] + d_box['height'], v_box['x'] + 230, v_box['y'], color=(20, 184, 166), label="1 : N (chunks)", style="dashed")
    draw_orthogonal_link(draw, u_box['x'] + 220, u_box['y'] + u_box['height'], c_box['x'] + 220, c_box['y'], color=(249, 115, 22), label="1 : N (owns)")
    draw_orthogonal_link(draw, c_box['x'] + 220, c_box['y'] + c_box['height'], m_box['x'] + 220, m_box['y'], color=(234, 88, 12), label="1 : N (cascade)")
    draw_orthogonal_link(draw, ais_box['x'] + 230, ais_box['y'] + ais_box['height'], aie_box['x'] + 230, aie_box['y'], color=(139, 92, 246), label="1 : N")
    draw_orthogonal_link(draw, ais_box['x'], ais_box['y'] + 100, aii_box['x'], aii_box['y'] + 60, color=(168, 85, 247), label="1 : N")
    draw_orthogonal_link(draw, c_box['x'] + c_box['width'], c_box['y'] + 70, ais_box['x'], ais_box['y'] + 70, color=(99, 102, 241), label="Coordinated UUID", style="dashed")
    
    # Legend
    draw.rectangle([40, 1480, img_w - 40, 1560], fill=(255, 255, 255), outline=(203, 213, 225), width=2)
    draw.text((60, 1505), "Architecture Legend:", font=font_col_name, fill=(15, 23, 42))
    
    draw.rounded_rectangle([220, 1502, 250, 1524], radius=4, fill=(234, 88, 12))
    draw.text((226, 1504), "PK", font=font_badge, fill=(255, 255, 255))
    draw.text((258, 1505), "Primary Key", font=font_legend, fill=(51, 65, 85))
    
    draw.rounded_rectangle([360, 1502, 390, 1524], radius=4, fill=(37, 99, 235))
    draw.text((366, 1504), "FK", font=font_badge, fill=(255, 255, 255))
    draw.text((398, 1505), "Foreign Key", font=font_legend, fill=(51, 65, 85))
    
    draw.rounded_rectangle([500, 1502, 530, 1524], radius=4, fill=(16, 185, 129))
    draw.text((506, 1504), "UK", font=font_badge, fill=(255, 255, 255))
    draw.text((538, 1505), "Unique Key", font=font_legend, fill=(51, 65, 85))
    
    draw.line([660, 1515, 710, 1515], fill=(59, 130, 246), width=2)
    draw.text((720, 1505), "Direct Relational FK", font=font_legend, fill=(51, 65, 85))
    
    draw.line([880, 1515, 930, 1515], fill=(20, 184, 166), width=2)
    draw.text((940, 1505), "Semantic JSONB / Coordinated Session Link", font=font_legend, fill=(51, 65, 85))
    
    save_image_formats(img, "01_master_database_erd")


def generate_user_document_erd():
    img_w, img_h = 1600, 1000
    img = Image.new('RGB', (img_w, img_h), (248, 250, 252))
    draw = ImageDraw.Draw(img)
    
    draw.rectangle([0, 0, img_w, 80], fill=(6, 95, 70))
    draw.text((40, 16), "DocMind — Document Ingestion & Vector Storage Architecture", font=font_title, fill=(255, 255, 255))
    draw.text((40, 50), "Multi-Tenant Isolation: User Ownership + Document Metadata + pgvector Embeddings", font=font_subtitle, fill=(167, 243, 208))
    
    users_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'BIGINT IDENTITY'},
        {'badge': 'UK', 'name': 'username', 'type': 'VARCHAR(50)'},
        {'badge': 'UK', 'name': 'email', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'role', 'type': 'VARCHAR(20)'}
    ]
    u_box = draw_table_card(draw, 80, 180, 380, "users", "Tenant / Identity", users_cols, (30, 58, 138), (59, 130, 246))
    
    doc_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'UUID'},
        {'badge': 'FK', 'name': 'user_id', 'type': 'BIGINT -> users(id)'},
        {'badge': '', 'name': 'filename', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'content_type', 'type': 'VARCHAR(100)'},
        {'badge': '', 'name': 'file_size', 'type': 'BIGINT'},
        {'badge': '', 'name': 'total_pages', 'type': 'INT'},
        {'badge': '', 'name': 'total_chunks', 'type': 'INT'},
        {'badge': '', 'name': 'status', 'type': 'VARCHAR(20) [INDEXED]'},
        {'badge': '', 'name': 'error_message', 'type': 'VARCHAR(1000)'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'updated_at', 'type': 'TIMESTAMP'}
    ]
    d_box = draw_table_card(draw, 560, 180, 460, "document_metadata", "Document Catalog", doc_cols, (6, 95, 70), (16, 185, 129))
    
    vec_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'UUID'},
        {'badge': '', 'name': 'content', 'type': 'TEXT (Document chunk text)'},
        {'badge': 'IX', 'name': 'metadata', 'type': 'JSONB {"userId", "documentId", ...}'},
        {'badge': 'IX', 'name': 'embedding', 'type': 'VECTOR(1536) [Cosine / HNSW]'}
    ]
    v_box = draw_table_card(draw, 1100, 180, 440, "vector_store", "pgvector Store", vec_cols, (15, 118, 110), (20, 184, 166))
    
    draw_orthogonal_link(draw, u_box['x'] + u_box['width'], u_box['y'] + 60, d_box['x'], d_box['y'] + 60, color=(16, 185, 129), label="1 : N")
    draw_orthogonal_link(draw, d_box['x'] + d_box['width'], d_box['y'] + 60, v_box['x'], v_box['y'] + 60, color=(20, 184, 166), label="1 : N (JSONB)", style="dashed")
    
    draw.rounded_rectangle([80, 680, img_w - 80, 940], radius=8, fill=(255, 255, 255), outline=(203, 213, 225), width=2)
    draw.text((110, 700), "Ingestion & Security Isolation Principles:", font=font_table_hdr, fill=(15, 23, 42))
    rules = [
        "1. Every document record is tied to users.id with a strict non-null foreign key constraint (users ||--o{ document_metadata).",
        "2. Document chunks written to vector_store embed both userId and documentId directly in the JSONB metadata column.",
        "3. Vector searches execute with composite filtering: filter.and(filter.eq('userId', user.getId()), filter.eq('documentId', docId)).",
        "4. This dual-layer architecture guarantees absolute multi-tenant isolation across both relational SQL and vector spaces."
    ]
    for idx, rule in enumerate(rules):
        draw.text((120, 740 + idx * 30), rule, font=font_subtitle, fill=(51, 65, 85))
        
    save_image_formats(img, "02_user_and_document_erd")


def generate_chat_and_session_erd():
    img_w, img_h = 1800, 1100
    img = Image.new('RGB', (img_w, img_h), (248, 250, 252))
    draw = ImageDraw.Draw(img)
    
    draw.rectangle([0, 0, img_w, 80], fill=(194, 65, 12))
    draw.text((40, 16), "DocMind — Conversational Memory & Spring AI Session Architecture", font=font_title, fill=(255, 255, 255))
    draw.text((40, 50), "Dual-Layer Memory: Relational UI History (JPA) + Low-Latency Context Window (Spring AI Session JDBC)", font=font_subtitle, fill=(254, 215, 170))
    
    users_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'BIGINT IDENTITY'},
        {'badge': 'UK', 'name': 'username', 'type': 'VARCHAR(50)'},
        {'badge': 'UK', 'name': 'email', 'type': 'VARCHAR(255)'}
    ]
    u_box = draw_table_card(draw, 60, 160, 360, "users", "Core Identity", users_cols, (30, 58, 138), (59, 130, 246))
    
    conv_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(255) [UUID]'},
        {'badge': 'FK', 'name': 'user_id', 'type': 'BIGINT -> users(id)'},
        {'badge': '', 'name': 'title', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'updated_at', 'type': 'TIMESTAMP'}
    ]
    c_box = draw_table_card(draw, 480, 160, 420, "conversations", "Chat Management", conv_cols, (194, 65, 12), (249, 115, 22))
    
    msg_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'BIGINT IDENTITY'},
        {'badge': 'FK', 'name': 'conversation_id', 'type': 'VARCHAR(255)'},
        {'badge': '', 'name': 'message_type', 'type': 'VARCHAR(20) [USER|AI]'},
        {'badge': '', 'name': 'content', 'type': 'TEXT'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP'}
    ]
    m_box = draw_table_card(draw, 480, 480, 420, "chat_messages", "Chat Audit History", msg_cols, (154, 52, 18), (234, 88, 12))
    
    ais_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(36) [Shared UUID]'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'expires_at', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'metadata', 'type': 'TEXT'},
        {'badge': '', 'name': 'event_version', 'type': 'INT'}
    ]
    ais_box = draw_table_card(draw, 1060, 160, 440, "ai_session", "Spring AI Session", ais_cols, (91, 33, 182), (139, 92, 246))
    
    aie_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(36)'},
        {'badge': '', 'name': 'seq', 'type': 'INT'},
        {'badge': 'FK', 'name': 'session_id', 'type': 'VARCHAR(36) -> ai_session'},
        {'badge': '', 'name': 'timestamp', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'event_type', 'type': 'VARCHAR(50)'},
        {'badge': '', 'name': 'message_type', 'type': 'VARCHAR(50)'},
        {'badge': '', 'name': 'message_content', 'type': 'TEXT'},
        {'badge': '', 'name': 'payload', 'type': 'TEXT'}
    ]
    aie_box = draw_table_card(draw, 1060, 480, 440, "ai_session_event", "Spring AI Events", aie_cols, (109, 40, 217), (167, 139, 250))
    
    draw_orthogonal_link(draw, u_box['x'] + u_box['width'], u_box['y'] + 60, c_box['x'], c_box['y'] + 60, color=(249, 115, 22), label="1 : N")
    draw_orthogonal_link(draw, c_box['x'] + 210, c_box['y'] + c_box['height'], m_box['x'] + 210, m_box['y'], color=(234, 88, 12), label="1 : N (cascade)")
    draw_orthogonal_link(draw, c_box['x'] + c_box['width'], c_box['y'] + 60, ais_box['x'], ais_box['y'] + 60, color=(99, 102, 241), label="Coordinated ID", style="dashed")
    draw_orthogonal_link(draw, ais_box['x'] + 220, ais_box['y'] + ais_box['height'], aie_box['x'] + 220, aie_box['y'], color=(139, 92, 246), label="1 : N")
    
    draw.rounded_rectangle([60, 840, img_w - 60, 1040], radius=8, fill=(255, 255, 255), outline=(203, 213, 225), width=2)
    draw.text((90, 860), "Dual Memory Strategy Architectural Rationale:", font=font_table_hdr, fill=(15, 23, 42))
    notes = [
        "Layer 1 (conversations & chat_messages): Full persistent message archive for UI rendering, user audit trail, and conversation listing.",
        "Layer 2 (ai_session & ai_session_event): High-speed Spring AI Session Memory advisor table used by LLM Prompting with sliding window (last 10 turns).",
        "Synchronization: Both layers share the identical conversationId UUID. Cleanup cascades across both stores on delete."
    ]
    for idx, note in enumerate(notes):
        draw.text((100, 900 + idx * 30), note, font=font_subtitle, fill=(51, 65, 85))
        
    save_image_formats(img, "03_chat_and_session_erd")


def generate_spring_ai_session_erd():
    img_w, img_h = 1600, 900
    img = Image.new('RGB', (img_w, img_h), (248, 250, 252))
    draw = ImageDraw.Draw(img)
    
    draw.rectangle([0, 0, img_w, 80], fill=(91, 33, 182))
    draw.text((40, 16), "DocMind — Spring AI JDBC Session Repository Schema", font=font_title, fill=(255, 255, 255))
    draw.text((40, 50), "Stateful Conversational Event Bus: Session Metadata, Event Journaling & Dynamic Attributes", font=font_subtitle, fill=(216, 180, 254))
    
    ais_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(36)'},
        {'badge': '', 'name': 'created_at', 'type': 'TIMESTAMP (Creation time)'},
        {'badge': '', 'name': 'expires_at', 'type': 'TIMESTAMP (TTL / Expiry)'},
        {'badge': '', 'name': 'metadata', 'type': 'TEXT (JSON Session Config)'},
        {'badge': '', 'name': 'event_version', 'type': 'INT (Optimistic Locking)'}
    ]
    ais_box = draw_table_card(draw, 560, 150, 480, "ai_session", "Session Root", ais_cols, (91, 33, 182), (139, 92, 246))
    
    aie_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'VARCHAR(36)'},
        {'badge': '', 'name': 'seq', 'type': 'INT (Ordering sequence)'},
        {'badge': 'FK', 'name': 'session_id', 'type': 'VARCHAR(36) -> ai_session'},
        {'badge': '', 'name': 'timestamp', 'type': 'TIMESTAMP'},
        {'badge': '', 'name': 'event_type', 'type': 'VARCHAR(50) [MESSAGE, TURN]'},
        {'badge': '', 'name': 'message_type', 'type': 'VARCHAR(50) [USER, ASSISTANT]'},
        {'badge': '', 'name': 'message_content', 'type': 'TEXT'},
        {'badge': '', 'name': 'payload', 'type': 'TEXT (Serialized Turn State)'},
        {'badge': '', 'name': 'synthetic', 'type': 'BOOLEAN'},
        {'badge': '', 'name': 'metadata', 'type': 'TEXT'}
    ]
    aie_box = draw_table_card(draw, 80, 460, 480, "ai_session_event", "Event Journal", aie_cols, (109, 40, 217), (167, 139, 250))
    
    aii_cols = [
        {'badge': 'FK', 'name': 'session_id', 'type': 'VARCHAR(36) -> ai_session'},
        {'badge': '', 'name': 'attr_name', 'type': 'VARCHAR(100) (Index key)'},
        {'badge': '', 'name': 'attr_value', 'type': 'VARCHAR(255) (Index value)'}
    ]
    aii_box = draw_table_card(draw, 1040, 460, 480, "ai_session_index", "Session Indexing", aii_cols, (126, 34, 206), (192, 132, 252))
    
    draw_orthogonal_link(draw, ais_box['x'] + 120, ais_box['y'] + ais_box['height'], aie_box['x'] + 240, aie_box['y'], color=(139, 92, 246), label="1 : N")
    draw_orthogonal_link(draw, ais_box['x'] + 360, ais_box['y'] + ais_box['height'], aii_box['x'] + 240, aii_box['y'], color=(168, 85, 247), label="1 : N")
    
    save_image_formats(img, "04_spring_ai_session_erd")


def generate_vector_store_erd():
    img_w, img_h = 1600, 950
    img = Image.new('RGB', (img_w, img_h), (248, 250, 252))
    draw = ImageDraw.Draw(img)
    
    draw.rectangle([0, 0, img_w, 80], fill=(15, 118, 110))
    draw.text((40, 16), "DocMind — pgvector Vector Store Architecture & Indexing", font=font_title, fill=(255, 255, 255))
    draw.text((40, 50), "High-Dimensional Vector Space: HNSW Cosine Indexing with Multi-Tenant JSONB Scoping", font=font_subtitle, fill=(204, 251, 241))
    
    vec_cols = [
        {'badge': 'PK', 'name': 'id', 'type': 'UUID (Chunk identifier)'},
        {'badge': '', 'name': 'content', 'type': 'TEXT (Parsed document chunk passage)'},
        {'badge': 'IX', 'name': 'metadata', 'type': 'JSONB (userId, documentId, fileName, chunkIndex...)'},
        {'badge': 'IX', 'name': 'embedding', 'type': 'VECTOR(1536) [OpenAI text-embedding-3-small]'}
    ]
    v_box = draw_table_card(draw, 80, 160, 680, "vector_store", "pgvector Public Table", vec_cols, (15, 118, 110), (20, 184, 166))
    
    # Metadata breakdown card
    meta_cols = [
        {'badge': 'KEY', 'name': 'userId', 'type': 'VARCHAR (Tenant user boundary for strict isolation)'},
        {'badge': 'KEY', 'name': 'documentId', 'type': 'VARCHAR (Parent document UUID reference)'},
        {'badge': 'KEY', 'name': 'fileName', 'type': 'VARCHAR (Original uploaded document filename)'},
        {'badge': 'KEY', 'name': 'contentType', 'type': 'VARCHAR (e.g. application/pdf, text/plain)'},
        {'badge': 'KEY', 'name': 'chunkIndex', 'type': 'INT (0-based chunk order inside document)'},
        {'badge': 'KEY', 'name': 'pageNumber', 'type': 'INT (1-based physical page number if applicable)'},
        {'badge': 'KEY', 'name': 'distance', 'type': 'FLOAT (Cosine distance calculated at search time)'}
    ]
    m_box = draw_table_card(draw, 840, 160, 680, "JSONB Metadata Attributes", "Document Context", meta_cols, (13, 148, 136), (45, 212, 191))
    
    draw_orthogonal_link(draw, v_box['x'] + v_box['width'], v_box['y'] + 120, m_box['x'], m_box['y'] + 120, color=(20, 184, 166), label="JSONB Keys", style="dashed")
    
    # Bottom Architecture Specs
    draw.rounded_rectangle([80, 520, img_w - 80, 890], radius=8, fill=(255, 255, 255), outline=(203, 213, 225), width=2)
    draw.text((110, 545), "pgvector Production Specifications:", font=font_table_hdr, fill=(15, 23, 42))
    specs = [
        "Index Algorithm: HNSW (Hierarchical Navigable Small World) with m=16, ef_construction=64.",
        "Distance Metric: Cosine Distance (<=> operator). Similarity score = 1.0 - (embedding <=> query_vector).",
        "Dimensions: 1536 floating-point values generated by OpenAI text-embedding-3-small.",
        "Query Routing: Composite filter expression in SQL: WHERE metadata->>'userId' = ? [AND metadata->>'documentId' = ?].",
        "Chunk Deletion Policy: DELETE FROM vector_store WHERE metadata->>'documentId' = ? triggered upon document deletion."
    ]
    for idx, spec in enumerate(specs):
        draw.text((120, 585 + idx * 30), spec, font=font_subtitle, fill=(51, 65, 85))
        
    save_image_formats(img, "05_vector_store_architecture")


if __name__ == '__main__':
    generate_master_erd()
    generate_user_document_erd()
    generate_chat_and_session_erd()
    generate_spring_ai_session_erd()
    generate_vector_store_erd()
    print("All diagrams generated successfully.")
