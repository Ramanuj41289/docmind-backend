# DocMind Backend Architecture Diagrams

Below are three Mermaid diagrams that capture the current backend design.  They are based on the Java source that lives under `src/main/java/com/ramanuj/docmind/`.

---

## 1️⃣ DTO Class Diagram

```mermaid
classDiagram
    direction TB
    %% DTOs (records) -------------------------------------------------
    class LoginRequestDto {
        +String username
        +String password
    }
    class LoginResponseDto {
        +String token
        +UserDto user
    }
    class RegisterRequestDto {
        +String username
        +String email
        +String password
    }
    class RegisterResponseDto {
        +String token
        +UserDto user
    }
    class UserDto {
        +String id
        +String username
        +String email
    }
    class DocumentRequestDto {
        +String title
        +String content
    }
    class DocumentResponseDto {
        +String id
        +String title
        +String content
        +String ownerId
    }
    class ConversationDto {
        +String id
        +String title
        +String ownerId
    }
    class ChatMessageDto {
        +String id
        +String conversationId
        +String role
        +String content
        +OffsetDateTime timestamp
    }
    %% Relationships ---------------------------------------------------
    LoginResponseDto --> UserDto : contains
    RegisterResponseDto --> UserDto : contains
    DocumentResponseDto --> UserDto : owner
    ConversationDto --> UserDto : owner
    ChatMessageDto --> ConversationDto : belongsTo
    ChatMessageDto --> UserDto : author
```

---

## 2️⃣ Entity‑Relationship Diagram

```mermaid
erDiagram
    USERS {
        string id PK "UUID"
        string username "unique"
        string email "unique"
        string passwordHash
    }
    DOCUMENT_METADATA {
        string id PK "UUID"
        string title
        string owner_id FK "USERS.id"
    }
    CONVERSATIONS {
        string id PK "UUID"
        string title
        string owner_id FK "USERS.id"
    }
    CHAT_MESSAGES {
        string id PK "UUID"
        string conversation_id FK "CONVERSATIONS.id"
        string author_id FK "USERS.id"
        string role
        string content
        datetime timestamp
    }
    VECTOR_STORE {
        string id PK "UUID"
        string vector_data
    }
    USERS ||--o{ DOCUMENT_METADATA : "creates"
    USERS ||--o{ CONVERSATIONS : "owns"
    USERS ||--o{ CHAT_MESSAGES : "writes"
    CONVERSATIONS ||--o{ CHAT_MESSAGES : "contains"
    DOCUMENT_METADATA ||--|| VECTOR_STORE : "indexed_by"
```

---

## 3️⃣ Controller‑to‑Service Flow Diagram

```mermaid
flowchart LR
    %% Security filter ------------------------------------------------
    subgraph Security
        SecFilter[JwtAuthenticationFilter]
    end
    %% Controllers ----------------------------------------------------
    AuthCtrl[AuthController]
    AdminCtrl[AdminController]
    DocCtrl[DocumentController]
    ConvCtrl[ConversationController]
    ChatCtrl[ChatController]
    %% Services -------------------------------------------------------
    AuthSrv[AuthService]
    AdminSrv[AdminService]
    DocSrv[DocumentService]
    ConvSrv[ConversationService]
    ChatSrv[ChatService]
    RAGSrv[VectorStoreRagService]
    %% Flow -----------------------------------------------------------
    SecFilter --> AuthCtrl
    AuthCtrl --> AuthSrv
    AdminCtrl --> AdminSrv
    DocCtrl --> DocSrv
    ConvCtrl --> ConvSrv
    ChatCtrl --> ChatSrv
    ChatSrv --> RAGSrv
    %% External AI ----------------------------------------------------
    RAGSrv --> OpenAI[OpenAI API]
    %% Data access ----------------------------------------------------
    AuthSrv --> UsersRepo[UserRepository]
    DocSrv --> DocsRepo[DocumentMetadataRepository]
    ConvSrv --> ConvRepo[ConversationRepository]
    ChatSrv --> MsgRepo[ChatMessageRepository]
```

---

*All diagrams use GitHub‑compatible Mermaid syntax and can be viewed directly in the repository’s markdown preview.*

---

## 4) Frontend Architecture Diagram

```mermaid
flowchart LR
    %% Client shell
    Browser[Browser]
    ViteApp[React + TypeScript SPA\nfrontend/docmind-frontend]

    %% UI layer
    subgraph UI[UI Layer]
        App[App.tsx]
        Navbar[Navbar]
        Sidebar[Sidebar]
        ChatArea[ChatArea]
        Modals[Upload/Search/Auth/Admin Modals]
        Toast[Toast Notifications]
    end

    %% State and routing
    subgraph ClientCore[Client Core]
        Router[React Router]
        AuthStore[authStore (Zustand)]
        ChatStore[chatStore (Zustand)]
        DocumentStore[documentStore (Zustand)]
        Types[Shared TS Types]
    end

    %% API layer
    subgraph ApiLayer[API + Transport]
        ApiClient[docmindApi.ts]
        Axios[Axios + JWT Interceptors]
        Stream[Fetch SSE Stream]
        Upload[Multipart Upload + Progress]
    end

    %% Backend
    subgraph Backend[Spring Boot Backend]
        AuthApi[/api/v1/auth/*]
        DocsApi[/api/v1/documents/*]
        ChatApi[/api/v1/chat/query]
        ChatStreamApi[/api/v1/chat/stream]
        SearchApi[/api/v1/chat/search/similarity]
        ConvApi[/api/v1/chat/conversations/*]
        AdminApi[/api/v1/admin/*]
    end

    %% Storage
    subgraph Data[Persistence + AI]
        Pg[(PostgreSQL)]
        Vectors[(pgvector/vector_store)]
        Session[(ai_session + chat_messages)]
    end

    Browser --> ViteApp --> App
    App --> Navbar
    App --> Sidebar
    App --> ChatArea
    App --> Modals
    App --> Toast

    App --> Router
    App --> AuthStore
    App --> ChatStore
    App --> DocumentStore
    App --> Types

    Navbar --> ApiClient
    Sidebar --> ApiClient
    ChatArea --> ApiClient
    Modals --> ApiClient

    ApiClient --> Axios
    ApiClient --> Stream
    ApiClient --> Upload

    Axios --> AuthApi
    Axios --> DocsApi
    Axios --> ChatApi
    Stream --> ChatStreamApi
    Axios --> SearchApi
    Axios --> ConvApi
    Axios --> AdminApi

    AuthApi --> Pg
    DocsApi --> Pg
    ConvApi --> Pg
    ChatApi --> Session
    ChatStreamApi --> Session
    SearchApi --> Vectors
```
