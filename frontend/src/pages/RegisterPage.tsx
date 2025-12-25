import React, { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { register as registerUser } from '../services/authService';
import type { RegisterRequest } from '../types/auth';
import { Link } from 'react-router-dom';

// Matrix rain background component
const MatrixRain: React.FC = () => {
    const canvasRef = useRef<HTMLCanvasElement>(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const resizeCanvas = () => {
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
        };
        resizeCanvas();
        window.addEventListener('resize', resizeCanvas);

        const chars = '0123456789€$%+-*/=<>[]{}()ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz@#&БГЛВДДСЕИКФАКТУРАСАЛДОКРЕДИТДЕБИТ';
        const fontSize = 14;
        const columns = Math.floor(canvas.width / fontSize);
        const drops: number[] = new Array(columns).fill(1);

        const draw = () => {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.05)';
            ctx.fillRect(0, 0, canvas.width, canvas.height);

            ctx.fillStyle = '#0f0';
            ctx.font = `${fontSize}px monospace`;

            for (let i = 0; i < drops.length; i++) {
                const text = chars[Math.floor(Math.random() * chars.length)];
                const x = i * fontSize;
                const y = drops[i] * fontSize;

                const brightness = Math.random();
                if (brightness > 0.95) {
                    ctx.fillStyle = '#fff';
                } else if (brightness > 0.8) {
                    ctx.fillStyle = '#5f5';
                } else {
                    ctx.fillStyle = '#0f0';
                }

                ctx.fillText(text, x, y);

                if (y > canvas.height && Math.random() > 0.975) {
                    drops[i] = 0;
                }
                drops[i]++;
            }
        };

        const interval = setInterval(draw, 50);

        return () => {
            clearInterval(interval);
            window.removeEventListener('resize', resizeCanvas);
        };
    }, []);

    return (
        <canvas
            ref={canvasRef}
            className="fixed top-0 left-0 w-full h-full -z-10"
            style={{ background: '#000' }}
        />
    );
};

const RegisterPage: React.FC = () => {
    const { register, handleSubmit, formState: { errors } } = useForm<RegisterRequest>();
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<boolean>(false);

    const onSubmit = async (data: RegisterRequest) => {
        try {
            await registerUser(data);
            setSuccess(true);
            setError(null);
        } catch (err: any) {
            setError(err.message);
            setSuccess(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen relative">
            <MatrixRain />

            <div className="w-full max-w-md p-8 space-y-6 bg-black/80 backdrop-blur-sm rounded-lg shadow-2xl border border-green-500/30 relative z-10">
                {/* Logo/Title */}
                <div className="text-center space-y-2">
                    <div className="flex items-center justify-center space-x-3">
                        <span className="text-5xl text-green-400 font-bold animate-pulse">€</span>
                    </div>
                    <h1 className="text-2xl font-bold text-green-400 tracking-wide">
                        Счетоводство
                    </h1>
                    <h2 className="text-xl font-semibold text-green-300">
                        Спринт Експрес 2026
                    </h2>
                    <div className="h-px bg-gradient-to-r from-transparent via-green-500 to-transparent mt-4"></div>
                </div>

                <h3 className="text-lg font-medium text-center text-green-200">Регистрация</h3>

                {success ? (
                    <div className="text-center p-4 bg-green-900/50 border border-green-500/50 rounded-md">
                        <p className="text-green-400">Регистрацията е успешна!</p>
                        <Link to="/login" className="font-medium text-green-300 hover:text-green-200 transition-colors">
                            Влезте в системата
                        </Link>
                    </div>
                ) : (
                    <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-green-300">
                                Потребителско име
                            </label>
                            <input
                                id="username"
                                type="text"
                                {...register('username', { required: 'Потребителското име е задължително' })}
                                className="block w-full px-3 py-2 mt-1 bg-black/50 text-green-400 placeholder-green-700 border border-green-500/50 rounded-md shadow-sm appearance-none focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm"
                                placeholder="username"
                            />
                            {errors.username && <p className="mt-2 text-sm text-red-400">{errors.username.message}</p>}
                        </div>

                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-green-300">
                                Имейл
                            </label>
                            <input
                                id="email"
                                type="email"
                                {...register('email', { required: 'Имейлът е задължителен' })}
                                className="block w-full px-3 py-2 mt-1 bg-black/50 text-green-400 placeholder-green-700 border border-green-500/50 rounded-md shadow-sm appearance-none focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm"
                                placeholder="email@example.com"
                            />
                            {errors.email && <p className="mt-2 text-sm text-red-400">{errors.email.message}</p>}
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-green-300">
                                Парола
                            </label>
                            <input
                                id="password"
                                type="password"
                                {...register('password', { required: 'Паролата е задължителна' })}
                                className="block w-full px-3 py-2 mt-1 bg-black/50 text-green-400 placeholder-green-700 border border-green-500/50 rounded-md shadow-sm appearance-none focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm"
                                placeholder="••••••••"
                            />
                            {errors.password && <p className="mt-2 text-sm text-red-400">{errors.password.message}</p>}
                        </div>

                        {error && (
                            <div className="p-3 bg-red-900/50 border border-red-500/50 rounded-md">
                                <p className="text-sm text-red-400">{error}</p>
                            </div>
                        )}

                        <div>
                            <button
                                type="submit"
                                className="flex justify-center w-full px-4 py-2 text-sm font-medium text-black bg-green-500 border border-transparent rounded-md shadow-sm hover:bg-green-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 focus:ring-offset-black transition-all duration-200 hover:shadow-green-500/50 hover:shadow-lg"
                            >
                                Регистрация
                            </button>
                        </div>
                    </form>
                )}

                <div className="text-sm text-center">
                    <Link to="/login" className="font-medium text-green-400 hover:text-green-300 transition-colors">
                        Вече имате акаунт? Вход
                    </Link>
                </div>

                {/* Footer */}
                <div className="pt-4 border-t border-green-500/20">
                    <p className="text-xs text-center text-green-600">
                        &copy; 2026 Sprint Express Accounting System
                    </p>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;
