
import { create } from 'zustand';

import { docmindApi } from '../api/docmindApi';
import type { User } from '../types';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  login: (username: string, password: string) => Promise<{
    token: string;
    user: User;
    username: string;
  }>;

  setUser: (user: User | null) => void;
  syncAuth: () => void;
  logout: () => void;
  handleUnauthorized: () => void;
}

const useAuthStore = create<AuthState>((set) => ({
  user: docmindApi.getCurrentUser(),
  isAuthenticated: Boolean(docmindApi.getAuthToken()),
  isLoading: false,

  login: async (username, password) => {
    set({ isLoading: true });

    try {
      const result = await docmindApi.login(
        username,
        password
      );

      set({
        user: result.user,
        isAuthenticated: true,
        isLoading: false,
      });

      return result;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  setUser: (user) => {
    set({
      user,
      isAuthenticated: Boolean(docmindApi.getAuthToken()),
    });
  },

  syncAuth: () => {
    set({
      user: docmindApi.getCurrentUser(),
      isAuthenticated: Boolean(docmindApi.getAuthToken()),
    });
  },

  logout: () => {
    docmindApi.clearAuthSession();

    set({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  handleUnauthorized: () => {
    docmindApi.clearAuthSession();

    set({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },
}));

export default useAuthStore;
