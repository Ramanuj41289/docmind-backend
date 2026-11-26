# DocMind Frontend

React 19.3.0 + Vite 8.3.8 frontend for the DocMind Spring Boot document RAG backend.

## Requirements

- Node.js 20.19+ or 22.12+
- npm 10+
- DocMind backend running at `http://localhost:8081`
- PostgreSQL/pgvector and the backend's configured embedding/chat provider

## Run locally

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

The `.env.example` leaves `VITE_API_BASE_URL` empty so Vite proxies `/api` to `http://localhost:8081` as configured in `vite.config.js`. For a separately hosted API, set `VITE_API_BASE_URL` to its origin and configure backend CORS accordingly.

## Features

- JWT login and registration. Tokens are attached to protected API requests.
- Document upload (single and multiple), upload progress, list, delete, and document-scoped inspection.
- Semantic chunk search through `POST /api/v1/chat/search/similarity`.
- Streaming and non-streaming RAG chat.
- Persistent conversation history, selecting prior conversations, starting a new chat, and deleting conversations.
- Administrator dashboard for platform statistics and user role management. Backend authorization remains authoritative.
- Markdown chat export.

## Backend API integration

| Feature | Endpoint |
|---|---|
| Register / login | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| Documents | `GET /api/v1/documents` |
| Upload | `POST /api/v1/documents/upload`, `POST /api/v1/documents/upload-multiple` |
| Document detail / delete | `GET /api/v1/documents/{id}`, `DELETE /api/v1/documents/{id}` |
| Chat | `POST /api/v1/chat/query`, `POST /api/v1/chat/stream` |
| Similarity search | `POST /api/v1/chat/search/similarity` |
| Conversation history | `GET /api/v1/chat/conversations` |
| Conversation messages | `GET /api/v1/chat/conversations/{id}/messages` |
| Delete conversation | `DELETE /api/v1/chat/conversations/{id}` |
| Admin users / stats | `GET /api/v1/admin/users`, `GET /api/v1/admin/stats` |
| Update user role | `PATCH /api/v1/admin/users/{userId}/role` |

## Chunk inspection limitation

The backend currently exposes semantic similarity search but not a dedicated paginated endpoint that enumerates every chunk for a document. The document inspection dialog therefore shows real document-filtered matches from the similarity-search API, not a complete enumeration of all stored chunks.

## Streaming notes

The stream endpoint returns answer text as SSE and exposes the conversation ID in the `X-Conversation-Id` response header. The frontend creates a UUID before the first message so subsequent messages use the same persisted conversation. Streaming responses currently do not include citation metadata; the non-streaming `/api/v1/chat/query` response does.
