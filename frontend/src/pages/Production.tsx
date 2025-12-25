import React, { useState } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { useCompany } from '../contexts/CompanyContext';
import {
    GET_TECHNOLOGY_CARDS,
    GET_PRODUCTION_BATCHES,
    GET_ACTIVE_TECHNOLOGY_CARDS,
    CREATE_TECHNOLOGY_CARD,
    UPDATE_TECHNOLOGY_CARD,
    DELETE_TECHNOLOGY_CARD,
    CREATE_PRODUCTION_BATCH,
    START_PRODUCTION_BATCH,
    COMPLETE_PRODUCTION_BATCH,
    CANCEL_PRODUCTION_BATCH,
    DELETE_PRODUCTION_BATCH,
    GET_ACCOUNTS
} from '../graphql/queries';

// Types
interface Account {
    id: string;
    code: string;
    name: string;
}

interface TechnologyCardStage {
    id: string;
    stageOrder: number;
    name: string;
    description?: string;
    inputQuantity: number;
    inputUnit?: string;
    inputAccount?: Account;
}

interface TechnologyCard {
    id: string;
    companyId: number;
    code: string;
    name: string;
    description?: string;
    outputQuantity: number;
    outputUnit?: string;
    isActive: boolean;
    outputAccount: Account;
    stages: TechnologyCardStage[];
    createdAt: string;
    updatedAt: string;
}

interface ProductionBatch {
    id: string;
    companyId: number;
    batchNumber: string;
    plannedQuantity: number;
    actualQuantity?: number;
    startedAt?: string;
    completedAt?: string;
    status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
    notes?: string;
    technologyCard: TechnologyCard;
    createdAt: string;
}

type TabType = 'technology-cards' | 'batches' | 'reports';

