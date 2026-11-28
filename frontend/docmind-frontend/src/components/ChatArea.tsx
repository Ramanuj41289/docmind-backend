
import React, {
    useState,
    useRef,
    useEffect,
    type ChangeEvent,
    type KeyboardEvent,
    type ReactNode
} from 'react';

import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

import {
    Sparkles,
    FileText,
    HelpCircle,
    CheckSquare,
    Cpu,
    Send,
    Bot,
    User,
    Copy,
    Check,
    RotateCcw,
    Download,
    Zap,
    ChevronDown,
    ChevronUp,
    BookOpen,
    Loader2
} from 'lucide-react';

import { FEATURE_TEMPLATES } from '../constants/templates';
import type { ChatMessage, Document, Citation } from '../types';

interface ChatCitation extends Citation {
    fileName?: string;
    chunkIndex?: number;
    pageNumber?: number;
    similarityScore?: number;
    snippet?: string;
}

interface ChatAreaMessage extends Omit<ChatMessage, 'citations'> {
    citations?: ChatCitation[];
}

const toNumber = (value: unknown): number | null => {
    return typeof value === 'number' && Number.isFinite(value)
        ? value
        : null;
};

const toBoolean = (value: unknown): boolean | null => {
    return typeof value === 'boolean' ? value : null;
};

interface ChatAreaProps {
    messages: ChatAreaMessage[];
    onSendMessage: (message: string) => void;
    selectedDoc: Document | null;
    streamTokens: boolean;
    setStreamTokens: (enabled: boolean) => void;
    onClearChat: () => void;
    onExportChat: () => void;
    isLoading: boolean;
}

interface FeatureTemplate {
    id: string | number;
    icon: string;
    title: string;
    description: string;
    prompt: string;
}

