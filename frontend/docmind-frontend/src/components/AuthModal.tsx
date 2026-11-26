
import React, { useState, type FormEvent } from 'react';

import {
    Lock,
    User,
    Mail,
    X,
    Loader2,
    AlertCircle,
    ShieldCheck,
    ArrowRight
} from 'lucide-react';

import axios, { type AxiosError } from 'axios';

import { docmindApi } from '../api/docmindApi';

import type {
    User as AppUser,
    ApiErrorResponse
} from '../types';

type ToastType = 'success' | 'error' | 'info';

interface AuthModalProps {
    isOpen: boolean;
    onClose: () => void;
    onLoginSuccess: (user: AppUser) => void;
    showToast: (message: string, type?: ToastType) => void;
}

export default function AuthModal({
    isOpen,
    onClose,
    onLoginSuccess,
    showToast,
}: AuthModalProps) {

    const [isRegisterMode, setIsRegisterMode] = useState<boolean>(false);
    const [username, setUsername] = useState<string>('');
    const [email, setEmail] = useState<string>('');
    const [password, setPassword] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (
        e: FormEvent<HTMLFormElement>
    ): Promise<void> => {

        e.preventDefault();
        setErrorMsg(null);

        if (!username.trim() || !password.trim()) {
            setErrorMsg('Please enter both username and password.');
            return;
        }

        if (isRegisterMode) {
            if (!email.trim()) {
                setErrorMsg('Please enter a valid email address.');
                return;
            }

            if (password.length < 8) {
                setErrorMsg('Password must be at least 8 characters long.');
                return;
            }
        }

        setIsLoading(true);

        try {

            if (isRegisterMode) {

                // Register new user
                await docmindApi.register(
                    username.trim(),
                    email.trim(),
                    password
                );

                showToast(
                    'Registration successful! Logging in...',
                    'success'
                );

                // Auto login after registration
                const loginData = await docmindApi.login(
                    username.trim(),
                    password
                );

                onLoginSuccess(loginData.user);
                onClose();

            } else {

                // Sign in existing user
                const loginData = await docmindApi.login(
                    username.trim(),
                    password
                );

                showToast(
                    `Welcome back, ${loginData.username}!`,
                    'success'
                );

                onLoginSuccess(loginData.user);
                onClose();
            }

        } catch (err: unknown) {

            console.error('Auth error:', err);

            let status: number | undefined;
            let serverMsg: string | undefined;

            if (axios.isAxiosError<ApiErrorResponse>(err)) {

                status = err.response?.status;

                serverMsg =
                    err.response?.data?.message ||
                    err.response?.data?.detail;

            } else if (err instanceof Error) {

                serverMsg = err.message;
            }

            if (!serverMsg) {

                if (status === 403 || status === 401) {

                    serverMsg = isRegisterMode
                        ? 'Registration failed. Username or email may already be registered.'
                        : 'Access denied or account not found. Please check your credentials or register a new account.';

                } else {

                    serverMsg =
                        err instanceof Error
                            ? err.message
                            : 'Authentication request failed. Please check your network and backend.';
                }
            }

            setErrorMsg(serverMsg);

        } finally {

            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">

            <div className="w-full max-w-md bg-[#0e172a] border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col">

                {/* Header */}
                <div className="p-5 border-b border-slate-800/80 flex items-center justify-between">

                    <div className="flex items-center gap-3">

                        <div className="w-10 h-10 rounded-xl bg-blue-600/10 text-blue-400 flex items-center justify-center border border-blue-500/20">
                            <ShieldCheck className="w-5 h-5" />
                        </div>

                        <div>
                            <h2 className="text-base font-bold text-white">
                                {isRegisterMode
                                    ? 'Create DocMind Account'
                                    : 'Sign in to DocMind'}
                            </h2>

                            <p className="text-xs text-slate-400">
                                Secure JWT Authentication for Document RAG
                            </p>
                        </div>

                    </div>

                    <button
                        onClick={onClose}
                        disabled={isLoading}
                        className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
                        aria-label="Close authentication modal"
                    >
                        <X className="w-5 h-5" />
                    </button>

                </div>

                {/* Tab switch */}
                <div className="flex border-b border-slate-800/80 bg-[#070b14]/50">

                    <button
                        type="button"
                        onClick={() => {
                            setIsRegisterMode(false);
                            setErrorMsg(null);
                        }}
                        className={`flex-1 py-3 text-xs font-semibold text-center border-b-2 transition ${
                            !isRegisterMode
                                ? 'border-blue-500 text-blue-400 bg-blue-500/5'
                                : 'border-transparent text-slate-400 hover:text-slate-200'
                        }`}
                    >
                        Sign In
                    </button>

                    <button
                        type="button"
                        onClick={() => {
                            setIsRegisterMode(true);
                            setErrorMsg(null);
                        }}
                        className={`flex-1 py-3 text-xs font-semibold text-center border-b-2 transition ${
                            isRegisterMode
                                ? 'border-blue-500 text-blue-400 bg-blue-500/5'
                                : 'border-transparent text-slate-400 hover:text-slate-200'
                        }`}
                    >
                        Register
                    </button>

                </div>

                {/* Form Body */}
                <form
                    onSubmit={handleSubmit}
                    className="p-6 space-y-4"
                >

                    {/* Error Message */}
                    {errorMsg && (
                        <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-red-300 text-xs flex flex-col gap-1.5">

                            <div className="flex items-start gap-2">

                                <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />

                                <span className="leading-relaxed">
                                    {errorMsg}
                                </span>

                            </div>

                            {!isRegisterMode && (
                                <button
                                    type="button"
                                    onClick={() => {
                                        setIsRegisterMode(true);
                                        setErrorMsg(null);
                                    }}
                                    className="text-left text-blue-400 hover:text-blue-300 font-medium underline flex items-center gap-1 mt-1 pl-6"
                                >
                                    <span>
                                        Switch to Register tab to create this user
                                    </span>

                                    <ArrowRight className="w-3 h-3" />
                                </button>
                            )}

                        </div>
                    )}

                    {/* Username */}
                    <div className="space-y-1.5">

                        <label className="text-xs font-medium text-slate-300">
                            Username
                        </label>

                        <div className="relative">

                            <User className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />

                            <input
                                type="text"
                                required
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="e.g. Ramanuj41289"
                                autoComplete="username"
                                className="w-full pl-10 pr-4 py-2.5 bg-[#070b14] border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/80 focus:ring-1 focus:ring-blue-500/20"
                            />

                        </div>

                    </div>

                    {/* Email (Register mode only) */}
                    {isRegisterMode && (
                        <div className="space-y-1.5 animate-fadeIn">

                            <label className="text-xs font-medium text-slate-300">
                                Email Address
                            </label>

                            <div className="relative">

                                <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />

                                <input
                                    type="email"
                                    required
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="Enter your email address"
                                    autoComplete="email"
                                    className="w-full pl-10 pr-4 py-2.5 bg-[#070b14] border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/80 focus:ring-1 focus:ring-blue-500/20"
                                />

                            </div>

                        </div>
                    )}

                    {/* Password */}
                    <div className="space-y-1.5">

                        <div className="flex justify-between items-center">

                            <label className="text-xs font-medium text-slate-300">
                                Password
                            </label>

                            {isRegisterMode && (
                                <span className="text-[10px] text-slate-500">
                                    Min 8 characters
                                </span>
                            )}

                        </div>

                        <div className="relative">

                            <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />

                            <input
                                type="password"
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••••••"
                                autoComplete={
                                    isRegisterMode
                                        ? 'new-password'
                                        : 'current-password'
                                }
                                className="w-full pl-10 pr-4 py-2.5 bg-[#070b14] border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/80 focus:ring-1 focus:ring-blue-500/20"
                            />

                        </div>

                    </div>

                    {/* Submit Button */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full mt-2 py-2.5 bg-blue-600 hover:bg-blue-500 disabled:opacity-40 text-white font-medium text-xs rounded-xl transition shadow-md shadow-blue-600/20 flex items-center justify-center gap-2"
                    >
                        {isLoading ? (
                            <>
                                <Loader2 className="w-4 h-4 animate-spin" />

                                <span>
                                    {isRegisterMode
                                        ? 'Creating Account...'
                                        : 'Authenticating...'}
                                </span>
                            </>
                        ) : (
                            <span>
                                {isRegisterMode
                                    ? 'Create Account & Sign In'
                                    : 'Sign In'}
                            </span>
                        )}
                    </button>

                </form>

            </div>
        </div>
    );
}
