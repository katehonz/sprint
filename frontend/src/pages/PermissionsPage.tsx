import React, { useEffect, useState } from 'react';
import type { Role, Permission } from '../types/permissions';
import { useAuth } from '../contexts/AuthContext';

const API_URL = '/api';

const PermissionsPage: React.FC = () => {
    const [roles, setRoles] = useState<Role[]>([]);
    const [permissions, setPermissions] = useState<Permission[]>([]);
    const [error, setError] = useState<string | null>(null);
    const { token } = useAuth();

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [rolesResponse, permissionsResponse] = await Promise.all([
                    fetch(`${API_URL}/roles`, { headers: { 'Authorization': `Bearer ${token}` } }),
                    fetch(`${API_URL}/permissions`, { headers: { 'Authorization': `Bearer ${token}` } }),
                ]);

                if (!rolesResponse.ok || !permissionsResponse.ok) {
                    throw new Error('Failed to fetch data');
                }

                const rolesData = await rolesResponse.json();
                const permissionsData = await permissionsResponse.json();

                setRoles(rolesData);
                setPermissions(permissionsData);
            } catch (err: any) {
                setError(err.message);
            }
        };

        if(token) {
            fetchData();
        }
    }, [token]);

    const handleToggle = async (roleId: number, permissionId: number, hasPermission: boolean) => {
        const role = roles.find(r => r.id === roleId);
        if (!role) return;

        const updatedPermissions = hasPermission
            ? role.permissions.filter(p => p.id !== permissionId)
            : [...role.permissions, permissions.find(p => p.id === permissionId)!];

        try {
            const response = await fetch(`${API_URL}/roles/${roleId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ ...role, permissions: updatedPermissions }),
            });

            if (!response.ok) {
                throw new Error('Failed to update role');
            }

            const updatedRole = await response.json();
            setRoles(roles.map(r => r.id === roleId ? updatedRole : r));

        } catch (err: any) {
            setError(err.message);
        }
    };

    if (error) {
        return <div className="text-red-600">Error: {error}</div>;
    }

    return (
        <div className="container mx-auto p-4">
            <h1 className="text-2xl font-bold mb-4">Role and Permission Management</h1>
            <div className="overflow-x-auto">
                <table className="min-w-full bg-white">
                    <thead>
                        <tr>
                            <th className="py-2 px-4 border-b">Role</th>
                            {permissions.map(p => (
                                <th key={p.id} className="py-2 px-4 border-b">
                                    {p.name}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {roles.map(role => (
                            <tr key={role.id}>
                                <td className="py-2 px-4 border-b">{role.name}</td>
                                {permissions.map(permission => {
                                    const hasPermission = role.permissions.some(p => p.id === permission.id);
                                    return (
                                        <td key={permission.id} className="py-2 px-4 border-b text-center">
                                            <label className="relative inline-flex items-center cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={hasPermission}
                                                    onChange={() => handleToggle(role.id, permission.id, hasPermission)}
                                                    className="sr-only peer"
                                                />
                                                <div className="w-11 h-6 bg-gray-200 rounded-full peer peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
                                            </label>
                                        </td>
                                    );
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};


export default PermissionsPage;
