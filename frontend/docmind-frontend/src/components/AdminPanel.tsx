
import { useEffect, useState } from 'react';

import {
    Activity,
    Shield,
    Users,
    X,
    RefreshCw,
    ShieldCheck
} from 'lucide-react';

import {
    docmindApi,
    getApiErrorMessage
} from '../api/docmindApi';

import type {
    User,
    UserRole,
    AdminStatus
} from '../types';

type ToastType = 'success' | 'error' | 'info';

interface AdminPanelProps {
    isOpen: boolean;
    onClose: () => void;
    showToast: (message: string, type?: ToastType) => void;
}

type AdminMetricValue = number | string;

const metricLabel = (key: string): string => {
    return key
        .replace(/([A-Z])/g, ' $1')
        .replace(/_/g, ' ')
        .replace(/^./, (value) => value.toUpperCase());
};

export default function AdminPanel({
    isOpen,
    onClose,
    showToast
}: AdminPanelProps) {

    const [stats, setStats] = useState<AdminStatus | null>(null);
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [updatingId, setUpdatingId] = useState<number | null>(null);

    // Load dashboard statistics and users
    const load = async (): Promise<void> => {

        setLoading(true);

        try {

            const [nextStats, nextUsers] = await Promise.all([
                docmindApi.getAdminStats(),
                docmindApi.getAdminUsers()
            ]);

            setStats(nextStats ?? null);

            setUsers(Array.isArray(nextUsers) ? nextUsers : []);

        } catch (error: unknown) {

            showToast(getApiErrorMessage(error), 'error');

        } finally {

            setLoading(false);
        }
    };

    // Load data when the admin panel opens
    useEffect(() => {

        if (isOpen) {
            void load();
        }

    }, [isOpen]);

    // Change user role
    const changeRole = async (
        user: User,
        role: UserRole
    ): Promise<void> => {

        if (user.role === role || user.id == null) {
            return;
        }

        setUpdatingId(user.id);

        const currentRole = String(user.role || 'USER').toUpperCase();
        const nextRole = String(role).toUpperCase();
        const highRiskChange = currentRole === 'ADMIN' || nextRole === 'ADMIN';

        const approvalConfirmed = highRiskChange
            ? window.confirm(
                `Confirm high-risk role update for ${user.username} (${currentRole} -> ${nextRole})?`
            )
            : undefined;

        if (highRiskChange && !approvalConfirmed) {
            setUpdatingId(null);
            showToast('Role update cancelled.', 'info');
            return;
        }

        try {

            await docmindApi.updateUserRole(
                user.id,
                role,
                approvalConfirmed
            );

            setUsers((items) =>
                items.map((item) =>
                    item.id === user.id
                        ? { ...item, role }
                        : item
                )
            );

            showToast(
                `Updated ${user.username}'s role to ${role}.`,
                'success'
            );

            await load();

        } catch (error: unknown) {

            showToast(getApiErrorMessage(error), 'error');

        } finally {

            setUpdatingId(null);
        }
    };

    if (!isOpen) {
        return null;
    }

    // Convert AdminStatus fields into dashboard metrics
    const metricEntries: [string, AdminMetricValue][] = stats
        ? Object.entries(stats).filter(
            ([, value]) =>
                typeof value === 'number' ||
                typeof value === 'string'
        ) as [string, AdminMetricValue][]
        : [];

    const displayedMetrics: [string, AdminMetricValue][] =
        metricEntries.length > 0
            ? metricEntries
            : [['totalUsers', users.length]];

    return (
        <div
            className="fixed inset-0 z-[70] flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) {
                    onClose();
                }
            }}
        >

            <section className="flex max-h-[90vh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl border border-slate-700 bg-[#0b1120] shadow-2xl">

                {/* Header */}
                <header className="flex items-center justify-between border-b border-slate-800 px-5 py-4">

                    <div className="flex items-center gap-3">

                        <div className="rounded-xl bg-blue-500/15 p-2 text-blue-300">
                            <ShieldCheck className="h-5 w-5" />
                        </div>

                        <div>
                            <h2 className="text-base font-semibold text-white">
                                Administration
                            </h2>

                            <p className="text-xs text-slate-400">
                                User access and platform overview
                            </p>
                        </div>

                    </div>

                    <div className="flex items-center gap-2">

                        <button
                            onClick={() => void load()}
                            disabled={loading}
                            className="rounded-lg border border-slate-700 p-2 text-slate-300 hover:bg-slate-800 disabled:opacity-50"
                            title="Refresh"
                        >
                            <RefreshCw
                                className={`h-4 w-4 ${
                                    loading ? 'animate-spin' : ''
                                }`}
                            />
                        </button>

                        <button
                            onClick={onClose}
                            className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white"
                            aria-label="Close administration"
                        >
                            <X className="h-4 w-4" />
                        </button>

                    </div>

                </header>

                {/* Dashboard Content */}
                <div className="flex-1 space-y-5 overflow-y-auto p-5">

                    {/* Statistics Cards */}
                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">

                        {displayedMetrics.map(([key, value]) => (

                            <div
                                key={key}
                                className="rounded-xl border border-slate-800 bg-[#111a2e] p-4"
                            >

                                <div className="mb-2 flex items-center gap-2 text-xs text-slate-400">

                                    <Activity className="h-4 w-4 text-blue-400" />

                                    {metricLabel(key)}

                                </div>

                                <p className="text-2xl font-semibold text-white">
                                    {value}
                                </p>

                            </div>

                        ))}

                    </div>

                    {/* User Management */}
                    <div className="overflow-hidden rounded-xl border border-slate-800">

                        <div className="flex items-center gap-2 border-b border-slate-800 bg-[#111a2e] px-4 py-3">

                            <Users className="h-4 w-4 text-blue-400" />

                            <h3 className="text-sm font-semibold text-white">
                                User management
                            </h3>

                            <span className="ml-auto text-xs text-slate-500">
                                {users.length} users
                            </span>

                        </div>

                        <div className="overflow-x-auto">

                            <table className="w-full min-w-[620px] text-left text-xs">

                                <thead className="bg-[#0e172a] text-[10px] uppercase tracking-wider text-slate-500">

                                    <tr>
                                        <th className="px-4 py-3">
                                            User
                                        </th>

                                        <th className="px-4 py-3">
                                            Email
                                        </th>

                                        <th className="px-4 py-3">
                                            Role
                                        </th>

                                        <th className="px-4 py-3">
                                            Access
                                        </th>
                                    </tr>

                                </thead>

                                <tbody className="divide-y divide-slate-800/80">

                                    {users.map((user) => (

                                        <tr
                                            key={user.id}
                                            className="text-slate-300"
                                        >

                                            {/* Username */}
                                            <td className="px-4 py-3">

                                                <span className="font-medium text-white">
                                                    {user.username || `User ${user.id}`}
                                                </span>

                                                <span className="mt-0.5 block text-[10px] text-slate-500">
                                                    ID: {user.id}
                                                </span>

                                            </td>

                                            {/* Email */}
                                            <td className="px-4 py-3">
                                                {user.email || 'Not provided'}
                                            </td>

                                            {/* Current Role */}
                                            <td className="px-4 py-3">

                                                <span className="rounded-full border border-slate-700 px-2 py-1 text-[10px]">
                                                    {user.role || 'USER'}
                                                </span>

                                            </td>

                                            {/* Role Selection */}
                                            <td className="px-4 py-3">

                                                <select
                                                    value={user.role || 'USER'}
                                                    disabled={
                                                        user.id == null ||
                                                        updatingId === user.id
                                                    }
                                                    onChange={(event) => {
                                                        void changeRole(
                                                            user,
                                                            event.target.value as UserRole
                                                        );
                                                    }}
                                                    className="rounded-lg border border-slate-700 bg-[#0b1120] px-2 py-1.5 text-xs text-slate-200 disabled:opacity-50"
                                                >

                                                    <option value="USER">
                                                        USER
                                                    </option>

                                                    <option value="ADMIN">
                                                        ADMIN
                                                    </option>

                                                </select>

                                            </td>

                                        </tr>

                                    ))}

                                    {!users.length && (

                                        <tr>
                                            <td
                                                colSpan={4}
                                                className="px-4 py-8 text-center text-slate-500"
                                            >
                                                {loading
                                                    ? 'Loading users...'
                                                    : 'No users found.'}
                                            </td>
                                        </tr>

                                    )}

                                </tbody>

                            </table>

                        </div>

                    </div>

                    {/* Security Information */}
                    <p className="flex items-center gap-2 text-[11px] text-slate-500">

                        <Shield className="h-3.5 w-3.5" />

                        Role changes are enforced by the backend.
                        The last administrator cannot be demoted.

                    </p>

                </div>

            </section>

        </div>
    );
}
