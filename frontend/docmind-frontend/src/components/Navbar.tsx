
import {
  Search,
  ExternalLink,
  Moon,
  Sun,
  Database,
  User,
  LogOut,
  LogIn,
  ShieldCheck
} from 'lucide-react';

import type { Document } from '../types';

interface NavbarProps {
  selectedDoc: Document | null;
  documentCount: number;
  onOpenSearch: () => void;
  darkMode: boolean;
  setDarkMode: (enabled: boolean) => void;
  backendConnected: boolean;
  currentUser: string | null;
  onOpenAuth: () => void;
  onLogout: () => void;
  onOpenAdmin: () => void;
  isAdmin?: boolean;
}

export default function Navbar({
  selectedDoc,
  documentCount,
  onOpenSearch,
  darkMode,
  setDarkMode,
  backendConnected,
  currentUser,
  onOpenAuth,
  onLogout,
  onOpenAdmin,
  isAdmin = false
}: NavbarProps) {
  const selectedDocumentName =
    selectedDoc?.filename ||
    selectedDoc?.fileName ||
    `All Documents (${documentCount})`;

  return (
    <header className="h-16 bg-[#0a0f1d] border-b border-slate-800/80 px-4 flex items-center justify-between select-none z-20">

      {/* Left: Brand & Tech Stack */}
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/20 text-white font-bold">
          <Database className="w-5 h-5 text-white" />
        </div>

        <div>
          <div className="flex items-center gap-2">
            <span className="text-base font-bold text-white tracking-tight">
              DocMind
            </span>

            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full border border-blue-500/30 text-blue-400 bg-blue-500/10 tracking-wide">
              AI RAG
            </span>
          </div>

          <p className="text-[11px] text-slate-400 font-normal hidden sm:block">
            Spring AI • PostgreSQL pgvector • OpenAI
          </p>
        </div>
      </div>

      {/* Center: Scope Pill */}
      <div className="hidden md:flex items-center">
        <div className="bg-[#111a2e] border border-slate-700/60 rounded-full px-3.5 py-1 text-xs text-slate-300 flex items-center gap-2 shadow-inner">
          <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />

          <span className="font-medium text-slate-300">
            Scope:{' '}
            <span className="text-white font-semibold">
              {selectedDocumentName}
            </span>
          </span>
        </div>
      </div>

      {/* Right: Actions & User Session */}
      <div className="flex items-center gap-3">

        {/* Backend Status Indicator */}
        <div
          className={`hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium border ${
            backendConnected
              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
              : 'bg-amber-500/10 text-amber-300 border-amber-500/30'
          }`}
          title={
            backendConnected
              ? 'Connected to backend on port 8081'
              : 'Backend unavailable. Check the Spring Boot service.'
          }
        >
          <span
            className={`w-1.5 h-1.5 rounded-full ${
              backendConnected ? 'bg-emerald-400' : 'bg-amber-400'
            }`}
          />

          {backendConnected ? 'Backend Live' : 'Backend Offline'}
        </div>

        {/* Search Chunks Button */}
        <button
          type="button"
          onClick={onOpenSearch}
          className="flex items-center gap-2 bg-[#131d33] hover:bg-[#1a2642] border border-slate-700/70 hover:border-blue-500/50 rounded-lg px-3 py-1.5 text-xs text-slate-200 transition group shadow-sm"
          title="Search clauses, concepts and facts across vector chunks"
        >
          <Search className="w-3.5 h-3.5 text-slate-400 group-hover:text-blue-400 transition" />

          <span className="font-medium">Search Chunks</span>

          <kbd className="hidden sm:inline-block bg-[#0b1120] text-slate-400 border border-slate-700 text-[10px] px-1.5 py-0.5 rounded font-mono">
            ⌘K
          </kbd>
        </button>

        {/* Swagger API Link */}
        <a
          href="http://localhost:8081/swagger-ui/index.html"
          target="_blank"
          rel="noreferrer"
          className="hidden sm:flex items-center gap-1 text-xs text-slate-400 hover:text-white px-2 py-1.5 rounded-lg hover:bg-slate-800/50 transition font-medium"
          title="Open Swagger OpenAPI Documentation"
        >
          <span>Swagger API</span>
          <ExternalLink className="w-3 h-3 text-slate-400" />
        </a>

        {/* Admin Button */}
        {currentUser && isAdmin && (
          <button
            type="button"
            onClick={onOpenAdmin}
            className="hidden sm:flex items-center gap-1.5 rounded-lg border border-indigo-500/30 bg-indigo-500/10 px-2.5 py-1.5 text-xs font-medium text-indigo-300 transition hover:bg-indigo-500/20"
            title="Open administration"
          >
            <ShieldCheck className="h-3.5 w-3.5" />
            Admin
          </button>
        )}

        {/* User Account / Auth Button */}
        {currentUser ? (
          <div className="flex items-center gap-1.5 bg-[#131d33] border border-slate-700/70 rounded-lg pl-2.5 pr-1.5 py-1 text-xs text-slate-200">
            <User className="w-3.5 h-3.5 text-blue-400" />

            <span className="font-medium max-w-[90px] truncate">
              {currentUser}
            </span>

            <button
              type="button"
              onClick={onLogout}
              className="p-1 hover:text-red-400 text-slate-400 rounded transition"
              title="Sign Out"
              aria-label="Sign out"
            >
              <LogOut className="w-3 h-3" />
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={onOpenAuth}
            className="flex items-center gap-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg px-3 py-1.5 text-xs font-medium transition shadow-sm shadow-blue-600/20"
          >
            <LogIn className="w-3.5 h-3.5" />
            <span>Sign In</span>
          </button>
        )}

        {/* Theme Toggle */}
        <button
          type="button"
          onClick={() => setDarkMode(!darkMode)}
          className="w-8 h-8 rounded-lg bg-[#131d33] hover:bg-[#1a2642] border border-slate-700/70 flex items-center justify-center text-slate-300 hover:text-white transition"
          title={darkMode ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {darkMode ? (
            <Sun className="w-4 h-4 text-amber-400" />
          ) : (
            <Moon className="w-4 h-4 text-slate-300" />
          )}
        </button>
      </div>
    </header>
  );
}
