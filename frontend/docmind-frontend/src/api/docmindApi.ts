
import axios, { type AxiosError } from 'axios';

import type {
  AdminStatus,
  AuthResult,
  ChatRequest,
  ChatResponse,
  Conversation,
  Document as DocMindDocument,
  SearchMatch,
  SearchRequest,
  SearchResponse,
  StoredChatMessage,
  StreamMetadata,
  User,
  UserRole,
} from '../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 120000,
});

const createRequestId = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  return `req-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

type UnauthorizedCallback = (() => void) | null;

let onUnauthorizedCallback: UnauthorizedCallback = null;

export const setOnUnauthorizedCallback = (
  callback: UnauthorizedCallback
): void => {
  onUnauthorizedCallback = callback;
};

// ------------------------------------------------------------
// Authentication helpers
// ------------------------------------------------------------

const AUTH_TOKEN_KEY = 'docmind_jwt_token';
const USERNAME_KEY = 'docmind_username';
const USER_KEY = 'docmind_user';

const getAuthToken = (): string | null =>
  localStorage.getItem(AUTH_TOKEN_KEY);

const getCurrentUsername = (): string | null =>
  localStorage.getItem(USERNAME_KEY);

const getCurrentUser = (): User | null => {
  try {
    return JSON.parse(
      localStorage.getItem(USER_KEY) || 'null'
    ) as User | null;
  } catch {
    return null;
  }
};

const setAuthSession = (token: string, user: User): void => {
  if (!token) {
    throw new Error(
      'The login response did not contain an access token.'
    );
  }

  localStorage.setItem(AUTH_TOKEN_KEY, token);
  localStorage.setItem(USERNAME_KEY, user.username || '');
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

const clearAuthSession = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(USER_KEY);
};

// ------------------------------------------------------------
// Axios interceptors
// ------------------------------------------------------------

api.interceptors.request.use((config) => {
  const url = config.url || '';

  const isAuthRequest =
    url.includes('/api/v1/auth/login') ||
    url.includes('/api/v1/auth/register');

  if (isAuthRequest) {
    config.headers.delete('Authorization');
  } else {
    const token = getAuthToken();

    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
  }

  config.headers.set('X-Request-Id', createRequestId());

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const url = error.config?.url || '';

    const isAuthRequest =
      url.includes('/auth/login') ||
      url.includes('/auth/register');

    if (error.response?.status === 401 && !isAuthRequest) {
      onUnauthorizedCallback?.();
    }

    return Promise.reject(error);
  }
);

// ------------------------------------------------------------
// Response utilities
// ------------------------------------------------------------

interface ApiEnvelope<T> {
  success?: boolean;
  message?: string;
  data?: T;
}

const unwrap = <T>(response: { data: unknown }): T => {
  const body = response.data as ApiEnvelope<T> | T;

  if (
    typeof body === 'object' &&
    body !== null &&
    'success' in body &&
    body.success === false
  ) {
    const message =
      'message' in body && typeof body.message === 'string'
        ? body.message
        : 'The request failed.';

    throw new Error(message);
  }

  if (
    typeof body === 'object' &&
    body !== null &&
    'data' in body
  ) {
    return body.data as T;
  }

  return body as T;
};

export const getApiErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data as
      | { message?: string; detail?: string }
      | undefined;

    return (
      responseData?.message ||
      responseData?.detail ||
      error.message ||
      'Something went wrong.'
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Something went wrong.';
};

// ------------------------------------------------------------
// API methods
// ------------------------------------------------------------

export const docmindApi = {

  // Authentication

  getAuthToken,

  getCurrentUsername,

  getCurrentUser,

  setAuthSession,

  clearAuthSession,

  async register(
    username: string,
    email: string,
    password: string
  ): Promise<unknown> {
    const response = await api.post(
      '/api/v1/auth/register',
      { username, email, password }
    );

    return unwrap<unknown>(response);
  },

  async login(
    username: string,
    password: string
  ): Promise<AuthResult> {
    const response = await api.post(
      '/api/v1/auth/login',
      { username, password }
    );

    const payload = response.data as {
      accessToken?: string;
      token?: string;
      user?: User;
      data?: {
        accessToken?: string;
        token?: string;
        user?: User;
      };
      message?: string;
    };

    const token =
      payload?.accessToken ||
      payload?.token ||
      payload?.data?.accessToken ||
      payload?.data?.token;

    const user =
      payload?.user ||
      payload?.data?.user ||
      { username };

    if (!token) {
      throw new Error(
        payload?.message ||
        'Login response did not contain an access token.'
      );
    }

    setAuthSession(token, user);

    return {
      token,
      user,
      username: user.username || username,
    };
  },

  // Health check

  async checkHealth(): Promise<boolean> {
    try {
      await api.get('/api/v1/documents', {
        timeout: 5000,
      });

      return true;
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        return [401, 403].includes(
          error.response?.status ?? 0
        );
      }

      return false;
    }
  },

  // Document management

  async getDocuments(): Promise<DocMindDocument[]> {
    return (
      unwrap<DocMindDocument[]>(
        await api.get('/api/v1/documents')
      ) || []
    );
  },

  async getDocument(
    id: number
  ): Promise<DocMindDocument> {
    return unwrap<DocMindDocument>(
      await api.get(`/api/v1/documents/${id}`)
    );
  },

  async uploadSingle(
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<unknown> {
    const form = new FormData();
    form.append('file', file);

    const response = await api.post(
      '/api/v1/documents/upload',
      form,
      {
        onUploadProgress: (event) => {
          if (event.total) {
            onProgress?.(
              Math.round(
                (event.loaded / event.total) * 100
              )
            );
          }
        },
      }
    );

    return unwrap<unknown>(response);
  },

  async uploadMultiple(
    files: File[],
    onProgress?: (progress: number) => void
  ): Promise<DocMindDocument[]> {
    const form = new FormData();

    files.forEach((file) => {
      form.append('files', file);
    });

    const response = await api.post(
      '/api/v1/documents/upload-multiple',
      form,
      {
        onUploadProgress: (event) => {
          if (event.total) {
            onProgress?.(
              Math.round(
                (event.loaded / event.total) * 100
              )
            );
          }
        },
      }
    );

    return unwrap<DocMindDocument[]>(response) || [];
  },

  async deleteDocument(id: number): Promise<unknown> {
    return unwrap<unknown>(
      await api.delete(`/api/v1/documents/${id}`)
    );
  },

  // Semantic search

  async searchSimilarity({
    query,
    documentId,
    topK = 5,
    similaritySearch = 0,
  }: SearchRequest): Promise<SearchResponse> {
    const response = await api.post(
      '/api/v1/chat/search/similarity',
      {
        query,
        documentId: documentId || null,
        topK: Number(topK),
        similaritySearch: Number(similaritySearch),
      }
    );

    return unwrap<SearchResponse>(response);
  },

  async getDocumentChunks(
    document: DocMindDocument
  ): Promise<SearchMatch[]> {
    const result = await this.searchSimilarity({
      query:
        document.filename ||
        document.fileName ||
        'document content',
      documentId: document.id,
      topK: 12,
      similaritySearch: 0,
    });

    return result?.matches || [];
  },

  // Conversation history

  async getConversations(): Promise<Conversation[]> {
    return (
      unwrap<Conversation[]>(
        await api.get('/api/v1/chat/conversations')
      ) || []
    );
  },

  async getConversationMessages(
    conversationId: string
  ): Promise<StoredChatMessage[]> {
    return (
      unwrap<StoredChatMessage[]>(
        await api.get(
          `/api/v1/chat/conversations/${conversationId}/messages`
        )
      ) || []
    );
  },

  async deleteConversation(
    conversationId: string
  ): Promise<unknown> {
    return unwrap<unknown>(
      await api.delete(
        `/api/v1/chat/conversations/${conversationId}`
      )
    );
  },

  // Administration

  async getAdminStats(): Promise<AdminStatus> {
    return unwrap<AdminStatus>(
      await api.get('/api/v1/admin/status')
    );
  },

  async getAdminUsers(): Promise<User[]> {
    return (
      unwrap<User[]>(
        await api.get('/api/v1/admin/users')
      ) || []
    );
  },

  async updateUserRole(
    userId: number,
    role: UserRole,
    approvalConfirmed?: boolean
  ): Promise<User> {
    return unwrap<User>(
      await api.patch(
        `/api/v1/admin/users/${userId}/role`,
        {
          role,
          approvalConfirmed:
            approvalConfirmed === undefined
              ? null
              : approvalConfirmed,
        }
      )
    );
  },

  // Non-streaming chat

  async askQuestion({
    question,
    documentId,
    topK = 5,
    minSimilarity = 0,
    conversationId,
  }: ChatRequest): Promise<ChatResponse> {
    const response = await api.post(
      '/api/v1/chat/query',
      {
        question,
        documentId: documentId || null,
        topK,
        minSimilarity,
        conversationId: conversationId || null,
      }
    );

    return unwrap<ChatResponse>(response);
  },

  // Streaming chat

  async streamQuestion(
    {
      question,
      documentId,
      topK = 5,
      minSimilarity = 0,
      conversationId,
    }: ChatRequest,
    onChunk: (token: string) => void,
    onDone?: (metadata: StreamMetadata) => void
  ): Promise<StreamMetadata> {
    const baseURL =
      import.meta.env.VITE_API_BASE_URL || '';

    const authToken = getAuthToken();
    const requestId = createRequestId();

    const response = await fetch(
      `${baseURL}/api/v1/chat/stream`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(authToken
            ? { Authorization: `Bearer ${authToken}` }
            : {}),
          'X-Request-Id': requestId,
        },
        body: JSON.stringify({
          question,
          documentId: documentId || null,
          topK,
          minSimilarity,
          conversationId: conversationId || null,
        }),
      }
    );

    if (!response.ok) {
      let message =
        `Chat request failed (${response.status}).`;

      try {
        const body = await response.json() as {
          message?: string;
        };

        message = body.message || message;
      } catch {
        // Response was not JSON.
      }

      if (response.status === 401) {
        clearAuthSession();
        onUnauthorizedCallback?.();
      }

      throw new Error(message);
    }

    const nextConversationId =
      response.headers.get('X-Conversation-Id') ||
      conversationId ||
      null;

    if (!response.body) {
      throw new Error(
        'The browser did not receive a streaming response body.'
      );
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    let buffer = '';
    let done = false;

    const processLine = (rawLine: string): void => {
      const line = rawLine.endsWith('\r')
        ? rawLine.slice(0, -1)
        : rawLine;

      if (!line || line.startsWith(':')) {
        return;
      }

      if (line.startsWith('data:')) {
        const data = line.slice(5);

        if (data === '[DONE]') {
          return;
        }

        // Preserve newline tokens sent as empty SSE data events.
        onChunk(data.length === 0 ? '\n' : data);
      }
    };

    while (!done) {
      const part = await reader.read();

      done = part.done ?? false;

      buffer += decoder.decode(
        part.value || new Uint8Array(),
        { stream: !done }
      );

      const lines = buffer.split('\n');

      // Preserve incomplete lines for the next read.
      buffer = lines.pop() ?? '';

      for (const line of lines) {
        processLine(line);
      }
    }

    if (buffer.length > 0) {
      processLine(buffer);
    }

    const metadata: StreamMetadata = {
      conversationId: nextConversationId,
      requestId:
        response.headers.get('X-Request-Id') || requestId,
    };

    onDone?.(metadata);

    return metadata;
  },
};