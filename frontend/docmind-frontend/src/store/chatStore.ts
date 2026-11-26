
import { create } from 'zustand';

import { docmindApi } from '../api/docmindApi';
import type {
  ChatMessage,
  Conversation,
  StoredChatMessage,
} from '../types';

interface ChatState {
  messages: ChatMessage[];
  conversations: Conversation[];
  conversationId: string | null;
  isLoading: boolean;

  fetchConversations: () => Promise<Conversation[]>;
  loadConversation: (conversationId: string) => Promise<ChatMessage[]>;
  deleteConversation: (conversationId: string) => Promise<void>;
  startNewConversation: () => void;
  addMessages: (messages: ChatMessage | ChatMessage[]) => void;
  updateMessage: (
    messageId: string | number,
    updates: Partial<ChatMessage>
  ) => void;
  appendToken: (messageId: string | number, token: string) => void;
  setMessages: (messages: ChatMessage[]) => void;
  setConversationId: (conversationId: string | null) => void;
  setConversations: (conversations: Conversation[]) => void;
  setIsLoading: (isLoading: boolean) => void;
  clearChat: () => void;
}

const useChatStore = create<ChatState>((set) => ({
  messages: [],
  conversations: [],
  conversationId: null,
  isLoading: false,

  fetchConversations: async () => {
    if (!docmindApi.getAuthToken()) {
      set({ conversations: [] });
      return [];
    }

    const result = await docmindApi.getConversations();
    const conversations: Conversation[] = Array.isArray(result)
      ? result
      : [];

    set({ conversations });
    return conversations;
  },

  loadConversation: async (conversationId) => {
    const stored: StoredChatMessage[] =
      await docmindApi.getConversationMessages(conversationId);

    const messages: ChatMessage[] = stored.map((message) => ({
      id: message.id,
      role:
        String(message.messageType).toUpperCase() === 'USER'
          ? 'user'
          : 'assistant',
      content: message.content || '',
      citations: [],
      createdAt: message.createdAt,
    }));

    set({ conversationId, messages });
    return messages;
  },

  deleteConversation: async (conversationId) => {
    await docmindApi.deleteConversation(conversationId);

    set((state) => ({
      conversations: state.conversations.filter(
        (conversation) => conversation.id !== conversationId
      ),
      messages:
        state.conversationId === conversationId
          ? []
          : state.messages,
      conversationId:
        state.conversationId === conversationId
          ? null
          : state.conversationId,
    }));
  },

  startNewConversation: () => {
    set({
      messages: [],
      conversationId: null,
    });
  },

  addMessages: (newMessages) => {
    set((state) => ({
      messages: [
        ...state.messages,
        ...(Array.isArray(newMessages)
          ? newMessages
          : [newMessages]),
      ],
    }));
  },

  updateMessage: (messageId, updates) => {
    set((state) => ({
      messages: state.messages.map((message) =>
        message.id === messageId
          ? { ...message, ...updates }
          : message
      ),
    }));
  },

  appendToken: (messageId, token) => {
    set((state) => ({
      messages: state.messages.map((message) =>
        message.id === messageId
          ? { ...message, content: message.content + token }
          : message
      ),
    }));
  },

  setMessages: (messages) => set({ messages }),

  setConversationId: (conversationId) => set({ conversationId }),

  setConversations: (conversations) => set({ conversations }),

  setIsLoading: (isLoading) => set({ isLoading }),

  clearChat: () => {
    set({
      messages: [],
      conversations: [],
      conversationId: null,
      isLoading: false,
    });
  },
}));

export default useChatStore;
