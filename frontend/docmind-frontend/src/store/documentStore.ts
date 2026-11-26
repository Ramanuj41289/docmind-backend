
import { create } from 'zustand';

import { docmindApi } from '../api/docmindApi';
import type { Document } from '../types';

interface DocumentState {
  documents: Document[];
  selectedDoc: Document | null;
  isLoading: boolean;

  fetchDocuments: () => Promise<Document[]>;
  selectDocument: (document: Document | null) => void;
  deleteDocument: (id: number) => Promise<void>;
  clearDocuments: () => void;
}

const useDocumentStore = create<DocumentState>((set, get) => ({
  documents: [],
  selectedDoc: null,
  isLoading: false,

  fetchDocuments: async () => {
    if (!docmindApi.getAuthToken()) {
      set({
        documents: [],
        selectedDoc: null,
        isLoading: false,
      });
      return [];
    }

    set({ isLoading: true });

    try {
      const result = await docmindApi.getDocuments();
      const documents: Document[] = Array.isArray(result)
        ? result
        : [];

      set({
        documents,
        isLoading: false,
      });

      const selectedDoc = get().selectedDoc;

      if (selectedDoc) {
        const updatedSelectedDoc = documents.find(
          (document) => document.id === selectedDoc.id
        );

        set({
          selectedDoc: updatedSelectedDoc ?? null,
        });
      }

      return documents;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  selectDocument: (document) => {
    set({ selectedDoc: document });
  },

  deleteDocument: async (id) => {
    await docmindApi.deleteDocument(id);

    set((state) => ({
      documents: state.documents.filter(
        (document) => document.id !== id
      ),
      selectedDoc:
        state.selectedDoc?.id === id
          ? null
          : state.selectedDoc,
    }));
  },

  clearDocuments: () => {
    set({
      documents: [],
      selectedDoc: null,
      isLoading: false,
    });
  },
}));

export default useDocumentStore;
