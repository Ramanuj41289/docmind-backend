
export type UserRole = 'USER' | 'ADMIN';

export interface User {
  id?: number;
  username: string;
  email?: string;
  role?: UserRole | string;
}

export interface AuthResult {
  token: string;
  user: User;
  username: string;
}

export interface Document {
  id: number;
  filename?: string;
  fileName?: string;
  contentType?: string;
  fileType?: string;
  fileSize?: number;
  size?: number;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  [key: string]: unknown;
}

export type MessageRole = 'user' | 'assistant';

export interface Citation {
  fileName?: string;
  chunkIndex?: number;
  pageNumber?: number;
  [key: string]: unknown;
}

export interface ChatMessage {
  id: string | number;
  role: MessageRole;
  content: string;
  citations?: Citation[];
  createdAt?: string;
  pending?: boolean;
  error?: boolean;
  responseTimeMs?: number;
  evalMetadata?: Record<string, unknown>;
}

export interface Conversation {
  id: string;
  title?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StoredChatMessage {
  id: number;
  messageType: string;
  content: string;
  createdAt: string;
}

export interface ChatRequest {
  question: string;
  documentId: number | null;
  topK?: number;
  minSimilarity?: number;
  conversationId: string | null;
}

export interface ChatResponse {
  answer: string;
  conversationId?: string;
  citations?: Citation[];
  responseTimeMs?: number;
  evalMetadata?: Record<string, unknown>;
}

export interface StreamMetadata {
  conversationId: string | null;
  requestId?: string | null;
}

export interface SearchRequest {
  query: string;
  documentId?: number | null;
  topK?: number;
  similaritySearch?: number;
}

export interface SearchMatch {
  documentId: number;
  fileName?: string;
  chunkIndex?: number;
  pageNumber?: number;
  snippet?: string;
  score?: number;
  [key: string]: unknown;
}

export interface SearchResponse {
  matches: SearchMatch[];
}

export interface AdminStatus {
  totalUsers: number;
  totalAdmins: number;
  regularUsers: number;
}

export interface ApiErrorResponse {
  success?: boolean;
  message?: string;
  detail?: string;
}