export default function ChatArea({
    messages,
    onSendMessage,
    selectedDoc,
    streamTokens,
    setStreamTokens,
    onClearChat,
    onExportChat,
    isLoading,
}: ChatAreaProps) {

    const [inputText, setInputText] = useState<string>('');
    const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

    const [expandedCitations, setExpandedCitations] =
        useState<Record<string, boolean>>({});

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    // Auto-scroll to the latest message
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({
            behavior: 'smooth',
            block: 'end',
        });
    }, [messages, isLoading]);

    // Dynamically adjust textarea height
    const handleInputChange = (
        e: ChangeEvent<HTMLTextAreaElement>
    ): void => {
        setInputText(e.target.value);

        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            textareaRef.current.style.height =
                `${Math.min(textareaRef.current.scrollHeight, 160)}px`;
        }
    };

    // Send message on Enter
    const handleKeyDown = (
        e: KeyboardEvent<HTMLTextAreaElement>
    ): void => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleSend = (): void => {
        if (!inputText.trim() || isLoading) return;

        onSendMessage(inputText.trim());
        setInputText('');

        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
        }
    };

    // Handle feature template selection
    const handleTemplateClick = (template: FeatureTemplate): void => {
        onSendMessage(template.prompt);
    };

    // Copy AI response
    const handleCopy = async (
        text: string,
        index: number
    ): Promise<void> => {
        try {
            await navigator.clipboard.writeText(text);
            setCopiedIndex(index);

            window.setTimeout(() => {
                setCopiedIndex(null);
            }, 2000);

        } catch (error) {
            console.error('Failed to copy response:', error);
        }
    };

    // Expand or collapse citation
    const toggleCitation = (
        msgIndex: number,
        citIndex: number
    ): void => {
        const key = `${msgIndex}-${citIndex}`;

        setExpandedCitations((prev) => ({
            ...prev,
            [key]: !prev[key],
        }));
    };

    // Feature template icons
    const getTemplateIcon = (iconName: string): ReactNode => {
        switch (iconName) {
            case 'FileText':
                return <FileText className="w-5 h-5 text-blue-400" />;

            case 'HelpCircle':
                return <HelpCircle className="w-5 h-5 text-blue-400" />;

            case 'CheckSquare':
                return <CheckSquare className="w-5 h-5 text-blue-400" />;

            case 'Cpu':
                return <Cpu className="w-5 h-5 text-blue-400" />;

            default:
                return <FileText className="w-5 h-5 text-blue-400" />;
        }
    };

    return (
        <main className="flex-1 min-w-0 flex flex-col bg-[#0b1120] h-[calc(100vh-4rem)] relative overflow-hidden">

            {/* Top Chat Bar */}
            <div className="min-h-12 border-b border-slate-800/80 px-4 md:px-6 flex items-center justify-between bg-[#0b1120]/80 backdrop-blur-sm z-10 gap-3">

                {/* Context Indicator */}
                <div className="flex items-center gap-2 text-xs min-w-0">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 flex-shrink-0" />

                    <span className="text-slate-400 flex-shrink-0">
                        Context:
                    </span>

                    <span className="text-slate-200 font-semibold truncate max-w-md">
                        {selectedDoc
                            ? selectedDoc.filename || selectedDoc.fileName
                            : 'All Uploaded Documents'}
                    </span>
                </div>

                {/* Right Controls */}
                <div className="flex items-center gap-3 md:gap-4 flex-shrink-0">

                    {/* Stream Tokens */}
                    <label className="flex items-center gap-2 cursor-pointer text-xs select-none text-slate-300">
                        <span className="text-slate-400 hidden sm:inline">
                            Stream Tokens
                        </span>

                        <div className="relative inline-flex items-center">
                            <input
                                type="checkbox"
                                checked={streamTokens}
                                onChange={(e) => setStreamTokens(e.target.checked)}
                                className="sr-only peer"
                            />

                            <div className="w-9 h-5 bg-slate-800 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600" />
                        </div>
                    </label>

                    {/* Export */}
                    <button
                        onClick={onExportChat}
                        disabled={messages.length === 0}
                        className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition"
                        title="Export conversation history"
                    >
                        <Download className="w-3.5 h-3.5" />
                        <span className="hidden sm:inline">Export</span>
                    </button>

                    {/* Clear */}
                    <button
                        onClick={onClearChat}
                        disabled={messages.length === 0}
                        className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition"
                        title="Clear chat history"
                    >
                        <RotateCcw className="w-3.5 h-3.5" />
                        <span className="hidden sm:inline">Clear</span>
                    </button>
                </div>
            </div>

            {/* Messages Scroll Area */}
            <div className="flex-1 min-w-0 overflow-x-hidden overflow-y-auto px-3 md:px-6 py-6">

                {messages.length === 0 ? (

                    /* Welcome Screen */
                    <div className="max-w-3xl mx-auto my-auto py-8 flex flex-col items-center justify-center text-center">

                        <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/25 mb-5 border border-blue-400/30">
                            <Sparkles className="w-8 h-8 text-white" />
                        </div>

                        <h1 className="text-2xl font-bold text-white tracking-tight mb-2">
                            DocMind AI Document Assistant
                        </h1>

                        <p className="text-sm text-slate-400 max-w-lg mb-8 leading-relaxed">
                            Upload PDF reports, Word files, or notes and chat naturally
                            with grounded facts, source citations, and page numbers.
                        </p>

                        {/* Feature Templates */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 w-full text-left">
                            {FEATURE_TEMPLATES.map((tmpl) => (
                                <div
                                    key={tmpl.id}
                                    onClick={() => handleTemplateClick(tmpl)}
                                    className="p-4 rounded-xl bg-[#111a2e]/80 hover:bg-[#15223c] border border-slate-800/90 hover:border-blue-500/50 cursor-pointer transition-all duration-200 group shadow-sm"
                                >
                                    <div className="flex items-center gap-2 mb-1.5">
                                        {getTemplateIcon(tmpl.icon)}

                                        <h3 className="text-sm font-semibold text-slate-200 group-hover:text-blue-400 transition">
                                            {tmpl.title}
                                        </h3>
                                    </div>

                                    <p className="text-xs text-slate-400 group-hover:text-slate-300 leading-normal">
                                        {tmpl.description}
                                    </p>
                                </div>
                            ))}
                        </div>
                    </div>

                ) : (

                    /* Chat Messages */
                    <div className="w-full max-w-4xl min-w-0 mx-auto space-y-6">

                        {messages.map((msg, index) => {
                            const isUser = msg.role === 'user';

                            return (
                                <div
                                    key={msg.id || index}
                                    className={`flex items-start gap-3.5 w-full min-w-0 ${
                                        isUser ? 'justify-end' : 'justify-start'
                                    }`}
                                >

                                    {/* AI Avatar */}
                                    {!isUser && (
                                        <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center shadow-md flex-shrink-0 mt-0.5">
                                            <Bot className="w-5 h-5" />
                                        </div>
                                    )}

                                    {/* Message Bubble */}
                                    <div
                                        className={`relative group min-w-0 rounded-2xl text-sm leading-relaxed ${
                                            isUser
                                                ? 'max-w-[80%] bg-blue-600 text-white rounded-tr-sm px-4 py-3 shadow-md'
                                                : 'flex-1 max-w-full bg-[#131d33] border border-slate-800 text-slate-200 rounded-tl-sm p-4 md:p-5 shadow-sm'
                                        }`}
                                    >

                                        {isUser ? (

                                            <p className="whitespace-pre-wrap break-words [overflow-wrap:anywhere]">
                                                {msg.content}
                                            </p>

                                        ) : (

                                            <div className="min-w-0 space-y-3">

                                                {/* Markdown Response */}
                                                <div className="prose prose-invert prose-sm max-w-none min-w-0 text-slate-200 leading-relaxed font-normal break-words [overflow-wrap:anywhere] prose-headings:text-slate-100 prose-headings:font-semibold prose-headings:mt-5 prose-headings:mb-2 prose-p:my-3 prose-li:my-1 prose-strong:text-white prose-a:text-blue-400 prose-code:text-blue-300 prose-pre:max-w-full prose-pre:overflow-x-auto">

                                                    <ReactMarkdown
                                                        remarkPlugins={[remarkGfm]}
                                                    >
                                                        {msg.content || ''}
                                                    </ReactMarkdown>

                                                </div>

                                                {/* Citations */}
                                                {msg.citations && msg.citations.length > 0 && (

                                                    <div className="mt-4 pt-3 border-t border-slate-800/80">

                                                        <div className="flex items-center gap-1.5 text-xs text-blue-400 font-medium mb-2">
                                                            <BookOpen className="w-3.5 h-3.5" />

                                                            <span>
                                                                Grounded Sources & Citations ({msg.citations.length})
                                                            </span>
                                                        </div>

                                                        <div className="space-y-1.5">

                                                            {msg.citations.map((cit, citIdx) => {
                                                                const key = `${index}-${citIdx}`;
                                                                const isExpanded = expandedCitations[key];

                                                                return (
                                                                    <div
                                                                        key={citIdx}
                                                                        className="bg-[#0b1120] border border-slate-800 rounded-lg p-2.5 text-xs text-slate-300 min-w-0"
                                                                    >

                                                                        <button
                                                                            type="button"
                                                                            onClick={() => toggleCitation(index, citIdx)}
                                                                            className="w-full flex items-center justify-between gap-3 text-left"
                                                                        >

                                                                            <div className="flex items-center gap-2 flex-wrap min-w-0">

                                                                                <span className="font-semibold text-slate-200 break-all">
                                                                                    {cit.fileName || 'Document'}
                                                                                </span>

                                                                                {cit.chunkIndex !== undefined && (
                                                                                    <span className="px-1.5 py-0.5 bg-slate-800 text-slate-400 rounded text-[10px] flex-shrink-0">
                                                                                        Chunk #{cit.chunkIndex}
                                                                                    </span>
                                                                                )}

                                                                                {cit.pageNumber !== undefined && (
                                                                                    <span className="px-1.5 py-0.5 bg-slate-800 text-slate-400 rounded text-[10px] flex-shrink-0">
                                                                                        Page {cit.pageNumber}
                                                                                    </span>
                                                                                )}

                                                                                {cit.similarityScore !== undefined && (
                                                                                    <span className="text-emerald-400 text-[10px] font-medium flex-shrink-0">
                                                                                        {(cit.similarityScore * 100).toFixed(1)}% match
                                                                                    </span>
                                                                                )}

                                                                            </div>

                                                                            {isExpanded ? (
                                                                                <ChevronUp className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                                                                            ) : (
                                                                                <ChevronDown className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                                                                            )}

                                                                        </button>

                                                                        {isExpanded && cit.snippet && (
                                                                            <div className="mt-2 pt-2 border-t border-slate-800/60 font-mono text-[11px] text-slate-400 bg-[#070b14] p-2 rounded whitespace-pre-wrap break-words [overflow-wrap:anywhere]">
                                                                                {cit.snippet}
                                                                            </div>
                                                                        )}

                                                                    </div>
                                                                );
                                                            })}

                                                        </div>
                                                    </div>
                                                )}

                                                {/* Bottom Actions */}
                                                <div className="flex items-center justify-between pt-2 text-[11px] text-slate-500">

                                                    <div className="flex items-center gap-3">
                                                        {msg.responseTimeMs && (
                                                            <span>
                                                                Synthesized in {msg.responseTimeMs} ms
                                                            </span>
                                                        )}

                                                        {msg.evalMetadata && (() => {
                                                            const retrieved = toNumber(msg.evalMetadata.retrievedChunkCount);
                                                            const citations = toNumber(msg.evalMetadata.citationCount);
                                                            const grounded = toBoolean(msg.evalMetadata.hasGrounding);

                                                            return (
                                                                <span className="text-slate-500">
                                                                    {retrieved !== null ? `${retrieved} chunks` : ''}
                                                                    {retrieved !== null && citations !== null ? ' | ' : ''}
                                                                    {citations !== null ? `${citations} citations` : ''}
                                                                    {(retrieved !== null || citations !== null) && grounded !== null ? ' | ' : ''}
                                                                    {grounded !== null ? (grounded ? 'grounded' : 'low-grounding') : ''}
                                                                </span>
                                                            );
                                                        })()}
                                                    </div>

                                                    <div className="ml-auto flex items-center gap-1">

                                                        <button
                                                            onClick={() => handleCopy(msg.content, index)}
                                                            className="p-1 text-slate-400 hover:text-white rounded hover:bg-slate-800/80 transition"
                                                            title="Copy response to clipboard"
                                                        >
                                                            {copiedIndex === index ? (
                                                                <Check className="w-3.5 h-3.5 text-emerald-400" />
                                                            ) : (
                                                                <Copy className="w-3.5 h-3.5" />
                                                            )}
                                                        </button>

                                                    </div>
                                                </div>

                                            </div>
                                        )}

                                    </div>

                                    {/* User Avatar */}
                                    {isUser && (
                                        <div className="w-8 h-8 rounded-full bg-slate-700/80 text-slate-300 flex items-center justify-center flex-shrink-0 mt-0.5">
                                            <User className="w-4 h-4" />
                                        </div>
                                    )}

                                </div>
                            );
                        })}

                        {/* Loading Indicator */}
                        {isLoading && (
                            <div className="flex items-start gap-3.5 min-w-0">

                                <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center shadow-md flex-shrink-0">
                                    <Bot className="w-5 h-5" />
                                </div>

                                <div className="min-w-0 max-w-full bg-[#131d33] border border-slate-800 rounded-2xl rounded-tl-sm p-4 text-slate-400 flex items-center gap-2 text-sm shadow-sm">
                                    <Loader2 className="w-4 h-4 text-blue-500 animate-spin flex-shrink-0" />

                                    <span className="break-words">
                                        Synthesizing answer from grounded vector context...
                                    </span>
                                </div>

                            </div>
                        )}

                        <div ref={messagesEndRef} />

                    </div>
                )}

            </div>

            {/* Bottom Chat Input */}
            <div className="p-3 md:p-4 bg-[#0b1120] border-t border-slate-800/80">

                <div className="max-w-4xl mx-auto min-w-0">

                    {/* Input Container */}
                    <div className="relative bg-[#0d1424] border border-slate-700/70 rounded-2xl p-2.5 focus-within:border-blue-500/70 focus-within:ring-2 focus-within:ring-blue-500/20 transition shadow-lg">

                        <textarea
                            ref={textareaRef}
                            rows={1}
                            value={inputText}
                            onChange={handleInputChange}
                            onKeyDown={handleKeyDown}
                            placeholder={
                                selectedDoc
                                    ? `Ask a question about ${selectedDoc.filename || selectedDoc.fileName}...`
                                    : 'Ask a question across all documents...'
                            }
                            className="w-full min-w-0 bg-transparent text-sm text-slate-100 placeholder-slate-500 focus:outline-none resize-none pl-2 pr-12 py-1 max-h-40 overflow-y-auto leading-relaxed"
                        />

                        {/* Send Button */}
                        <button
                            onClick={handleSend}
                            disabled={!inputText.trim() || isLoading}
                            className="absolute right-2.5 bottom-2.5 w-8 h-8 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-40 disabled:hover:bg-blue-600 text-white flex items-center justify-center transition shadow-md shadow-blue-600/30"
                            title="Send message"
                        >
                            {isLoading ? (
                                <Loader2 className="w-4 h-4 animate-spin text-white" />
                            ) : (
                                <Send className="w-4 h-4" />
                            )}
                        </button>

                    </div>

                    {/* Input Instructions */}
                    <div className="flex items-center justify-between text-[11px] text-slate-500 mt-2 px-1">

                        <div className="flex items-center gap-1.5">

                            <span>Press</span>

                            <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-400 font-mono text-[10px]">
                                Enter
                            </kbd>

                            <span>to send,</span>

                            <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-400 font-mono text-[10px]">
                                Shift+Enter
                            </kbd>

                            <span>for newline</span>

                        </div>

                        <div className="flex items-center gap-1 text-slate-400">
                            <Zap className="w-3 h-3 text-blue-500" />
                            <span>Grounded RAG</span>
                        </div>

                    </div>

                </div>
            </div>

        </main>
    );
}
