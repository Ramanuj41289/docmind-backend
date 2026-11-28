import {
    lazy,
    Suspense,
    useCallback,
    useEffect,
    useState
} from 'react';
import { isAxiosError } from 'axios';
import { useLocation, useNavigate } from 'react-router-dom';

import Navbar from './components/Navbar';
import Toast from './components/Toast';

import {
    docmindApi,
    getApiErrorMessage,
    setOnUnauthorizedCallback
} from './api/docmindApi';

import useAuthStore from './store/authStore';
import useDocumentStore from './store/documentStore';
import useChatStore from './store/chatStore';

import type {
    ChatMessage,
    ChatRequest,
    Conversation,
    Document as AppDocument,
    SearchMatch,
    StreamMetadata,
    User
} from './types';

const Sidebar = lazy(() => import('./components/Sidebar'));
const ChatArea = lazy(() => import('./components/ChatArea'));
const UploadModal = lazy(() => import('./components/UploadModal'));
const SearchModal = lazy(() => import('./components/SearchModal'));
const DocumentChunkModal = lazy(() => import('./components/DocumentChunkModal'));
const AuthModal = lazy(() => import('./components/AuthModal'));
const AdminPanel = lazy(() => import('./components/AdminPanel'));

type ToastType = 'success' | 'error' | 'info';

interface ToastState {
    message: string;
    type: ToastType;
}