const Production: React.FC = () => {
    const { companyId } = useCompany();
    const [activeTab, setActiveTab] = useState<TabType>('technology-cards');
    const [showTechCardModal, setShowTechCardModal] = useState(false);
    const [showBatchModal, setShowBatchModal] = useState(false);
    const [editingCard, setEditingCard] = useState<TechnologyCard | null>(null);

    // Queries
    const { data: techCardsData, loading: techCardsLoading, refetch: refetchTechCards } = useQuery<{ technologyCards: TechnologyCard[] }>(GET_TECHNOLOGY_CARDS, {
        variables: { companyId },
        skip: !companyId,
    });

    const { data: batchesData, loading: batchesLoading, refetch: refetchBatches } = useQuery<{ productionBatches: ProductionBatch[] }>(GET_PRODUCTION_BATCHES, {
        variables: { companyId },
        skip: !companyId,
    });

    const { data: activeTechCardsData } = useQuery<{ activeTechnologyCards: TechnologyCard[] }>(GET_ACTIVE_TECHNOLOGY_CARDS, {
        variables: { companyId },
        skip: !companyId,
    });

    const { data: accountsData } = useQuery<{ accounts: Account[] }>(GET_ACCOUNTS, {
        variables: { companyId },
        skip: !companyId,
    });

    // Mutations
    const [createTechCard] = useMutation(CREATE_TECHNOLOGY_CARD);
    const [updateTechCard] = useMutation(UPDATE_TECHNOLOGY_CARD);
    const [deleteTechCard] = useMutation(DELETE_TECHNOLOGY_CARD);
    const [createBatch] = useMutation(CREATE_PRODUCTION_BATCH);
    const [startBatch] = useMutation(START_PRODUCTION_BATCH);
    const [completeBatch] = useMutation(COMPLETE_PRODUCTION_BATCH);
    const [cancelBatch] = useMutation(CANCEL_PRODUCTION_BATCH);
    const [deleteBatch] = useMutation(DELETE_PRODUCTION_BATCH);

    const technologyCards: TechnologyCard[] = techCardsData?.technologyCards || [];
    const productionBatches: ProductionBatch[] = batchesData?.productionBatches || [];
    const activeTechCards: TechnologyCard[] = activeTechCardsData?.activeTechnologyCards || [];
    const accounts: Account[] = accountsData?.accounts || [];

    // Form state for technology card
    const [techCardForm, setTechCardForm] = useState({
        code: '',
        name: '',
        description: '',
        outputAccountId: '',
        outputQuantity: '1',
        outputUnit: '',
        stages: [] as { stageOrder: number; name: string; inputAccountId: string; inputQuantity: string; inputUnit: string }[]
    });

    // Form state for batch
    const [batchForm, setBatchForm] = useState({
        technologyCardId: '',
        batchNumber: '',
        plannedQuantity: '',
        notes: ''
    });

    const generateBatchNumber = () => {
        const date = new Date();
        return `BATCH-${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, '0')}${String(date.getDate()).padStart(2, '0')}-${Date.now() % 10000}`;
    };

    const handleCreateTechCard = () => {
        setEditingCard(null);
        setTechCardForm({
            code: '',
            name: '',
            description: '',
            outputAccountId: '',
            outputQuantity: '1',
            outputUnit: '',
            stages: []
        });
        setShowTechCardModal(true);
    };

    const handleEditTechCard = (card: TechnologyCard) => {
        setEditingCard(card);
        setTechCardForm({
            code: card.code,
            name: card.name,
            description: card.description || '',
            outputAccountId: card.outputAccount.id,
            outputQuantity: String(card.outputQuantity),
            outputUnit: card.outputUnit || '',
            stages: card.stages.map(s => ({
                stageOrder: s.stageOrder,
                name: s.name,
                inputAccountId: s.inputAccount?.id || '',
                inputQuantity: String(s.inputQuantity),
                inputUnit: s.inputUnit || ''
            }))
        });
        setShowTechCardModal(true);
    };

    const handleSaveTechCard = async () => {
        try {
            const input = {
                companyId: Number(companyId),
                code: techCardForm.code,
                name: techCardForm.name,
                description: techCardForm.description || null,
                outputAccountId: Number(techCardForm.outputAccountId),
                outputQuantity: parseFloat(techCardForm.outputQuantity),
                outputUnit: techCardForm.outputUnit || null,
                stages: techCardForm.stages.map((s, idx) => ({
                    stageOrder: idx + 1,
                    name: s.name,
                    inputAccountId: Number(s.inputAccountId),
                    inputQuantity: parseFloat(s.inputQuantity),
                    inputUnit: s.inputUnit || null
                }))
            };

            if (editingCard) {
                await updateTechCard({
                    variables: { input: { id: Number(editingCard.id), ...input } }
                });
            } else {
                await createTechCard({ variables: { input } });
            }

            setShowTechCardModal(false);
            refetchTechCards();
        } catch (error) {
            console.error('Error saving technology card:', error);
            alert('Грешка при запазване на технологичната карта');
        }
    };

    const handleDeleteTechCard = async (id: string) => {
        if (!window.confirm('Сигурни ли сте, че искате да изтриете тази технологична карта?')) return;
        try {
            await deleteTechCard({ variables: { id: Number(id) } });
            refetchTechCards();
        } catch (error) {
            console.error('Error deleting technology card:', error);
            alert('Грешка при изтриване. Възможно е картата да има производствени партиди.');
        }
    };

    const handleCreateBatch = () => {
        setBatchForm({
            technologyCardId: '',
            batchNumber: generateBatchNumber(),
            plannedQuantity: '',
            notes: ''
        });
        setShowBatchModal(true);
    };

    const handleSaveBatch = async () => {
        try {
            await createBatch({
                variables: {
                    input: {
                        companyId: Number(companyId),
                        technologyCardId: Number(batchForm.technologyCardId),
                        batchNumber: batchForm.batchNumber,
                        plannedQuantity: parseFloat(batchForm.plannedQuantity),
                        notes: batchForm.notes || null
                    }
                }
            });
            setShowBatchModal(false);
            refetchBatches();
        } catch (error) {
            console.error('Error creating batch:', error);
            alert('Грешка при създаване на партида');
        }
    };

    const handleStartBatch = async (id: string) => {
        try {
            await startBatch({ variables: { id: Number(id) } });
            refetchBatches();
        } catch (error) {
            console.error('Error starting batch:', error);
            alert('Грешка при стартиране на партида');
        }
    };

    const handleCompleteBatch = async (id: string) => {
        try {
            await completeBatch({ variables: { id: Number(id) } });
            refetchBatches();
        } catch (error) {
            console.error('Error completing batch:', error);
            alert('Грешка при завършване на партида. Проверете дали всички етапи са завършени.');
        }
    };

    const handleCancelBatch = async (id: string) => {
        if (!window.confirm('Сигурни ли сте, че искате да отмените тази партида?')) return;
        try {
            await cancelBatch({ variables: { id: Number(id) } });
            refetchBatches();
        } catch (error) {
            console.error('Error cancelling batch:', error);
            alert('Грешка при отмяна на партида');
        }
    };

    const handleDeleteBatch = async (id: string) => {
        if (!window.confirm('Сигурни ли сте, че искате да изтриете тази партида?')) return;
        try {
            await deleteBatch({ variables: { id: Number(id) } });
            refetchBatches();
        } catch (error) {
            console.error('Error deleting batch:', error);
            alert('Грешка при изтриване на партида');
        }
    };

    const addStage = () => {
        setTechCardForm(prev => ({
            ...prev,
            stages: [...prev.stages, { stageOrder: prev.stages.length + 1, name: '', inputAccountId: '', inputQuantity: '1', inputUnit: '' }]
        }));
    };

    const removeStage = (index: number) => {
        setTechCardForm(prev => ({
            ...prev,
            stages: prev.stages.filter((_, i) => i !== index)
        }));
    };

    const updateStage = (index: number, field: string, value: string) => {
        setTechCardForm(prev => ({
            ...prev,
            stages: prev.stages.map((s, i) => i === index ? { ...s, [field]: value } : s)
        }));
    };

    const getStatusLabel = (status: string) => {
        const labels: Record<string, string> = {
            'PLANNED': 'Планирана',
            'IN_PROGRESS': 'В процес',
            'COMPLETED': 'Завършена',
            'CANCELLED': 'Отказана'
        };
        return labels[status] || status;
    };

    const getStatusColor = (status: string) => {
        const colors: Record<string, string> = {
            'PLANNED': 'bg-gray-100 text-gray-800',
            'IN_PROGRESS': 'bg-blue-100 text-blue-800',
            'COMPLETED': 'bg-green-100 text-green-800',
            'CANCELLED': 'bg-red-100 text-red-800'
        };
        return colors[status] || 'bg-gray-100 text-gray-800';
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Производство</h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Производствено счетоводство с технологични карти и автоматични операции
                    </p>
                </div>
            </div>

            {/* Tabs */}
            <div className="border-b border-gray-200">
                <nav className="-mb-px flex space-x-8">
                    <button
                        onClick={() => setActiveTab('technology-cards')}
                        className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'technology-cards'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Технологични карти
                    </button>
                    <button
                        onClick={() => setActiveTab('batches')}
                        className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'batches'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Производствени партиди
                    </button>
                    <button
                        onClick={() => setActiveTab('reports')}
                        className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${
                            activeTab === 'reports'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                        }`}
                    >
                        Справки
                    </button>
                </nav>
            </div>

            {/* Technology Cards Tab */}
            {activeTab === 'technology-cards' && (
                <div className="space-y-4">
                    <div className="flex justify-between items-center">
                        <h2 className="text-lg font-semibold text-gray-900">Технологични карти</h2>
                        <button
                            onClick={handleCreateTechCard}
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                        >
                            + Нова карта
                        </button>
                    </div>

                    <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
                        {techCardsLoading ? (
                            <div className="p-8 text-center text-gray-500">Зареждане...</div>
                        ) : technologyCards.length === 0 ? (
                            <div className="p-8 text-center text-gray-500">
                                {companyId ? 'Няма създадени технологични карти' : 'Моля, изберете компания'}
                            </div>
                        ) : (
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Код</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Наименование</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Изход</th>
                                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Етапи</th>
                                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Статус</th>
                                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Действия</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {technologyCards.map((card) => (
                                        <tr key={card.id} className="hover:bg-gray-50">
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{card.code}</td>
                                            <td className="px-6 py-4 text-sm text-gray-700">{card.name}</td>
                                            <td className="px-6 py-4 text-sm text-gray-500">
                                                {card.outputQuantity} {card.outputUnit} → {card.outputAccount.name}
                                            </td>
                                            <td className="px-6 py-4 text-center text-sm text-gray-500">{card.stages.length}</td>
                                            <td className="px-6 py-4 text-center">
                                                <span className={`px-2 py-1 text-xs rounded-full ${card.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                                                    {card.isActive ? 'Активна' : 'Неактивна'}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                                                <button onClick={() => handleEditTechCard(card)} className="text-blue-600 hover:text-blue-900 mr-3">Редактирай</button>
                                                <button onClick={() => handleDeleteTechCard(card.id)} className="text-red-600 hover:text-red-900">Изтрий</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {/* Production Batches Tab */}
            {activeTab === 'batches' && (
                <div className="space-y-4">
                    <div className="flex justify-between items-center">
                        <h2 className="text-lg font-semibold text-gray-900">Производствени партиди</h2>
                        <button
                            onClick={handleCreateBatch}
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                            disabled={activeTechCards.length === 0}
                        >
                            + Нова партида
                        </button>
                    </div>

                    <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
                        {batchesLoading ? (
                            <div className="p-8 text-center text-gray-500">Зареждане...</div>
                        ) : productionBatches.length === 0 ? (
                            <div className="p-8 text-center text-gray-500">
                                {companyId ? 'Няма създадени производствени партиди' : 'Моля, изберете компания'}
                            </div>
                        ) : (
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Номер партида</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Технологична карта</th>
                                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Количество</th>
                                        <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Статус</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Дата</th>
                                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Действия</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {productionBatches.map((batch) => (
                                        <tr key={batch.id} className="hover:bg-gray-50">
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{batch.batchNumber}</td>
                                            <td className="px-6 py-4 text-sm text-gray-700">{batch.technologyCard.name}</td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-gray-500">
                                                {batch.plannedQuantity} {batch.technologyCard.outputUnit}
                                            </td>
                                            <td className="px-6 py-4 text-center">
                                                <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(batch.status)}`}>
                                                    {getStatusLabel(batch.status)}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                {new Date(batch.createdAt).toLocaleDateString('bg-BG')}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                                                {batch.status === 'PLANNED' && (
                                                    <>
                                                        <button onClick={() => handleStartBatch(batch.id)} className="text-blue-600 hover:text-blue-900 mr-3">Започни</button>
                                                        <button onClick={() => handleDeleteBatch(batch.id)} className="text-red-600 hover:text-red-900">Изтрий</button>
                                                    </>
                                                )}
                                                {batch.status === 'IN_PROGRESS' && (
                                                    <>
                                                        <button onClick={() => handleCompleteBatch(batch.id)} className="text-green-600 hover:text-green-900 mr-3">Завърши</button>
                                                        <button onClick={() => handleCancelBatch(batch.id)} className="text-red-600 hover:text-red-900">Отмени</button>
                                                    </>
                                                )}
                                                {(batch.status === 'COMPLETED' || batch.status === 'CANCELLED') && (
                                                    <span className="text-gray-400">-</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {/* Reports Tab */}
            {activeTab === 'reports' && (
                <div className="space-y-4">
                    <h2 className="text-lg font-semibold text-gray-900">Производствени справки</h2>
                    <div className="bg-white shadow-sm rounded-lg border border-gray-200 p-6">
                        <p className="text-gray-500 text-center">
                            Справките ще бъдат добавени в бъдеща версия.
                        </p>
                    </div>
                </div>
            )}

            {/* Technology Card Modal */}
            {showTechCardModal && (
                <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
                    <div className="relative top-10 mx-auto p-5 border w-full max-w-3xl shadow-lg rounded-md bg-white mb-10">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-semibold text-gray-900">
                                {editingCard ? 'Редактиране на технологична карта' : 'Нова технологична карта'}
                            </h3>
                            <button onClick={() => setShowTechCardModal(false)} className="text-gray-400 hover:text-gray-600">
                                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Код *</label>
                                    <input
                                        type="text"
                                        value={techCardForm.code}
                                        onChange={(e) => setTechCardForm(prev => ({ ...prev, code: e.target.value }))}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Наименование *</label>
                                    <input
                                        type="text"
                                        value={techCardForm.name}
                                        onChange={(e) => setTechCardForm(prev => ({ ...prev, name: e.target.value }))}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Описание</label>
                                <textarea
                                    value={techCardForm.description}
                                    onChange={(e) => setTechCardForm(prev => ({ ...prev, description: e.target.value }))}
                                    rows={2}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Изходна сметка *</label>
                                    <select
                                        value={techCardForm.outputAccountId}
                                        onChange={(e) => setTechCardForm(prev => ({ ...prev, outputAccountId: e.target.value }))}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                    >
                                        <option value="">Изберете сметка</option>
                                        {accounts.map(acc => (
                                            <option key={acc.id} value={acc.id}>{acc.code} - {acc.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Изходно количество</label>
                                    <input
                                        type="number"
                                        step="0.0001"
                                        value={techCardForm.outputQuantity}
                                        onChange={(e) => setTechCardForm(prev => ({ ...prev, outputQuantity: e.target.value }))}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Мярка</label>
                                    <input
                                        type="text"
                                        value={techCardForm.outputUnit}
                                        onChange={(e) => setTechCardForm(prev => ({ ...prev, outputUnit: e.target.value }))}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                        placeholder="кг, бр, л..."
                                    />
                                </div>
                            </div>

                            {/* Stages */}
                            <div className="border-t pt-4">
                                <div className="flex justify-between items-center mb-2">
                                    <h4 className="font-medium text-gray-900">Етапи (вход)</h4>
                                    <button
                                        type="button"
                                        onClick={addStage}
                                        className="px-3 py-1 text-sm bg-gray-100 text-gray-700 rounded hover:bg-gray-200"
                                    >
                                        + Добави етап
                                    </button>
                                </div>

                                {techCardForm.stages.length === 0 ? (
                                    <p className="text-sm text-gray-500 text-center py-4">Няма добавени етапи</p>
                                ) : (
                                    <div className="space-y-2">
                                        {techCardForm.stages.map((stage, idx) => (
                                            <div key={idx} className="flex items-center gap-2 p-2 bg-gray-50 rounded">
                                                <span className="text-sm font-medium w-6">{idx + 1}.</span>
                                                <input
                                                    type="text"
                                                    placeholder="Наименование"
                                                    value={stage.name}
                                                    onChange={(e) => updateStage(idx, 'name', e.target.value)}
                                                    className="flex-1 px-2 py-1 text-sm border border-gray-300 rounded"
                                                />
                                                <select
                                                    value={stage.inputAccountId}
                                                    onChange={(e) => updateStage(idx, 'inputAccountId', e.target.value)}
                                                    className="w-48 px-2 py-1 text-sm border border-gray-300 rounded"
                                                >
                                                    <option value="">Сметка</option>
                                                    {accounts.map(acc => (
                                                        <option key={acc.id} value={acc.id}>{acc.code}</option>
                                                    ))}
                                                </select>
                                                <input
                                                    type="number"
                                                    step="0.0001"
                                                    placeholder="Кол."
                                                    value={stage.inputQuantity}
                                                    onChange={(e) => updateStage(idx, 'inputQuantity', e.target.value)}
                                                    className="w-20 px-2 py-1 text-sm border border-gray-300 rounded"
                                                />
                                                <input
                                                    type="text"
                                                    placeholder="Мярка"
                                                    value={stage.inputUnit}
                                                    onChange={(e) => updateStage(idx, 'inputUnit', e.target.value)}
                                                    className="w-16 px-2 py-1 text-sm border border-gray-300 rounded"
                                                />
                                                <button
                                                    type="button"
                                                    onClick={() => removeStage(idx)}
                                                    className="text-red-500 hover:text-red-700"
                                                >
                                                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                    </svg>
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="flex justify-end space-x-3 pt-4 mt-4 border-t">
                            <button onClick={() => setShowTechCardModal(false)} className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50">
                                Отказ
                            </button>
                            <button onClick={handleSaveTechCard} className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700">
                                {editingCard ? 'Запази' : 'Създай'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Create Batch Modal */}
            {showBatchModal && (
                <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
                    <div className="relative top-20 mx-auto p-5 border w-full max-w-lg shadow-lg rounded-md bg-white">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-semibold text-gray-900">Нова производствена партида</h3>
                            <button onClick={() => setShowBatchModal(false)} className="text-gray-400 hover:text-gray-600">
                                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Номер на партида *</label>
                                <input
                                    type="text"
                                    value={batchForm.batchNumber}
                                    onChange={(e) => setBatchForm(prev => ({ ...prev, batchNumber: e.target.value }))}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Технологична карта *</label>
                                <select
                                    value={batchForm.technologyCardId}
                                    onChange={(e) => setBatchForm(prev => ({ ...prev, technologyCardId: e.target.value }))}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                >
                                    <option value="">Изберете карта</option>
                                    {activeTechCards.map(tc => (
                                        <option key={tc.id} value={tc.id}>{tc.code} - {tc.name}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Планирано количество *</label>
                                <input
                                    type="number"
                                    step="0.0001"
                                    value={batchForm.plannedQuantity}
                                    onChange={(e) => setBatchForm(prev => ({ ...prev, plannedQuantity: e.target.value }))}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                    placeholder="0.00"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Бележки</label>
                                <textarea
                                    value={batchForm.notes}
                                    onChange={(e) => setBatchForm(prev => ({ ...prev, notes: e.target.value }))}
                                    rows={2}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end space-x-3 pt-4 mt-4 border-t">
                            <button onClick={() => setShowBatchModal(false)} className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50">
                                Отказ
                            </button>
                            <button onClick={handleSaveBatch} className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700">
                                Създай партида
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Production;
