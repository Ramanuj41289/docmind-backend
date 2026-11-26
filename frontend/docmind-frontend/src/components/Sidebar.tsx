
import { useState } from 'react';
import type { DragEvent, ChangeEvent, MouseEvent } from 'react';

import {
  Database,
  UploadCloud,
  Files,
  Search,
  FileText,
  CheckCircle2,
  Trash2,
  Eye,
  AlertCircle,
  MessageSquare,
  Plus,
  Trash
} from 'lucide-react';

import type { Document, Conversation } from '../types';

interface SidebarProps {
  documents: Document[];
  selectedDoc: Document | null;
  onSelectDoc: (document: Document | null) => void;
  onOpenUpload: (files?: File[]) => void;
  onInspectDoc: (document: Document) => void;
  onDeleteDoc: (id: number, name: string) => void;
  filterText: string;
  setFilterText: (value: string) => void;
  conversations?: Conversation[];
  activeConversationId?: string | null;
  onNewConversation: () => void;
  onSelectConversation: (conversation: Conversation) => void;
  onDeleteConversation: (conversation: Conversation) => void;
}

export default function Sidebar({
  documents,
  selectedDoc,
  onSelectDoc,
  onOpenUpload,
  onInspectDoc,
  onDeleteDoc,
  filterText,
  setFilterText,
  conversations = [],
  activeConversationId = null,
  onNewConversation,
  onSelectConversation,
  onDeleteConversation
}: SidebarProps) {
  const [dragOver, setDragOver] = useState<boolean>(false);

  // Format bytes to KB or MB.
  const formatSize = (bytes?: number | null): string => {
    if (bytes == null) {
      return 'Size unavailable';
    }

    if (bytes === 0) {
      return '0 KB';
    }

    if (bytes >= 1024 * 1024) {
      return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    return `${(bytes / 1024).toFixed(1)} KB`;
  };

  const filteredDocs = documents.filter((doc) => {
    const name = doc.filename || doc.fileName || '';
    return name.toLowerCase().includes(filterText.toLowerCase());
  });

  const handleDragOver = (event: DragEvent<HTMLDivElement>): void => {
    event.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (): void => {
    setDragOver(false);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>): void => {
    event.preventDefault();
    setDragOver(false);

    if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      onOpenUpload(Array.from(event.dataTransfer.files));
    }
  };

  const handleFilterChange = (
    event: ChangeEvent<HTMLInputElement>
  ): void => {
    setFilterText(event.target.value);
  };

  const handleDocumentAction = (
    event: MouseEvent<HTMLButtonElement>,
    action: () => void
  ): void => {
    event.stopPropagation();
    action();
  };

  return (
    <aside className="w-80 bg-[#070b14] border-r border-slate-800/80 flex flex-col h-[calc(100vh-4rem)] select-none">

      {/* Conversation history */}
      <div className="border-b border-slate-800/70 p-3">
        <div className="flex items-center justify-between mb-2 px-1">
          <div className="flex items-center gap-2">
            <MessageSquare className="w-4 h-4 text-blue-400" />

            <h2 className="text-xs font-semibold tracking-wide text-slate-200">
              Conversations
            </h2>
          </div>

          <button
            type="button"
            onClick={onNewConversation}
            className="flex items-center gap-1 rounded-md bg-blue-600/15 px-2 py-1 text-[10px] font-semibold text-blue-300 hover:bg-blue-600/25"
            title="Start a new conversation"
          >
            <Plus className="h-3 w-3" />
            New
          </button>
        </div>

        <div className="max-h-44 space-y-1 overflow-y-auto">
          {conversations.length === 0 ? (
            <p className="px-2 py-3 text-center text-[10px] text-slate-500">
              Your conversations will appear here
            </p>
          ) : (
            conversations.map((conversation) => (
              <div
                key={conversation.id}
                className={`group flex items-center gap-1 rounded-lg border px-2 py-2 ${
                  activeConversationId === conversation.id
                    ? 'border-blue-500/40 bg-blue-500/10'
                    : 'border-transparent hover:border-slate-700 hover:bg-slate-800/50'
                }`}
              >
                <button
                  type="button"
                  onClick={() => onSelectConversation(conversation)}
                  className="min-w-0 flex-1 text-left"
                >
                  <span className="block truncate text-[11px] font-medium text-slate-200">
                    {conversation.title || 'Untitled conversation'}
                  </span>

                  <span className="block text-[9px] text-slate-500">
                    {conversation.updatedAt
                      ? new Date(conversation.updatedAt).toLocaleString()
                      : ''}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onDeleteConversation(conversation)}
                  className="rounded p-1 text-slate-500 opacity-0 transition hover:bg-red-500/10 hover:text-red-400 group-hover:opacity-100"
                  title="Delete conversation"
                  aria-label="Delete conversation"
                >
                  <Trash className="h-3 w-3" />
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Knowledge Base Header */}
      <div className="p-4 border-b border-slate-800/60">
        <div className="flex items-center gap-2 mb-1">
          <Database className="w-4 h-4 text-blue-500" />

          <h2 className="text-sm font-semibold text-white tracking-wide">
            Knowledge Base
          </h2>
        </div>

        <p className="text-[11px] text-slate-400">
          Indexed vector documents
        </p>
      </div>

      {/* Upload Dropzone */}
      <div className="p-3">
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => onOpenUpload()}
          className={`border-2 border-dashed rounded-xl p-4 text-center cursor-pointer transition-all duration-200 ${
            dragOver
              ? 'border-blue-500 bg-blue-500/10'
              : 'border-blue-500/30 hover:border-blue-500/60 bg-[#0e172a]/60 hover:bg-[#131d33]/60'
          }`}
        >
          <div className="w-9 h-9 mx-auto rounded-full bg-blue-500/10 flex items-center justify-center text-blue-400 mb-2 border border-blue-500/20">
            <UploadCloud className="w-5 h-5" />
          </div>

          <p className="text-xs font-semibold text-slate-200 mb-0.5">
            Upload document or drag here
          </p>

          <p className="text-[10px] text-slate-400">
            PDF, Word, Markdown, TXT, CSV
          </p>
        </div>
      </div>

      {/* All Documents Selector */}
      <div className="px-3 pb-2">
        <button
          type="button"
          onClick={() => onSelectDoc(null)}
          className={`w-full flex items-center justify-between p-2.5 rounded-lg border transition ${
            selectedDoc === null
              ? 'bg-[#131d33] border-blue-500/50 shadow-sm text-white'
              : 'bg-[#0b1120] hover:bg-[#111a2e] border-slate-800/80 text-slate-300'
          }`}
        >
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-md bg-blue-600/20 text-blue-400 flex items-center justify-center">
              <Files className="w-4 h-4" />
            </div>

            <div className="text-left">
              <p className="text-xs font-semibold text-white">
                All Documents
              </p>

              <p className="text-[10px] text-slate-400">
                Search across complete corpus
              </p>
            </div>
          </div>

          <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
            {documents.length}
          </span>
        </button>
      </div>

      {/* Filter Files Input */}
      <div className="px-3 pb-2">
        <div className="relative">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-2.5" />

          <input
            type="text"
            value={filterText}
            onChange={handleFilterChange}
            placeholder="Filter files..."
            className="w-full bg-[#0b1120] border border-slate-800/90 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500/60 focus:ring-1 focus:ring-blue-500/20"
          />
        </div>
      </div>

      {/* Documents / Status */}
      <div className="px-4 py-1.5 flex items-center justify-between text-[10px] font-semibold tracking-wider text-slate-500 uppercase border-y border-slate-800/60 bg-[#070b14]">
        <span>Documents ({filteredDocs.length})</span>
        <span>Status</span>
      </div>

      {/* Document Items */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1.5">
        {filteredDocs.length === 0 ? (
          <div className="text-center py-8 text-slate-500 text-xs">
            {filterText
              ? 'No matching documents'
              : 'No documents uploaded yet'}
          </div>
        ) : (
          filteredDocs.map((doc) => {
            const isSelected = selectedDoc?.id === doc.id;
            const docName = doc.filename || doc.fileName || 'Untitled';

            const chunks =
              typeof doc.totalChunks === 'number'
                ? doc.totalChunks
                : typeof doc.chunksCreated === 'number'
                  ? doc.chunksCreated
                  : 0;

            const pages =
              doc.totalPages != null
                ? String(doc.totalPages)
                : 'N/A';

            const status = String(doc.status || 'UNKNOWN').toUpperCase();

            const isIndexed =
              status === 'INDEXED' || status === 'COMPLETED';

            return (
              <div
                key={doc.id}
                onClick={() => onSelectDoc(doc)}
                className={`group relative p-2.5 rounded-lg border cursor-pointer transition-all ${
                  isSelected
                    ? 'bg-[#131d33] border-blue-500/50 shadow-sm'
                    : 'bg-[#0b1120] hover:bg-[#0f172a] border-slate-800/70 hover:border-slate-700'
                }`}
              >
                <div className="flex items-start gap-2.5">
                  <div className="mt-0.5 text-slate-400 group-hover:text-blue-400 transition">
                    <FileText className="w-4 h-4" />
                  </div>

                  <div className="flex-1 min-w-0">
                    <p
                      className="text-xs font-semibold text-slate-200 group-hover:text-white truncate"
                      title={docName}
                    >
                      {docName}
                    </p>

                    <p className="text-[10px] text-slate-400 mt-0.5">
                      {formatSize(doc.fileSize)} • {pages}p
                    </p>

                    <div className="flex items-center justify-between mt-2 pt-1 border-t border-slate-800/50 text-[10px]">
                      <span className="text-slate-400 font-medium">
                        {chunks} {chunks === 1 ? 'chunk' : 'chunks'}
                      </span>

                      <div
                        className={`flex items-center gap-1 font-medium ${
                          isIndexed
                            ? 'text-emerald-400'
                            : status === 'FAILED'
                              ? 'text-red-400'
                              : 'text-amber-300'
                        }`}
                      >
                        {isIndexed ? (
                          <CheckCircle2 className="w-3 h-3" />
                        ) : (
                          <AlertCircle className="w-3 h-3" />
                        )}

                        <span>{status.replaceAll('_', ' ')}</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Hover Action Buttons */}
                <div className="absolute top-2 right-2 hidden group-hover:flex items-center gap-1 bg-[#131d33] px-1 py-0.5 rounded border border-slate-700/80 shadow-md">
                  <button
                    type="button"
                    onClick={(event) =>
                      handleDocumentAction(event, () => onInspectDoc(doc))
                    }
                    className="p-1 text-slate-400 hover:text-blue-400 hover:bg-slate-700/50 rounded"
                    title="Inspect Vector Chunks"
                    aria-label={`Inspect ${docName}`}
                  >
                    <Eye className="w-3.5 h-3.5" />
                  </button>

                  <button
                    type="button"
                    onClick={(event) =>
                      handleDocumentAction(event, () =>
                        onDeleteDoc(doc.id, docName)
                      )
                    }
                    className="p-1 text-slate-400 hover:text-red-400 hover:bg-slate-700/50 rounded"
                    title="Delete Document & Vectors"
                    aria-label={`Delete ${docName}`}
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </aside>
  );
}
