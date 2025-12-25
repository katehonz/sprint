import type { LoginRequest, RegisterRequest } from '../types/auth';

const API_URL = '/api/auth';

export const login = async (data: LoginRequest) => {
    const response = await fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    });

    if (!response.ok) {
        throw new Error('Login failed');
    }

    return response.json();
};

export const register = async (data: RegisterRequest) => {
    const response = await fetch(`${API_URL}/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Registration failed' }));
        throw new Error(errorData.message || 'Registration failed');
    }

    // Register returns no content on success, so we don't try to parse json
    return;
};