export default function App() {
    const navigate = useNavigate();
    const location = useLocation();

    // Authentication store.
    const user = useAuthStore((state) => state.user);
    const loginUser = useAuthStore((state) => state.setUser);
    const logoutUser = useAuthStore((state) => state.logout);
    const handleUnauthorized = useAuthStore(
        (state) => state.handleUnauthorized
    );

    // Document store.
    const documents = useDocumentStore((state) => state.documents);
    const selectedDoc = useDocumentStore((state) => state.selectedDoc);
    const fetchDocuments = useDocumentStore(
        (state) => state.fetchDocuments
    );
    const selectDocument = useDocumentStore(
        (state) => state.selectDocument
    );
    const deleteDocument = useDocumentStore(
        (state) => state.deleteDocument
    );
    const clearDocuments = useDocumentStore(
        (state) => state.clearDocuments
    );

    // Chat store.
    const messages = useChatStore((state) => state.messages);
    const conversations = useChatStore((state) => state.conversations);
    const conversationId = useChatStore(
        (state) => state.conversationId
    );
    const isLoading = useChatStore((state) => state.isLoading);

    const fetchConversations = useChatStore(
        (state) => state.fetchConversations
    );
    const loadConversation = useChatStore(
        (state) => state.loadConversation
    );
    const removeConversation = useChatStore(
        (state) => state.deleteConversation
    );
    const addMessages = useChatStore((state) => state.addMessages);
    const updateMessage = useChatStore(
        (state) => state.updateMessage
    );
    const appendToken = useChatStore(
        (state) => state.appendToken
    );
    const startNewConversation = useChatStore(
        (state) => state.startNewConversation
    );
    const setConversationId = useChatStore(
        (state) => state.setConversationId
    );
    const setIsLoading = useChatStore(
        (state) => state.setIsLoading
    );
    const clearChat = useChatStore((state) => state.clearChat);

    const currentUser = user?.username || null;
    const isAdmin = String(user?.role || '').toUpperCase() === 'ADMIN';

    // Local UI state.
    const [filterText, setFilterText] = useState<string>('');
    const [streamTokens, setStreamTokens] = useState<boolean>(true);
    const [darkMode, setDarkMode] = useState<boolean>(true);
    const [backendConnected, setBackendConnected] = useState<boolean>(false);

    const [uploadModalOpen, setUploadModalOpen] = useState<boolean>(false);
    const [uploadInitialFiles, setUploadInitialFiles] = useState<File[]>([]);
    const [searchModalOpen, setSearchModalOpen] = useState<boolean>(false);
    const [chunkModalDoc, setChunkModalDoc] = useState<AppDocument | null>(null);

    const [authModalOpen, setAuthModalOpen] = useState<boolean>(false);
    const [adminPanelOpen, setAdminPanelOpen] = useState<boolean>(false);

    const [toast, setToast] = useState<ToastState | null>(null);

    // Toast notification handler.
    const showToast = useCallback(
        (message: string, type: ToastType = 'success'): void => {
            setToast({ message, type });

            window.setTimeout(() => {
                setToast((previous) =>
                    previous?.message === message ? null : previous
                );
            }, 3500);
        },
        []
    );

    // Protect admin route.
    useEffect(() => {
        if (location.pathname === '/admin' && !isAdmin) {
            setAdminPanelOpen(false);
            showToast(
                'You do not have permission to access the admin panel.',
                'error'
            );
            navigate('/', { replace: true });
            return;
        }

        setAdminPanelOpen(location.pathname === '/admin' && isAdmin);
    }, [location.pathname, isAdmin, navigate, showToast]);

    // Refresh documents.
    const refreshDocuments = useCallback(async (): Promise<void> => {
        if (!docmindApi.getAuthToken()) {
            clearDocuments();
            return;
        }

        try {
            await fetchDocuments();
            setBackendConnected(true);
        } catch (error: unknown) {
            clearDocuments();

            setBackendConnected(
                isAxiosError(error) && error.response
                    ? true
                    : await docmindApi.checkHealth()
            );

            showToast(getApiErrorMessage(error), 'error');
        }
    }, [fetchDocuments, clearDocuments, showToast]);

    // Refresh conversation history.
    const refreshConversations = useCallback(async (): Promise<void> => {
        if (!docmindApi.getAuthToken()) {
            clearChat();
            return;
        }

        try {
            await fetchConversations();
        } catch (error: unknown) {
            showToast(getApiErrorMessage(error), 'error');
        }
    }, [fetchConversations, clearChat, showToast]);

    // Backend health, authentication and keyboard shortcuts.
    useEffect(() => {
        let active = true;

        setOnUnauthorizedCallback(() => {
            handleUnauthorized();
            clearDocuments();
            clearChat();
            setAuthModalOpen(true);
            showToast(
                'Your session has expired. Please sign in again.',
                'error'
            );
        });

        void (async () => {
            const alive = await docmindApi.checkHealth();

            if (!active) return;

            setBackendConnected(alive);

            if (docmindApi.getAuthToken()) {
                await Promise.all([
                    refreshDocuments(),
                    refreshConversations()
                ]);
            }
        })();

        const onKeyDown = (event: globalThis.KeyboardEvent): void => {
            if (
                (event.metaKey || event.ctrlKey) &&
                event.key.toLowerCase() === 'k'
            ) {
                event.preventDefault();

                if (!docmindApi.getAuthToken()) {
                    setAuthModalOpen(true);
                    return;
                }

                setSearchModalOpen(true);
            }
        };

        window.addEventListener('keydown', onKeyDown);

        return () => {
            active = false;
            window.removeEventListener('keydown', onKeyDown);
            setOnUnauthorizedCallback(null);
        };
    }, [
        refreshDocuments,
        refreshConversations,
        showToast,
        handleUnauthorized,
        clearDocuments,
        clearChat
    ]);

    // Login handler.
    const handleLoginSuccess = async (authenticatedUser: User): Promise<void> => {
        loginUser(docmindApi.getCurrentUser() || authenticatedUser);
        setAuthModalOpen(false);

        await Promise.all([
            refreshDocuments(),
            refreshConversations()
        ]);

        showToast(
            `Welcome to DocMind, ${authenticatedUser?.username ||
            docmindApi.getCurrentUsername() || 'back'
            }!`,
            'success'
        );
    };

    // Logout handler.
    const handleLogout = (): void => {
        logoutUser();
        clearDocuments();
        clearChat();
        setAuthModalOpen(true);

        if (location.pathname === '/admin') {
            navigate('/');
        }

        showToast('You have signed out.', 'info');
    };

    // Open upload modal.
    const handleOpenUpload = (files: File[] = []): void => {
        if (!docmindApi.getAuthToken()) {
            setAuthModalOpen(true);
            showToast('Sign in before uploading documents.', 'info');
            return;
        }

        setUploadInitialFiles(files);
        setUploadModalOpen(true);
    };

    // Delete document.
    const handleDeleteDoc = async (id: number, name: string): Promise<void> => {
        try {
            await deleteDocument(id);

            if (selectedDoc?.id === id) {
                startNewConversation();
            }

            showToast(`Deleted ${name}.`, 'success');
        } catch (error: unknown) {
            showToast(getApiErrorMessage(error), 'error');
        }
    };

    // Select document.
    const handleSelectDocument = (document: AppDocument | null): void => {
        selectDocument(document);
        startNewConversation();
    };

    // Send chat message.
    const handleSendMessage = async (
        text: string,
        documentIdOverride?: number | null
    ): Promise<void> => {
        if (!text.trim() || isLoading) return;

        if (!docmindApi.getAuthToken()) {
            setAuthModalOpen(true);
            showToast(
                'Sign in to start a document conversation.',
                'info'
            );
            return;
        }

        const userMessage: ChatMessage = {
            id: crypto.randomUUID(),
            role: 'user',
            content: text.trim()
        };

        const assistantId = crypto.randomUUID();

        const assistantMessage: ChatMessage = {
            id: assistantId,
            role: 'assistant',
            content: '',
            citations: [],
            pending: true
        };

        addMessages([userMessage, assistantMessage]);
        setIsLoading(true);

        const activeConversationId = conversationId || crypto.randomUUID();
        setConversationId(activeConversationId);

        const request: ChatRequest = {
            question: text.trim(),
            documentId:
                documentIdOverride !== undefined
                    ? documentIdOverride
                    : selectedDoc?.id || null,
            conversationId: activeConversationId
        };

        try {
            if (streamTokens) {
                const result = await docmindApi.streamQuestion(
                    request,
                    (token: string) => {
                        appendToken(assistantId, token);
                    },
                    (metadata: StreamMetadata) => {
                        if (metadata.conversationId) {
                            setConversationId(metadata.conversationId);
                        }

                        updateMessage(assistantId, { pending: false });
                    }
                );

                if (result?.conversationId) {
                    setConversationId(result.conversationId);
                }
            } else {
                const response = await docmindApi.askQuestion(request);

                setConversationId(
                    response.conversationId || activeConversationId
                );

                updateMessage(assistantId, {
                    content: response.answer || '',
                    citations: response.citations || [],
                    responseTimeMs: response.responseTimeMs,
                    evalMetadata: response.evalMetadata,
                    pending: false
                });
            }
        } catch (error: unknown) {
            updateMessage(assistantId, {
                content: getApiErrorMessage(error),
                pending: false,
                error: true
            });
            showToast(getApiErrorMessage(error), 'error');
        } finally {
            setIsLoading(false);
            await refreshConversations();
        }
    };

    // Start new conversation.
    const handleNewConversation = (): void => {
        startNewConversation();
        showToast('Started a new conversation.', 'info');
    };

    // Load existing conversation.
    const handleSelectConversation = async (
        conversation: Conversation
    ): Promise<void> => {
        if (isLoading) return;

        try {
            await loadConversation(conversation.id);
            selectDocument(null);
        } catch (error: unknown) {
            showToast(getApiErrorMessage(error), 'error');
        }
    };

    // Delete conversation.
    const handleDeleteConversation = async (
        conversation: Conversation
    ): Promise<void> => {
        if (
            !window.confirm(
                `Delete "${conversation.title || 'this conversation'}"? This cannot be undone.`
            )
        ) {
            return;
        }

        try {
            await removeConversation(conversation.id);
            showToast('Conversation deleted.', 'success');
        } catch (error: unknown) {
            showToast(getApiErrorMessage(error), 'error');
        }
    };

    const handleClearChat = handleNewConversation;

    // Export conversation.
    const handleExportChat = (): void => {
        if (!messages.length) return;

        const markdown = [
            '# DocMind conversation',
            `Scope: ${selectedDoc?.filename ||
            selectedDoc?.fileName || 'All documents'
            }`,
            `Exported: ${new Date().toLocaleString()}`,
            '',
            ...messages.map(
                (message) =>
                    `## ${message.role === 'user' ? 'You' : 'DocMind'}\n\n${message.content}\n${(message.citations || [])
                        .map(
                            (citation) =>
                                `- ${citation.fileName || 'Document'} · chunk ${citation.chunkIndex ?? 'n/a'} · page ${citation.pageNumber ?? 'n/a'}`
                        )
                        .join('\n')}`
            )
        ].join('\n\n');

        const url = URL.createObjectURL(
            new Blob([markdown], {
                type: 'text/markdown;charset=utf-8'
            })
        );

        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `docmind-chat-${Date.now()}.md`;
        anchor.click();
        URL.revokeObjectURL(url);
    };

    // Start a conversation from a search result.
    const handleChatWithChunk = (chunk: SearchMatch): void => {
        const doc = documents.find(
            (item) => item.id === chunk.documentId
        );

        if (doc) {
            selectDocument(doc);
        }

        setSearchModalOpen(false);

        void handleSendMessage(
            `Explain this passage from ${chunk.fileName || 'the document'} (chunk ${chunk.chunkIndex ?? 'unknown'}):\n\n${chunk.snippet || ''}`,
            chunk.documentId || null
        );
    };

    // Chat with a document.
    const handleChatWithDoc = (doc: AppDocument): void => {
        handleSelectDocument(doc);
        setChunkModalDoc(null);
    };

    // Admin navigation.
    const handleOpenAdmin = (): void => {
        if (!isAdmin) {
            showToast(
                'You do not have permission to access the admin panel.',
                'error'
            );
            return;
        }

        navigate('/admin');
    };

    const handleCloseAdmin = (): void => {
        navigate('/');
    };

    return (
        <div
            className={`${darkMode ? 'dark' : ''} min-h-screen flex flex-col bg-[#080d19] text-slate-100`}
        >
            <Navbar
                selectedDoc={selectedDoc}
                documentCount={documents.length}
                onOpenSearch={() => {
                    if (!docmindApi.getAuthToken()) {
                        setAuthModalOpen(true);
                        return;
                    }
                    setSearchModalOpen(true);
                }}
                darkMode={darkMode}
                setDarkMode={setDarkMode}
                backendConnected={backendConnected}
                currentUser={currentUser}
                onOpenAuth={() => setAuthModalOpen(true)}
                onLogout={handleLogout}
                onOpenAdmin={handleOpenAdmin}
                isAdmin={isAdmin}
            />

            <Suspense
                fallback={
                    <div className="flex flex-1 min-h-[50vh] items-center justify-center text-sm text-slate-400">
                        Loading DocMind...
                    </div>
                }
            >
                <div className="flex flex-1 min-h-0 overflow-hidden">
                    <Sidebar
                        documents={documents}
                        selectedDoc={selectedDoc}
                        onSelectDoc={handleSelectDocument}
                        onOpenUpload={handleOpenUpload}
                        onInspectDoc={setChunkModalDoc}
                        onDeleteDoc={handleDeleteDoc}
                        filterText={filterText}
                        setFilterText={setFilterText}
                        conversations={conversations}
                        activeConversationId={conversationId}
                        onNewConversation={handleNewConversation}
                        onSelectConversation={handleSelectConversation}
                        onDeleteConversation={handleDeleteConversation}
                    />

                    <ChatArea
                        messages={messages}
                        onSendMessage={(message: string) => {
                            void handleSendMessage(message);
                        }}
                        selectedDoc={selectedDoc}
                        streamTokens={streamTokens}
                        setStreamTokens={setStreamTokens}
                        onClearChat={handleClearChat}
                        onExportChat={handleExportChat}
                        isLoading={isLoading}
                    />
                </div>

                {uploadModalOpen && (
                    <UploadModal
                        isOpen={uploadModalOpen}
                        onClose={() => setUploadModalOpen(false)}
                        initialFiles={uploadInitialFiles}
                        onUploadSuccess={refreshDocuments}
                        showToast={showToast}
                    />
                )}

                {searchModalOpen && (
                    <SearchModal
                        isOpen={searchModalOpen}
                        onClose={() => setSearchModalOpen(false)}
                        documents={documents}
                        onChatWithChunk={handleChatWithChunk}
                        showToast={showToast}
                    />
                )}

                {chunkModalDoc && (
                    <DocumentChunkModal
                        isOpen={Boolean(chunkModalDoc)}
                        onClose={() => setChunkModalDoc(null)}
                        document={chunkModalDoc}
                        onDeleteDocument={handleDeleteDoc}
                        onChatWithDoc={handleChatWithDoc}
                        showToast={showToast}
                    />
                )}

                {authModalOpen && (
                    <AuthModal
                        isOpen={authModalOpen}
                        onClose={() => setAuthModalOpen(false)}
                        onLoginSuccess={handleLoginSuccess}
                        showToast={showToast}
                    />
                )}

                {adminPanelOpen && isAdmin && (
                    <AdminPanel
                        isOpen={adminPanelOpen && isAdmin}
                        onClose={handleCloseAdmin}
                        showToast={showToast}
                    />
                )}
            </Suspense>

            <Toast
                toast={toast}
                onClose={() => setToast(null)}
            />
        </div>
    );
}
