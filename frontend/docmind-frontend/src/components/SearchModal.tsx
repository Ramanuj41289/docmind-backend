
import { useState } from 'react';
import type { ChangeEvent, KeyboardEvent } from 'react';
import axios from 'axios';

import {
  Search,
  X,
  Sparkles,
  FileText,
  Loader2,
  ArrowRight
} from 'lucide-react';

import { docmindApi } from '../api/docmindApi';

import type {
  Document,
  SearchRequest,
  SearchResponse,
  SearchMatch,
  ApiErrorResponse
} from '../types';

interface SearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  documents: Document[];
  onChatWithChunk: (chunk: SearchMatch) => void;
  showToast?: (
    message: string,
    type?: 'success' | 'error' | 'info'
  ) => void;
}

const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      error.message ||
      'Search failed.'
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Search failed.';
};

export default function SearchModal({
  isOpen,
  onClose,
  documents,
  onChatWithChunk,
  showToast
}: SearchModalProps) {
  const [query, setQuery] = useState<string>('');
  const [selectedDocId, setSelectedDocId] = useState<string>('');
  const [topK, setTopK] = useState<string>('5');

  const [isSearching, setIsSearching] = useState<boolean>(false);
  const [results, setResults] = useState<SearchResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  if (!isOpen) {
    return null;
  }

  const handleSearch = async (): Promise<void> => {
    if (!query.trim() || isSearching) {
      return;
    }

    setIsSearching(true);
    setErrorMessage('');

    try {
      const searchRequest: SearchRequest = {
        query: query.trim(),
        documentId: selectedDocId
          ? Number(selectedDocId)
          : null,
        topK: Number(topK),
        similaritySearch: 0.0
      };

      const data: SearchResponse =
        await docmindApi.searchSimilarity(searchRequest);

      setResults(data);
    } catch (error: unknown) {
      setResults(null);

      const message = getErrorMessage(error);

      setErrorMessage(message);
      showToast?.(message, 'error');
    } finally {
      setIsSearching(false);
    }
  };

  const handleKeyDown = (
    event: KeyboardEvent<HTMLInputElement>
  ): void => {
    if (event.key === 'Enter') {
      event.preventDefault();
      void handleSearch();
    }
  };

  const handleQueryChange = (
    event: ChangeEvent<HTMLInputElement>
  ): void => {
    setQuery(event.target.value);
  };

  const handleDocumentChange = (
    event: ChangeEvent<HTMLSelectElement>
  ): void => {
    setSelectedDocId(event.target.value);
  };

  const handleTopKChange = (
    event: ChangeEvent<HTMLSelectElement>
  ): void => {
    setTopK(event.target.value);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-2xl bg-white dark:bg-[#111827] text-slate-800 dark:text-slate-100 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col max-h-[85vh]">

        {/* Header */}
        <div className="px-6 py-4.5 border-b border-slate-100 dark:border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Search className="w-5 h-5 text-blue-600 dark:text-blue-400" />

            <h2 className="text-base font-bold text-slate-900 dark:text-white">
              Semantic Vector Similarity Search
            </h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-white rounded-lg transition"
            aria-label="Close search modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Input Bar */}
        <div className="p-6 pb-4 border-b border-slate-100 dark:border-slate-800/80 space-y-3">
          <form
            onSubmit={(event) => {
              event.preventDefault();
              void handleSearch();
            }}
            className="flex items-center gap-3"
          >
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-blue-500 absolute left-3.5 top-3.5" />

              <input
                type="text"
                autoFocus
                value={query}
                onChange={handleQueryChange}
                onKeyDown={handleKeyDown}
                placeholder="Search concepts, clauses, or facts across vector chunks..."
                className="w-full pl-10 pr-4 py-2.5 text-sm bg-slate-50 dark:bg-[#0b1120] border border-slate-300 dark:border-slate-700 rounded-xl text-slate-900 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            <button
              type="submit"
              disabled={!query.trim() || isSearching}
              className="bg-blue-600 hover:bg-blue-500 disabled:opacity-40 text-white font-medium text-sm px-6 py-2.5 rounded-xl transition shadow-md shadow-blue-600/20 flex items-center gap-2"
            >
              {isSearching ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Searching</span>
                </>
              ) : (
                <span>Search</span>
              )}
            </button>
          </form>

          {/* Advanced Search Options */}
          <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400 pt-1">
            <div className="flex items-center gap-2">
              <span>Filter Document:</span>

              <select
                value={selectedDocId}
                onChange={handleDocumentChange}
                className="bg-slate-100 dark:bg-[#0b1120] border border-slate-200 dark:border-slate-700 rounded-lg px-2.5 py-1 text-slate-700 dark:text-slate-300 text-xs focus:outline-none"
              >
                <option value="">All Documents</option>

                {documents.map((document) => (
                  <option key={document.id} value={document.id}>
                    {document.filename || document.fileName || 'Untitled'}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span>Top Matches:</span>

              <select
                value={topK}
                onChange={handleTopKChange}
                className="bg-slate-100 dark:bg-[#0b1120] border border-slate-200 dark:border-slate-700 rounded-lg px-2 py-1 text-slate-700 dark:text-slate-300 text-xs focus:outline-none"
              >
                <option value="3">3 chunks</option>
                <option value="5">5 chunks</option>
                <option value="8">8 chunks</option>
                <option value="12">12 chunks</option>
              </select>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="p-6 overflow-y-auto flex-1 min-h-[300px]">

          {errorMessage && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              {errorMessage}
            </div>
          )}

          {/* Initial Empty State */}
          {!results && !isSearching && !errorMessage && (
            <div className="py-16 text-center flex flex-col items-center justify-center">
              <div className="w-12 h-12 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-500 flex items-center justify-center mb-3">
                <Sparkles className="w-6 h-6" />
              </div>

              <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm">
                Type a search term above to inspect matching vector chunks.
              </p>
            </div>
          )}

          {/* Searching Loader */}
          {isSearching && (
            <div className="py-16 text-center flex flex-col items-center justify-center space-y-3">
              <Loader2 className="w-8 h-8 text-blue-500 animate-spin" />

              <p className="text-xs text-slate-400">
                Querying PostgreSQL pgvector with cosine distance metrics...
              </p>
            </div>
          )}

          {/* Results List */}
          {results && !isSearching && (
            <div className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800 pb-2">
                <span>
                  Found{' '}
                  <strong className="text-blue-500">
                    {results.matches?.length || 0}
                  </strong>{' '}
                  matching vector chunks
                </span>

                <span>Query: &quot;{query}&quot;</span>
              </div>

              {results.matches && results.matches.length > 0 ? (
                results.matches.map((item, idx) => (
                  <div
                    key={`${item.documentId}-${item.chunkIndex ?? idx}`}
                    className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-[#0b1120] hover:border-blue-500/50 transition space-y-2.5 shadow-sm"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-blue-500" />

                        <span className="text-xs font-bold text-slate-900 dark:text-white">
                          {item.fileName || 'Document'}
                        </span>

                        {item.chunkIndex !== undefined && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-mono">
                            Chunk #{item.chunkIndex}
                          </span>
                        )}

                        {item.pageNumber !== undefined && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-mono">
                            Page {item.pageNumber}
                          </span>
                        )}
                      </div>

                      {typeof item.similarityScore === 'number' && (
                        <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                          {(item.similarityScore * 100).toFixed(1)}% match
                        </span>
                      )}
                    </div>

                    {/* Snippet */}
                    <div className="text-xs text-slate-700 dark:text-slate-300 font-mono-code bg-white dark:bg-[#070b14] p-3 rounded-lg border border-slate-200 dark:border-slate-800/80 leading-relaxed whitespace-pre-wrap max-h-48 overflow-y-auto">
                      {item.snippet || 'No text preview returned.'}
                    </div>

                    {/* Chat About Chunk */}
                    <div className="flex justify-end pt-1">
                      <button
                        type="button"
                        onClick={() => {
                          onChatWithChunk(item);
                          onClose();
                        }}
                        className="flex items-center gap-1.5 text-xs text-blue-600 dark:text-blue-400 hover:underline font-medium"
                      >
                        <span>Chat about this chunk</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-12 text-center text-slate-500 text-sm">
                  No matching vector chunks found with the current similarity threshold.
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
