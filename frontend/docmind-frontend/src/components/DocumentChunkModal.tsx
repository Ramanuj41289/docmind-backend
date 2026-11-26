
import { useEffect, useState } from 'react';
import type { Document as ReactDocument } from '../types';
import {
  FileText,
  X,
  Copy,
  Check,
  Trash2,
  MessageSquare,
  Layers,
  Calendar,
  Files,
  HardDrive,
  Loader2
} from 'lucide-react';
import { docmindApi } from '../api/docmindApi';
import type { ApiErrorResponse } from '../types';
import axios from 'axios';

interface DocumentChunk {
  chunkIndex?: number;
  index?: number;
  snippet?: string;
  content?: string;
}

interface DocumentChunkModalProps {
  isOpen: boolean;
  onClose: () => void;
  document: ReactDocument | null;
  onDeleteDocument: (id: number, name: string) => void;
  onChatWithDoc: (document: ReactDocument) => void;
  showToast?: (
    message: string,
    type?: 'success' | 'error' | 'info'
  ) => void;
}

export default function DocumentChunkModal({
  isOpen,
  onClose,
  document,
  onDeleteDocument,
  onChatWithDoc,
  showToast
}: DocumentChunkModalProps) {
  const [copiedId, setCopiedId] = useState<boolean>(false);
  const [chunks, setChunks] = useState<DocumentChunk[]>([]);
  const [isLoadingChunks, setIsLoadingChunks] = useState<boolean>(false);
  const [chunkError, setChunkError] = useState<string>('');

  useEffect(() => {
    let active = true;

    if (document) {
      setIsLoadingChunks(true);
      setChunkError('');

      docmindApi
        .getDocumentChunks(document)
        .then((data) => {
          if (active) {
            setChunks((data || []) as DocumentChunk[]);
          }
        })
        .catch((error: unknown) => {
          if (active) {
            setChunks([]);

            const message = axios.isAxiosError<ApiErrorResponse>(error)
              ? error.response?.data?.message
              : error instanceof Error
                ? error.message
                : undefined;

            setChunkError(
              message || 'Unable to retrieve matching chunks.'
            );
          }
        })
        .finally(() => {
          if (active) {
            setIsLoadingChunks(false);
          }
        });
    }

    return () => {
      active = false;
    };
  }, [document]);

  if (!isOpen || !document) {
    return null;
  }

  const docName =
    document.filename || document.fileName || 'Untitled Document';

  const docId = document.id
    ? String(document.id)
    : 'unknown-id';

  const truncatedId =
    docId.length > 18
      ? `${docId.substring(0, 18)}...`
      : docId;

  const formatSize = (bytes?: number | null): string => {
    if (bytes == null) return 'N/A';
    if (bytes === 0) return '0 KB';

    if (bytes >= 1024 * 1024) {
      return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    return `${(bytes / 1024).toFixed(1)} KB`;
  };

  const formatDate = (dateStr?: string): string => {
    if (!dateStr) return 'Not available';

    try {
      const date = new Date(dateStr);

      if (Number.isNaN(date.getTime())) {
        return 'Not available';
      }

      return date.toLocaleDateString('en-GB');
    } catch {
      return 'Not available';
    }
  };

  const handleCopyId = async (): Promise<void> => {
    try {
      await navigator.clipboard.writeText(docId);
      setCopiedId(true);

      showToast?.('Document ID copied to clipboard', 'info');

      window.setTimeout(() => {
        setCopiedId(false);
      }, 2000);
    } catch {
      showToast?.('Unable to copy document ID', 'error');
    }
  };

  // Convert unknown metadata values into renderable strings.
  const totalChunks = String(
    document.totalChunks ?? document.chunksCreated ?? 'N/A'
  );

  const totalPages = String(document.totalPages ?? 'N/A');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-3xl bg-white text-slate-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh] border border-slate-200">

        {/* Header */}
        <div className="p-6 pb-5 flex items-start justify-between border-b border-slate-100">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0 border border-blue-100">
              <FileText className="w-6 h-6" />
            </div>

            <div>
              <h2 className="text-xl font-bold text-slate-900 tracking-tight">
                {docName}
              </h2>

              <div className="flex items-center gap-2 mt-1 text-xs text-slate-500 font-mono">
                <span>{truncatedId}</span>

                <button
                  type="button"
                  onClick={handleCopyId}
                  className="p-0.5 hover:text-slate-800 transition"
                  title="Copy Document UUID"
                  aria-label="Copy document ID"
                >
                  {copiedId ? (
                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>

                <span className="text-slate-300">•</span>

                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                  {String(document.status || 'UNKNOWN').replaceAll('_', ' ')}
                </span>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">

          {/* Metadata Stat Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3.5">

            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50/70">
              <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium mb-1">
                <HardDrive className="w-4 h-4 text-slate-400" />
                <span>File Size</span>
              </div>

              <p className="text-base font-bold text-slate-900">
                {formatSize(document.fileSize)}
              </p>
            </div>

            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50/70">
              <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium mb-1">
                <Layers className="w-4 h-4 text-slate-400" />
                <span>Vector Chunks</span>
              </div>

              <p className="text-base font-bold text-slate-900">
                {totalChunks} Chunks
              </p>
            </div>

            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50/70">
              <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium mb-1">
                <Files className="w-4 h-4 text-slate-400" />
                <span>Total Pages</span>
              </div>

              <p className="text-base font-bold text-slate-900">
                {totalPages} Pages
              </p>
            </div>

            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50/70">
              <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium mb-1">
                <Calendar className="w-4 h-4 text-slate-400" />
                <span>Uploaded</span>
              </div>

              <p className="text-base font-bold text-slate-900">
                {formatDate(document.createdAt)}
              </p>
            </div>
          </div>

          {/* Chunk Section Header */}
          <div className="flex items-center justify-between text-xs font-bold tracking-wider text-slate-600 uppercase pt-2">
            <span>Retrieved Vector Chunk Previews</span>

            <span className="text-[11px] font-semibold text-slate-400 tracking-normal">
              Semantic search matches
            </span>
          </div>

          {/* Chunk Cards */}
          {isLoadingChunks ? (
            <div className="py-12 text-center flex flex-col items-center justify-center space-y-2">
              <Loader2 className="w-6 h-6 text-blue-500 animate-spin" />

              <p className="text-xs text-slate-400">
                Loading vector chunks from store...
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {chunkError && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-700">
                  {chunkError}
                </div>
              )}

              {!chunkError && chunks.length === 0 && (
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">
                  No matching chunk previews were returned for this document.
                </div>
              )}

              {chunks.map((chunk, idx) => (
                <div
                  key={idx}
                  className="p-4 rounded-2xl border border-slate-200 bg-slate-50/60 space-y-2 hover:border-slate-300 transition"
                >
                  <p className="text-xs font-bold text-slate-800">
                    Chunk #{chunk.chunkIndex ?? chunk.index ?? idx}
                  </p>

                  <div className="font-mono text-xs text-slate-700 bg-white p-3.5 rounded-xl border border-slate-200 leading-relaxed whitespace-pre-wrap max-h-48 overflow-y-auto">
                    {chunk.snippet || chunk.content || 'No text preview returned.'}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-5 bg-slate-50 border-t border-slate-200 flex items-center justify-between">

          <button
            type="button"
            onClick={() => {
              if (
                window.confirm(
                  `Are you sure you want to delete "${docName}" and purge its vectors from pgvector?`
                )
              ) {
                onDeleteDocument(document.id, docName);
                onClose();
              }
            }}
            className="flex items-center gap-1.5 text-xs font-semibold text-red-600 hover:text-red-700 px-3 py-2 rounded-xl hover:bg-red-50 transition"
          >
            <Trash2 className="w-4 h-4" />
            <span>Delete Document</span>
          </button>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="text-xs font-semibold text-slate-700 hover:text-slate-900 px-4 py-2.5 rounded-xl border border-slate-300 hover:bg-slate-100 transition"
            >
              Close
            </button>

            <button
              type="button"
              onClick={() => {
                onChatWithDoc(document);
                onClose();
              }}
              className="flex items-center gap-2 bg-blue-700 hover:bg-blue-600 text-white text-xs font-semibold px-5 py-2.5 rounded-xl transition shadow-md shadow-blue-700/20"
            >
              <MessageSquare className="w-4 h-4" />
              <span>Chat with Document</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
