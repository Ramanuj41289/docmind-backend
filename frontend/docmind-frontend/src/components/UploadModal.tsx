
import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import axios from 'axios';

import {
  UploadCloud,
  X,
  FileText,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Trash2,
  Plus
} from 'lucide-react';

import { docmindApi } from '../api/docmindApi';
import type { Document, ApiErrorResponse } from '../types';

type UploadStatus = 'pending' | 'uploading' | 'indexed' | 'error';

interface UploadResult extends Document {
  message?: string;
}

interface UploadFileItem {
  file: File;
  id: string;
  name: string;
  size: number;
  progress: number;
  status: UploadStatus;
  errorMessage: string | null;
  result?: UploadResult;
}

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialFiles?: File[];
  onUploadSuccess: () => Promise<void> | void;
  showToast: (
    message: string,
    type?: 'success' | 'error' | 'info'
  ) => void;
}

const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      error.message ||
      'Upload failed.'
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Upload failed.';
};

const createUploadItem = (file: File): UploadFileItem => ({
  file,
  id: crypto.randomUUID(),
  name: file.name,
  size: file.size,
  progress: 0,
  status: 'pending',
  errorMessage: null
});

export default function UploadModal({
  isOpen,
  onClose,
  initialFiles = [],
  onUploadSuccess,
  showToast
}: UploadModalProps) {
  const [files, setFiles] = useState<UploadFileItem[]>([]);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [overallProgress, setOverallProgress] = useState<number>(0);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Initialize files if passed from drag-and-drop.
  useEffect(() => {
    if (initialFiles && initialFiles.length > 0) {
      const formatted = initialFiles.map(createUploadItem);
      setFiles(formatted);
    } else if (!isUploading) {
      setFiles([]);
      setOverallProgress(0);
    }
  }, [initialFiles]);

  if (!isOpen) {
    return null;
  }

  const formatSize = (bytes: number): string => {
    if (!bytes) return '0 KB';

    if (bytes >= 1024 * 1024) {
      return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    return `${(bytes / 1024).toFixed(1)} KB`;
  };

  const handleFileSelect = (
    event: ChangeEvent<HTMLInputElement>
  ): void => {
    if (!event.target.files) {
      return;
    }

    const selected = Array.from(event.target.files).map(createUploadItem);

    setFiles((previous) => [...previous, ...selected]);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveFile = (id: string): void => {
    if (isUploading) return;

    setFiles((previous) => previous.filter((file) => file.id !== id));
  };

  const handleClearAll = (): void => {
    if (isUploading) return;

    setFiles([]);
    setOverallProgress(0);
  };

  const startUpload = async (): Promise<void> => {
    if (!files.length || isUploading) {
      return;
    }

    setIsUploading(true);
    setOverallProgress(0);

    let succeeded = 0;

    try {
      if (files.length === 1) {
        const item = files[0];

        setFiles((current) =>
          current.map((file) =>
            file.id === item.id
              ? { ...file, status: 'uploading' }
              : file
          )
        );

        const result = (await docmindApi.uploadSingle(
          item.file,
          (progress: number) => {
            setOverallProgress(progress);

            setFiles((current) =>
              current.map((file) =>
                file.id === item.id
                  ? { ...file, progress }
                  : file
              )
            );
          }
        )) as UploadResult;

        const failed =
          String(result?.status || 'INDEXED').toUpperCase() === 'FAILED';

        succeeded = failed ? 0 : 1;

        setFiles((current) =>
          current.map((file) =>
            file.id === item.id
              ? {
                  ...file,
                  status: failed ? 'error' : 'indexed',
                  progress: 100,
                  result,
                  errorMessage: result?.message || null
                }
              : file
          )
        );
      } else {
        setFiles((current) =>
          current.map((file) => ({
            ...file,
            status: 'uploading'
          }))
        );

        const results = (await docmindApi.uploadMultiple(
          files.map((file) => file.file),
          setOverallProgress
        )) as UploadResult[];

        succeeded = Array.isArray(results)
          ? results.filter(
              (item) =>
                String(item?.status || 'INDEXED').toUpperCase() !==
                'FAILED'
            ).length
          : files.length;

        setFiles((current) =>
          current.map((file, index) => {
            const result = results?.[index];

            const failed =
              String(result?.status || '').toUpperCase() === 'FAILED';

            return {
              ...file,
              status: failed ? 'error' : 'indexed',
              progress: 100,
              result,
              errorMessage: failed
                ? result?.message || 'Document processing failed'
                : null
            };
          })
        );

        setOverallProgress(100);
      }

      const failedCount = files.length - succeeded;

      showToast(
        failedCount
          ? `Uploaded ${succeeded} document(s); ${failedCount} failed.`
          : `Uploaded and indexed ${succeeded} document${
              succeeded === 1 ? '' : 's'
            }.`,
        failedCount ? 'error' : 'success'
      );

      await onUploadSuccess?.();

      window.setTimeout(() => {
        onClose();
        setFiles([]);
        setOverallProgress(0);
      }, 700);
    } catch (error: unknown) {
      const errorMessage = getErrorMessage(error);

      setFiles((current) =>
        current.map((file) =>
          file.status === 'uploading'
            ? {
                ...file,
                status: 'error',
                errorMessage
              }
            : file
        )
      );

      showToast(errorMessage, 'error');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-xl bg-[#0e172a] border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">

        {/* Modal Header */}
        <div className="p-5 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center border border-blue-500/20">
              <UploadCloud className="w-5 h-5" />
            </div>

            <div>
              <h2 className="text-base font-bold text-white">
                Upload & Index Documents
              </h2>

              <p className="text-xs text-slate-400">
                Single or multiple files (PDF, DOCX, TXT, MD, CSV)
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isUploading}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
            aria-label="Close upload modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 overflow-y-auto space-y-4 flex-1">

          {/* Dropzone Area */}
          <div
            onClick={() => fileInputRef.current?.click()}
            className="border-2 border-dashed border-blue-500/40 hover:border-blue-500 bg-[#131d33]/50 hover:bg-[#131d33]/80 rounded-xl p-6 text-center cursor-pointer transition"
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept=".pdf,.docx,.doc,.txt,.md,.csv"
              onChange={handleFileSelect}
              className="hidden"
            />

            <UploadCloud className="w-8 h-8 text-blue-400 mx-auto mb-2" />

            <p className="text-sm font-semibold text-slate-200">
              Click to browse or drop documents here
            </p>

            <p className="text-xs text-slate-400 mt-1">
              Supports single or bulk upload • Automatic chunking & embeddings
            </p>
          </div>

          {/* Files Queue List */}
          {files.length > 0 && (
            <div className="space-y-2.5">
              <div className="flex items-center justify-between text-xs text-slate-400 px-1">
                <span className="font-medium text-slate-300">
                  Selected Files ({files.length})
                </span>

                {!isUploading && (
                  <button
                    type="button"
                    onClick={handleClearAll}
                    className="text-red-400 hover:text-red-300 transition"
                  >
                    Clear All
                  </button>
                )}
              </div>

              {/* Overall Progress Bar */}
              {isUploading && (
                <div className="bg-[#131d33] p-3 rounded-xl border border-slate-800 space-y-1.5">
                  <div className="flex justify-between text-xs font-semibold text-slate-300">
                    <span>Overall Indexing Progress</span>

                    <span className="text-blue-400">
                      {overallProgress}%
                    </span>
                  </div>

                  <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                    <div
                      className="bg-gradient-to-r from-blue-600 to-indigo-500 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${overallProgress}%` }}
                    />
                  </div>
                </div>
              )}

              {/* Individual File Items */}
              <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                {files.map((item) => (
                  <div
                    key={item.id}
                    className="p-3 bg-[#131d33] border border-slate-800/80 rounded-xl space-y-2"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-2.5 min-w-0">
                        <FileText className="w-4 h-4 text-blue-400 flex-shrink-0" />

                        <div className="min-w-0">
                          <p
                            className="text-xs font-medium text-slate-200 truncate"
                            title={item.name}
                          >
                            {item.name}
                          </p>

                          <p className="text-[10px] text-slate-400">
                            {formatSize(item.size)}
                          </p>
                        </div>
                      </div>

                      {/* Status / Remove Button */}
                      <div className="flex items-center gap-2 flex-shrink-0">
                        {item.status === 'pending' && (
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-400">
                            Ready
                          </span>
                        )}

                        {item.status === 'uploading' && (
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 flex items-center gap-1">
                            <Loader2 className="w-2.5 h-2.5 animate-spin" />
                            {item.progress}%
                          </span>
                        )}

                        {item.status === 'indexed' && (
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 flex items-center gap-1 font-medium">
                            <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                            Indexed
                          </span>
                        )}

                        {item.status === 'error' && (
                          <span
                            className="text-[10px] px-2 py-0.5 rounded-full bg-red-500/10 text-red-400 flex items-center gap-1"
                            title={item.errorMessage || 'Upload failed'}
                          >
                            <AlertCircle className="w-3 h-3 text-red-400" />
                            Error
                          </span>
                        )}

                        {!isUploading && item.status !== 'indexed' && (
                          <button
                            type="button"
                            onClick={() => handleRemoveFile(item.id)}
                            className="p-1 text-slate-500 hover:text-red-400 rounded transition"
                            aria-label={`Remove ${item.name}`}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>

                    {/* Individual Progress Bar */}
                    {isUploading && (
                      <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                        <div
                          className="bg-blue-500 h-1.5 rounded-full transition-all duration-200"
                          style={{ width: `${item.progress}%` }}
                        />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-[#0a0f1d] border-t border-slate-800 flex items-center justify-between">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
            className="flex items-center gap-1.5 text-xs text-slate-300 hover:text-white px-3 py-2 rounded-lg border border-slate-700 hover:bg-slate-800 transition"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Files</span>
          </button>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isUploading}
              className="text-xs text-slate-400 hover:text-white px-4 py-2 rounded-lg transition"
            >
              Cancel
            </button>

            <button
              type="button"
              onClick={startUpload}
              disabled={files.length === 0 || isUploading}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 disabled:opacity-40 disabled:hover:bg-blue-600 text-white font-medium text-xs px-5 py-2.5 rounded-xl transition shadow-md shadow-blue-600/30"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-white" />
                  <span>Processing Vectors...</span>
                </>
              ) : (
                <span>Upload & Index ({files.length})</span>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
