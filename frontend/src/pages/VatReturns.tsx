import { useState, useEffect } from 'react';
import { useQuery, useMutation, useLazyQuery } from '@apollo/client/react';
import JSZip from 'jszip';
import { saveAs } from 'file-saver';
import { useCompany } from '../contexts/CompanyContext';
import {
  GET_VAT_RETURNS,
  GENERATE_VAT_RETURN,
  SUBMIT_VAT_RETURN,
  EXPORT_DEKLAR,
  EXPORT_POKUPKI,
  EXPORT_PRODAGBI,
  DELETE_VAT_RETURN,
  GET_VAT_RETURN_DETAILS,
  UPDATE_VAT_RETURN,
  GET_COMPANY,
  GET_VAT_JOURNAL_ENTRIES
} from '../graphql/queries';

interface VatReturn {
  id: string;
  periodYear: number;
  periodMonth: number;
  periodFrom: string;
  periodTo: string;
  status: 'DRAFT' | 'CALCULATED' | 'SUBMITTED' | 'ACCEPTED' | 'PAID';
  outputVatAmount: number;
  inputVatAmount: number;
  vatToPay: number;
  vatToRefund: number;
  submittedAt?: string;
  dueDate: string;
  salesDocumentCount: number;
  purchaseDocumentCount: number;
}

interface VatReturnDetails extends VatReturn {
  salesBase20: number;
  salesVat20: number;
  salesBase9: number;
  salesVat9: number;
  salesBaseVop: number;
  salesVatVop: number;
  salesBase0Export: number;
  salesBase0Vod: number;
  salesBase0Art3: number;
  salesBaseArt21: number;
  salesBaseArt69: number;
  salesBaseExempt: number;
  salesVatPersonalUse: number;
  purchaseBaseFullCredit: number;
  purchaseVatFullCredit: number;
  purchaseBasePartialCredit: number;
  purchaseVatPartialCredit: number;
  purchaseBaseNoCredit: number;
  purchaseVatAnnualAdjustment: number;
  creditCoefficient: number;
  totalDeductibleVat: number;
  calculatedAt?: string;
  effectiveVatToPay: number;
  vatForDeduction: number;
  vatRefundArt92: number;
}

interface Company {
  id: string;
  name: string;
  eik: string;
  vatNumber: string;
  managerName: string;
  napOffice: string;
}

interface VatJournalEntry {
  id: string;
  entryNumber: string;
  documentNumber: string;
  documentDate: string;
  vatDate: string;
  description: string;
  vatDocumentType: string;
  vatPurchaseOperation: string;
  vatSalesOperation: string;
  totalAmount: number;
  totalVatAmount: number;
  counterpart: {
    id: string;
    name: string;
    vatNumber: string;
    eik: string;
  } | null;
  lines: {
    id: string;
    baseAmount: number;
    vatAmount: number;
  }[];
}

type TabType = 'dds' | 'pokupki' | 'prodajbi' | 'deklaracia' | 'vies';

export default function VatReturns() {
  const { companyId } = useCompany();
  const [activeTab, setActiveTab] = useState<TabType>('dds');
  const [selectedPeriod, setSelectedPeriod] = useState<{ year: number; month: number } | null>(null);
  const [selectedReturn, setSelectedReturn] = useState<VatReturn | null>(null);
  const [showNewModal, setShowNewModal] = useState(false);
  const [newPeriod, setNewPeriod] = useState({ year: new Date().getFullYear(), month: new Date().getMonth() + 1 });
  const [manualFields, setManualFields] = useState({
    vatToPay: '0',
    vatToRefund: '0',
    effectiveVatToPay: '0',
    vatForDeduction: '0',
    vatRefundArt92: '0',
  });
  const [zipLoading, setZipLoading] = useState(false);

  // Queries
  const { data: companyData } = useQuery<{ company: Company }>(GET_COMPANY, {
    variables: { id: companyId },
    skip: !companyId,
  });
  const { data, loading, refetch } = useQuery<{ vatReturns: VatReturn[] }>(GET_VAT_RETURNS, {
    variables: { companyId },
    skip: !companyId,
  });
  const [fetchDetails, { data: detailsData, loading: detailsLoading }] = useLazyQuery<{ vatReturn: VatReturnDetails }>(GET_VAT_RETURN_DETAILS);

  // Calculate period dates for journal entries query
  const periodStart = selectedPeriod ? `${selectedPeriod.year}-${String(selectedPeriod.month).padStart(2, '0')}-01` : null;
  const periodEnd = selectedPeriod ? `${selectedPeriod.year}-${String(selectedPeriod.month).padStart(2, '0')}-${new Date(selectedPeriod.year, selectedPeriod.month, 0).getDate()}` : null;

  const { data: purchaseEntriesData } = useQuery<{ journalEntries: VatJournalEntry[] }>(GET_VAT_JOURNAL_ENTRIES, {
    variables: {
      filter: {
        companyId,
        vatFromDate: periodStart,
        vatToDate: periodEnd,
        vatPurchaseOperation: 'any',
      }
    },
    skip: !companyId || !selectedPeriod || !detailsData?.vatReturn,
  });
  const { data: salesEntriesData } = useQuery<{ journalEntries: VatJournalEntry[] }>(GET_VAT_JOURNAL_ENTRIES, {
    variables: {
      filter: {
        companyId,
        vatFromDate: periodStart,
        vatToDate: periodEnd,
        vatSalesOperation: 'any',
      }
    },
    skip: !companyId || !selectedPeriod || !detailsData?.vatReturn,
  });

  // Mutations
  const [generateVatReturn, { loading: creating }] = useMutation(GENERATE_VAT_RETURN);
  const [submitVatReturn] = useMutation(SUBMIT_VAT_RETURN);
  const [deleteVatReturn] = useMutation(DELETE_VAT_RETURN);
  const [updateVatReturn] = useMutation(UPDATE_VAT_RETURN);
  const [exportDeklar] = useMutation(EXPORT_DEKLAR);
  const [exportPokupki] = useMutation(EXPORT_POKUPKI);
  const [exportProdajbi] = useMutation(EXPORT_PRODAGBI);

  const company = companyData?.company;
  const vatReturns: VatReturn[] = data?.vatReturns || [];
  const details: VatReturnDetails | null = detailsData?.vatReturn || null;
  const isEditable = details?.status === 'DRAFT' || details?.status === 'CALCULATED';
  const purchaseEntries: VatJournalEntry[] = purchaseEntriesData?.journalEntries || [];
  const salesEntries: VatJournalEntry[] = salesEntriesData?.journalEntries || [];

  // Auto-select latest period
  useEffect(() => {
    if (vatReturns.length > 0 && !selectedReturn) {
      const latest = vatReturns[0];
      setSelectedReturn(latest);
      setSelectedPeriod({ year: latest.periodYear, month: latest.periodMonth });
      fetchDetails({ variables: { id: latest.id } });
    }
  }, [vatReturns, selectedReturn, fetchDetails]);

  // Sync manual fields when details change
  useEffect(() => {
    if (details) {
      setManualFields({
        vatToPay: details.vatToPay?.toString() || '0',
        vatToRefund: details.vatToRefund?.toString() || '0',
        effectiveVatToPay: details.effectiveVatToPay?.toString() || '0',
        vatForDeduction: details.vatForDeduction?.toString() || '0',
        vatRefundArt92: details.vatRefundArt92?.toString() || '0',
      });
    }
  }, [details]);


  // Helpers
  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount || 0);
  const formatDate = (dateStr: string) => dateStr ? new Date(dateStr).toLocaleDateString('bg-BG') : '-';
  const monthNames = ['Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни', 'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

  const downloadFile = (filename: string, base64Content: string) => {
    // Decode Base64 to raw bytes (Windows-1251 encoded)
    const binaryString = atob(base64Content);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    const blob = new Blob([bytes], { type: 'application/octet-stream' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  };

  // Handlers
  const handlePeriodChange = (year: number, month: number) => {
    const found = vatReturns.find(v => v.periodYear === year && v.periodMonth === month);
    if (found) {
      setSelectedReturn(found);
      setSelectedPeriod({ year, month });
      fetchDetails({ variables: { id: found.id } });
    }
  };

  const handleGenerate = async () => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const { data } = await generateVatReturn({
        variables: {
          input: { companyId, periodYear: newPeriod.year, periodMonth: newPeriod.month },
        },
      }) as { data: any };
      setShowNewModal(false);
      refetch();
      if (data?.generateVatReturn) {
        setSelectedReturn(data.generateVatReturn);
        setSelectedPeriod({ year: newPeriod.year, month: newPeriod.month });
        fetchDetails({ variables: { id: data.generateVatReturn.id } });
      }
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при генериране');
    }
  };

  const handleUpdateReturn = async () => {
    if (!selectedReturn) return;
    try {
      await updateVatReturn({
        variables: {
          id: selectedReturn.id,
          input: {
            vatToPay: parseFloat(manualFields.vatToPay),
            vatToRefund: parseFloat(manualFields.vatToRefund),
            effectiveVatToPay: parseFloat(manualFields.effectiveVatToPay),
            vatForDeduction: parseFloat(manualFields.vatForDeduction),
            vatRefundArt92: parseFloat(manualFields.vatRefundArt92),
          },
        },
      });
      fetchDetails({ variables: { id: selectedReturn.id } });
      alert('Данните са записани успешно');
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при запис');
    }
  };

  const handleExport = async (type: 'deklar' | 'pokupki' | 'prodajbi') => {
    if (!selectedReturn) return;
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let result: { data: any };
      const filename = `${type.toUpperCase()}.TXT`;
      if (type === 'deklar') {
        result = await exportDeklar({ variables: { id: selectedReturn.id } }) as { data: any };
        downloadFile(filename, result.data.exportDeklar);
      } else if (type === 'pokupki') {
        result = await exportPokupki({ variables: { id: selectedReturn.id } }) as { data: any };
        downloadFile(filename, result.data.exportPokupki);
      } else {
        result = await exportProdajbi({ variables: { id: selectedReturn.id } }) as { data: any };
        downloadFile(filename, result.data.exportProdajbi);
      }
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при експорт');
    }
  };

  // Helper to decode base64 to Uint8Array (for Windows-1251 encoded content)
  const base64ToBytes = (base64: string): Uint8Array => {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
  };

  const handleGenerateZip = async () => {
    if (!selectedReturn) return;
    setZipLoading(true);
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const [deklarResult, pokupkiResult, prodajbiResult] = await Promise.all([
        exportDeklar({ variables: { id: selectedReturn.id } }),
        exportPokupki({ variables: { id: selectedReturn.id } }),
        exportProdajbi({ variables: { id: selectedReturn.id } }),
      ]) as { data: any }[];

      const zip = new JSZip();
      // Backend returns Base64-encoded Windows-1251 content - decode to raw bytes
      zip.file('DEKLAR.TXT', base64ToBytes(deklarResult.data.exportDeklar));
      zip.file('POKUPKI.TXT', base64ToBytes(pokupkiResult.data.exportPokupki));
      zip.file('PRODAGBI.TXT', base64ToBytes(prodajbiResult.data.exportProdajbi));

      const zipBlob = await zip.generateAsync({ type: 'blob' });
      saveAs(zipBlob, `VAT_${selectedReturn.periodYear}_${String(selectedReturn.periodMonth).padStart(2, '0')}.zip`);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при генериране на ZIP');
    } finally {
      setZipLoading(false);
    }
  };

  const handleSubmitReturn = async () => {
    if (!selectedReturn) return;
    if (!confirm('Сигурни ли сте, че искате да подадете декларацията?')) return;
    try {
      await submitVatReturn({ variables: { id: selectedReturn.id } });
      refetch();
      fetchDetails({ variables: { id: selectedReturn.id } });
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при подаване');
    }
  };

  const handleDelete = async () => {
    if (!selectedReturn) return;
    if (!confirm('Сигурни ли сте, че искате да изтриете тази декларация?')) return;
    try {
      await deleteVatReturn({ variables: { id: selectedReturn.id } });
      setSelectedReturn(null);
      setSelectedPeriod(null);
      refetch();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Грешка при изтриване');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  // Generate list of available periods for dropdown (used for future dropdown feature)
  const _availablePeriods = vatReturns.map(v => ({ year: v.periodYear, month: v.periodMonth, id: v.id }));
  void _availablePeriods; // suppress unused variable warning

  return (
    <div className="space-y-0">
      {/* Header with period selector */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <h1 className="text-xl font-semibold text-gray-900">ДДС Дневници:</h1>
            <span className="text-gray-400">⚙</span>
          </div>
          <div className="flex items-center space-x-3">
            <span className="text-gray-600">Данъчен период:</span>
            <select
              value={selectedPeriod?.month || ''}
              onChange={(e) => selectedPeriod && handlePeriodChange(selectedPeriod.year, parseInt(e.target.value))}
              className="border border-gray-300 rounded px-3 py-1.5 text-sm"
            >
              {[...Array(12)].map((_, i) => (
                <option key={i + 1} value={i + 1}>{String(i + 1).padStart(2, '0')}</option>
              ))}
            </select>
            <select
              value={selectedPeriod?.year || ''}
              onChange={(e) => selectedPeriod && handlePeriodChange(parseInt(e.target.value), selectedPeriod.month)}
              className="border border-gray-300 rounded px-3 py-1.5 text-sm"
            >
              {[...Array(5)].map((_, i) => {
                const year = new Date().getFullYear() - i;
                return <option key={year} value={year}>{year}</option>;
              })}
            </select>
            <button
              onClick={() => setShowNewModal(true)}
              className="px-4 py-1.5 text-sm text-orange-600 border border-orange-300 rounded hover:bg-orange-50"
            >
              Нов период
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b border-gray-200">
        <nav className="flex px-6">
          {[
            { id: 'dds', label: 'ДДС' },
            { id: 'pokupki', label: 'Дневник за Покупки' },
            { id: 'prodajbi', label: 'Дневник за Продажби' },
            { id: 'deklaracia', label: 'Декларация' },
            { id: 'vies', label: 'VIES Декларация' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as TabType)}
              className={`px-4 py-3 text-sm font-medium border-b-2 -mb-px ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Content */}
      <div className="p-6">
        {!selectedReturn ? (
          <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
            <p className="text-gray-500 mb-4">Няма избран данъчен период</p>
            <button
              onClick={() => setShowNewModal(true)}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              Създай нов период
            </button>
          </div>
        ) : detailsLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin h-8 w-8 border-2 border-blue-500 border-t-transparent rounded-full" />
          </div>
        ) : details ? (
          <>
            {/* TAB: ДДС Summary */}
            {activeTab === 'dds' && (
              <div className="space-y-6">
                {/* Company Info */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                  <div className="grid grid-cols-2 gap-8">
                    <div>
                      <p className="text-sm"><span className="text-gray-500">Компания:</span> <span className="font-semibold">{company?.name}</span></p>
                      <p className="text-sm"><span className="text-gray-500">ЕИК:</span> {company?.vatNumber}</p>
                      <p className="text-sm"><span className="text-gray-500">Данъчен период:</span> {selectedPeriod?.month}/{selectedPeriod?.year}</p>
                      <p className="text-sm"><span className="text-gray-500">Контакт:</span> {company?.managerName}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold">{company?.napOffice || 'ТД НА НАП'}</p>
                      <p className="text-sm"><span className="text-gray-500">Сметка:</span> BG88BNBG96618000195001</p>
                      <p className="text-sm"><span className="text-gray-500">Вид Плащане:</span> 110000</p>
                    </div>
                  </div>
                </div>

                {/* Summary Table */}
                <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
                  <table className="min-w-full">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">#</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Дневник</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Записи</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Данъчна Основа</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Начислено ДДС</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Други Данъчни Основи</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                      <tr>
                        <td className="px-4 py-3 text-sm">1</td>
                        <td className="px-4 py-3 text-sm font-medium">Продажби</td>
                        <td className="px-4 py-3 text-sm text-center">{details.salesDocumentCount}</td>
                        <td className="px-4 py-3 text-sm text-right">{formatCurrency(details.salesBase20 + details.salesBase9 + details.salesBaseVop)}</td>
                        <td className="px-4 py-3 text-sm text-right">{formatCurrency(details.outputVatAmount)}</td>
                        <td className="px-4 py-3 text-sm text-right">{formatCurrency(details.salesBase0Export + details.salesBase0Vod + details.salesBaseExempt)}</td>
                      </tr>
                      <tr>
                        <td className="px-4 py-3 text-sm">2</td>
                        <td className="px-4 py-3 text-sm font-medium">Покупки</td>
                        <td className="px-4 py-3 text-sm text-center">{details.purchaseDocumentCount}</td>
                        <td className="px-4 py-3 text-sm text-right">{formatCurrency(details.purchaseBaseFullCredit + details.purchaseBasePartialCredit)}</td>
                        <td className="px-4 py-3 text-sm text-right">{formatCurrency(details.totalDeductibleVat)}</td>
                        <td className="px-4 py-3 text-sm text-right">{formatCurrency(details.purchaseBaseNoCredit)}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                {/* Result */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                  <div className="flex justify-between items-center">
                    <div>
                      <p className="text-sm text-gray-500">Краен Срок</p>
                      <p className="text-lg font-semibold">{formatDate(details.dueDate)}</p>
                    </div>
                    <div className="text-right space-y-2">
                      <p className="text-sm"><span className="text-gray-500">Общо ДК:</span> <span className="font-semibold">{formatCurrency(details.totalDeductibleVat)}</span></p>
                      <p className="text-lg">
                        <span className="text-gray-500">{details.vatToPay > 0 ? 'ДДС за внасяне:' : 'ДДС за възстановяване:'}</span>{' '}
                        <span className={`font-bold ${details.vatToPay > 0 ? 'text-red-600' : 'text-green-600'}`}>
                          {formatCurrency(details.vatToPay > 0 ? details.vatToPay : details.vatToRefund)}
                        </span>
                      </p>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex justify-between">
                  <div className="space-x-2">
                    {isEditable && (
                      <button onClick={handleDelete} className="px-4 py-2 text-red-600 border border-red-300 rounded hover:bg-red-50">
                        Изтрий
                      </button>
                    )}
                  </div>
                  <div className="space-x-2">
                    {details.status === 'CALCULATED' && (
                      <>
                        <button
                          onClick={handleGenerateZip}
                          disabled={zipLoading}
                          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                        >
                          {zipLoading ? 'Генериране...' : 'Изпрати към НАП (ZIP)'}
                        </button>
                        <button onClick={handleSubmitReturn} className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700">
                          Маркирай като подадена
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB: Дневник за Покупки */}
            {activeTab === 'pokupki' && (
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <h2 className="text-lg font-semibold">Дневник за Покупки за период {selectedPeriod?.month}/{selectedPeriod?.year}</h2>
                  <button onClick={() => handleExport('pokupki')} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">
                    📥 Свали
                  </button>
                </div>
                <div className="bg-white rounded-lg border border-gray-200 overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.1<br/><span className="text-xs font-normal">№ по ред</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.3<br/><span className="text-xs font-normal">Вид</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.4<br/><span className="text-xs font-normal">Документ номер</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.5<br/><span className="text-xs font-normal">Дата</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.6<br/><span className="text-xs font-normal">Номер на контр.</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.7<br/><span className="text-xs font-normal">Име на контрагента</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.8<br/><span className="text-xs font-normal">Вид стока/услуга</span></th>
                        <th className="px-3 py-2 text-right font-medium text-gray-500">кл.9<br/><span className="text-xs font-normal">ДО без право</span></th>
                        <th className="px-3 py-2 text-right font-medium text-gray-500">кл.10<br/><span className="text-xs font-normal">ДО с право ПДК</span></th>
                        <th className="px-3 py-2 text-right font-medium text-gray-500">кл.11<br/><span className="text-xs font-normal">ДДС с право ПДК</span></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {purchaseEntries.length === 0 ? (
                        <tr>
                          <td colSpan={10} className="px-4 py-8 text-center text-gray-500">
                            Няма документи за покупки в този период
                            <br/>
                            <span className="text-sm">Общо записи: {details.purchaseDocumentCount} | ДО: {formatCurrency(details.purchaseBaseFullCredit)} | ДДС: {formatCurrency(details.purchaseVatFullCredit)}</span>
                          </td>
                        </tr>
                      ) : (
                        purchaseEntries.map((entry, idx) => {
                          const totalBase = entry.lines.reduce((sum, l) => sum + (l.baseAmount || 0), 0);
                          const totalVat = entry.lines.reduce((sum, l) => sum + (l.vatAmount || 0), 0);
                          const isNoCredit = entry.vatPurchaseOperation === 'пок40';
                          return (
                            <tr key={entry.id} className="hover:bg-gray-50">
                              <td className="px-3 py-2">{idx + 1}</td>
                              <td className="px-3 py-2">{entry.vatDocumentType || '01'}</td>
                              <td className="px-3 py-2 font-medium">{entry.documentNumber}</td>
                              <td className="px-3 py-2">{formatDate(entry.vatDate)}</td>
                              <td className="px-3 py-2">{entry.counterpart?.vatNumber || entry.counterpart?.eik || '-'}</td>
                              <td className="px-3 py-2 max-w-[200px] truncate">{entry.counterpart?.name || '-'}</td>
                              <td className="px-3 py-2 max-w-[150px] truncate">{entry.description}</td>
                              <td className="px-3 py-2 text-right">{isNoCredit ? formatCurrency(totalBase) : '-'}</td>
                              <td className="px-3 py-2 text-right">{!isNoCredit ? formatCurrency(totalBase) : '-'}</td>
                              <td className="px-3 py-2 text-right font-medium">{!isNoCredit ? formatCurrency(totalVat) : '-'}</td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                    {purchaseEntries.length > 0 && (
                      <tfoot className="bg-gray-100 font-medium">
                        <tr>
                          <td colSpan={7} className="px-3 py-2 text-right">ОБЩО:</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(details.purchaseBaseNoCredit)}</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(details.purchaseBaseFullCredit)}</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(details.purchaseVatFullCredit)}</td>
                        </tr>
                      </tfoot>
                    )}
                  </table>
                </div>
              </div>
            )}

            {/* TAB: Дневник за Продажби */}
            {activeTab === 'prodajbi' && (
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <h2 className="text-lg font-semibold">Дневник за Продажби за период {selectedPeriod?.month}/{selectedPeriod?.year}</h2>
                  <button onClick={() => handleExport('prodajbi')} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">
                    📥 Свали
                  </button>
                </div>
                <div className="bg-white rounded-lg border border-gray-200 overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.1<br/><span className="text-xs font-normal">№ по ред</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.3<br/><span className="text-xs font-normal">Вид</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.4<br/><span className="text-xs font-normal">Документ номер</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.5<br/><span className="text-xs font-normal">Дата</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.6<br/><span className="text-xs font-normal">Номер на контр.</span></th>
                        <th className="px-3 py-2 text-left font-medium text-gray-500">кл.7<br/><span className="text-xs font-normal">Име на контрагента</span></th>
                        <th className="px-3 py-2 text-right font-medium text-gray-500">кл.9<br/><span className="text-xs font-normal">ДО 20%</span></th>
                        <th className="px-3 py-2 text-right font-medium text-gray-500">кл.10<br/><span className="text-xs font-normal">ДДС 20%</span></th>
                        <th className="px-3 py-2 text-right font-medium text-gray-500">кл.15<br/><span className="text-xs font-normal">ВОД</span></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {salesEntries.length === 0 ? (
                        <tr>
                          <td colSpan={9} className="px-4 py-8 text-center text-gray-500">
                            Няма документи за продажби в този период
                            <br/>
                            <span className="text-sm">Общо записи: {details.salesDocumentCount} | ДО 20%: {formatCurrency(details.salesBase20)} | ДДС: {formatCurrency(details.salesVat20)}</span>
                          </td>
                        </tr>
                      ) : (
                        salesEntries.map((entry, idx) => {
                          const totalBase = entry.lines.reduce((sum, l) => sum + (l.baseAmount || 0), 0);
                          const totalVat = entry.lines.reduce((sum, l) => sum + (l.vatAmount || 0), 0);
                          const isVOD = entry.vatSalesOperation === 'про16';
                          return (
                            <tr key={entry.id} className="hover:bg-gray-50">
                              <td className="px-3 py-2">{idx + 1}</td>
                              <td className="px-3 py-2">{entry.vatDocumentType || '01'}</td>
                              <td className="px-3 py-2 font-medium">{entry.documentNumber}</td>
                              <td className="px-3 py-2">{formatDate(entry.vatDate)}</td>
                              <td className="px-3 py-2">{entry.counterpart?.vatNumber || entry.counterpart?.eik || '-'}</td>
                              <td className="px-3 py-2 max-w-[200px] truncate">{entry.counterpart?.name || '-'}</td>
                              <td className="px-3 py-2 text-right">{!isVOD ? formatCurrency(totalBase) : '-'}</td>
                              <td className="px-3 py-2 text-right font-medium">{!isVOD ? formatCurrency(totalVat) : '-'}</td>
                              <td className="px-3 py-2 text-right">{isVOD ? formatCurrency(totalBase) : '-'}</td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                    {salesEntries.length > 0 && (
                      <tfoot className="bg-gray-100 font-medium">
                        <tr>
                          <td colSpan={6} className="px-3 py-2 text-right">ОБЩО:</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(details.salesBase20)}</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(details.salesVat20)}</td>
                          <td className="px-3 py-2 text-right">{formatCurrency(details.salesBase0Vod)}</td>
                        </tr>
                      </tfoot>
                    )}
                  </table>
                </div>
              </div>
            )}

            {/* TAB: Декларация */}
            {activeTab === 'deklaracia' && (
              <div className="space-y-6">
                <h2 className="text-lg font-semibold">Декларация за период {selectedPeriod?.month}/{selectedPeriod?.year}</h2>
                <p className="text-sm text-gray-600">Наименование: <span className="font-semibold">{company?.name}</span></p>

                {/* Раздел А */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                  <h3 className="font-semibold text-gray-800 mb-4 border-b pb-2">Раздел А: Данни за начислен данък върху добавена стойност</h3>
                  <div className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
                    <div className="flex justify-between"><span className="text-gray-600">Общ размер на данъчните основи за облагане с ДДС:</span> <span className="font-medium">{formatCurrency(details.salesBase20 + details.salesBase9 + details.salesBaseVop + details.salesBase0Art3 + details.salesBase0Vod + details.salesBase0Export + details.salesBaseArt21 + details.salesBaseArt69 + details.salesBaseExempt)} <span className="text-gray-400">01</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">Всичко начислен ДДС:</span> <span className="font-medium">{formatCurrency(details.outputVatAmount)} <span className="text-gray-400">20</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО на облагаемите доставки със ставка 20%:</span> <span className="font-medium">{formatCurrency(details.salesBase20)} <span className="text-gray-400">11</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">Начислен ДДС:</span> <span className="font-medium">{formatCurrency(details.salesVat20)} <span className="text-gray-400">21</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО на ВОП и доставки по чл.82, ал.2-6:</span> <span className="font-medium">{formatCurrency(details.salesBaseVop)} <span className="text-gray-400">12</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">Начислен ДДС за ВОП:</span> <span className="font-medium">{formatCurrency(details.salesVatVop)} <span className="text-gray-400">22</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО на облагаемите доставки със ставка 9%:</span> <span className="font-medium">{formatCurrency(details.salesBase9)} <span className="text-gray-400">13</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">Начислен данък за лични нужди:</span> <span className="font-medium">{formatCurrency(details.salesVatPersonalUse)} <span className="text-gray-400">23</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО със ставка 0% по глава трета:</span> <span className="font-medium">{formatCurrency(details.salesBase0Art3)} <span className="text-gray-400">14</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">Начислен ДДС 9%:</span> <span className="font-medium">{formatCurrency(details.salesVat9)} <span className="text-gray-400">24</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО за ВОД на стоки (0%):</span> <span className="font-medium">{formatCurrency(details.salesBase0Vod)} <span className="text-gray-400">15</span></span></div>
                    <div></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО по чл.140, 146 и чл.173 (0%):</span> <span className="font-medium">{formatCurrency(details.salesBase0Export)} <span className="text-gray-400">16</span></span></div>
                    <div></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО на услуги по чл.21, ал.2:</span> <span className="font-medium">{formatCurrency(details.salesBaseArt21)} <span className="text-gray-400">17</span></span></div>
                    <div></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО по чл.69, ал.2:</span> <span className="font-medium">{formatCurrency(details.salesBaseArt69)} <span className="text-gray-400">18</span></span></div>
                    <div></div>

                    <div className="flex justify-between"><span className="text-gray-600">ДО на освободени доставки:</span> <span className="font-medium">{formatCurrency(details.salesBaseExempt)} <span className="text-gray-400">19</span></span></div>
                    <div></div>
                  </div>
                </div>

                {/* Раздел Б */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                  <h3 className="font-semibold text-gray-800 mb-4 border-b pb-2">Раздел Б: Данни за упражнено право на данъчен кредит</h3>
                  <div className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
                    <div className="flex justify-between"><span className="text-gray-600">ДО без право на дан.кредит:</span> <span className="font-medium">{formatCurrency(details.purchaseBaseNoCredit)} <span className="text-gray-400">30</span></span></div>
                    <div></div>

                    <div className="flex justify-between"><span className="text-gray-600">- с право на пълен ДК:</span> <span className="font-medium">{formatCurrency(details.purchaseBaseFullCredit)} <span className="text-gray-400">31</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">ДДС с право на пълен ДК:</span> <span className="font-medium">{formatCurrency(details.purchaseVatFullCredit)} <span className="text-gray-400">41</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">- с право на частичен ДК:</span> <span className="font-medium">{formatCurrency(details.purchaseBasePartialCredit)} <span className="text-gray-400">32</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">ДДС с право на частичен ДК:</span> <span className="font-medium">{formatCurrency(details.purchaseVatPartialCredit)} <span className="text-gray-400">42</span></span></div>

                    <div className="flex justify-between"><span className="text-gray-600">Коефицент по чл.73, ал.5:</span> <span className="font-medium">{details.creditCoefficient?.toFixed(3) || '0.000'} <span className="text-gray-400">33</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">Годишна корекция по чл.73, ал.8:</span> <span className="font-medium">{formatCurrency(details.purchaseVatAnnualAdjustment || 0)} <span className="text-gray-400">43</span></span></div>

                    <div></div>
                    <div className="flex justify-between"><span className="text-gray-600">Общ данъчен кредит (кл.41+кл.42×кл.33+кл.43):</span> <span className="font-medium">{formatCurrency(details.totalDeductibleVat)} <span className="text-gray-400">40</span></span></div>
                  </div>
                </div>

                {/* Раздел В */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                  <h3 className="font-semibold text-gray-800 mb-4 border-b pb-2">Раздел В: Резултат за периода</h3>
                  <div className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
                    <div className="flex justify-between"><span className="text-gray-600">ДДС за внасяне (кл.20 - кл.40) &gt; 0:</span> <span className="font-medium text-red-600">{formatCurrency(details.vatToPay)} <span className="text-gray-400">50</span></span></div>
                    <div className="flex justify-between"><span className="text-gray-600">ДДС за възстановяване (кл.20 - кл.40) &lt; 0:</span> <span className="font-medium text-green-600">{formatCurrency(details.vatToRefund)} <span className="text-gray-400">60</span></span></div>
                  </div>
                </div>

                {/* Раздел Г и Д - Ръчно въвеждане */}
                {isEditable && (
                  <div className="bg-white rounded-lg border border-gray-200 p-6">
                    <h3 className="font-semibold text-gray-800 mb-4 border-b pb-2">Раздел Г: ДДС за внасяне / Раздел Д: ДДС за възстановяване</h3>
                    <div className="grid grid-cols-3 gap-6">
                      <div>
                        <label className="block text-sm text-gray-600 mb-1">Данък за внасяне от кл.50, приспаднат (кл. 70)</label>
                        <input
                          type="number"
                          step="0.01"
                          value={manualFields.vatToPay}
                          onChange={(e) => setManualFields({ ...manualFields, vatToPay: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-sm text-gray-600 mb-1">Данък за внасяне, внесен ефективно (кл. 71)</label>
                        <input
                          type="number"
                          step="0.01"
                          value={manualFields.effectiveVatToPay}
                          onChange={(e) => setManualFields({ ...manualFields, effectiveVatToPay: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                        />
                      </div>
                      <div></div>
                      <div>
                        <label className="block text-sm text-gray-600 mb-1">Съгласно чл.92, ал.1 ЗДДС (кл. 80)</label>
                        <input
                          type="number"
                          step="0.01"
                          value={manualFields.vatToRefund}
                          onChange={(e) => setManualFields({ ...manualFields, vatToRefund: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-sm text-gray-600 mb-1">Съгласно чл.92, ал.3 (кл. 81)</label>
                        <input
                          type="number"
                          step="0.01"
                          value={manualFields.vatForDeduction}
                          onChange={(e) => setManualFields({ ...manualFields, vatForDeduction: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-sm text-gray-600 mb-1">Съгласно чл.92, ал.4 (кл. 82)</label>
                        <input
                          type="number"
                          step="0.01"
                          value={manualFields.vatRefundArt92}
                          onChange={(e) => setManualFields({ ...manualFields, vatRefundArt92: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                        />
                      </div>
                    </div>
                    <div className="mt-4 flex justify-end space-x-2">
                      <button onClick={handleUpdateReturn} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                        Съхрани
                      </button>
                      <button onClick={() => window.print()} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">
                        Печат
                      </button>
                    </div>
                  </div>
                )}

                {/* Export buttons */}
                <div className="flex justify-end space-x-2">
                  <button onClick={() => handleExport('deklar')} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">
                    DEKLAR.TXT
                  </button>
                </div>
              </div>
            )}

            {/* TAB: VIES */}
            {activeTab === 'vies' && (
              <div className="space-y-6">
                <h2 className="text-lg font-semibold">VIES Декларация за период {selectedPeriod?.month}/{selectedPeriod?.year}</h2>

                <div className="bg-white rounded-lg border border-gray-200 p-6">
                  <div className="text-center py-8 text-gray-500">
                    <p>VIES Декларацията се попълва автоматично от ВОД записите в дневника за продажби.</p>
                    <p className="mt-2 text-sm">ВОД за периода: <span className="font-semibold">{formatCurrency(details.salesBase0Vod)}</span></p>
                  </div>
                </div>

                <div className="flex justify-end">
                  <button className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50" disabled>
                    VIES.TXT (скоро)
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="text-center text-gray-500 py-12">Грешка при зареждане на данните</div>
        )}
      </div>

      {/* New Period Modal */}
      {showNewModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg w-full max-w-md shadow-2xl">
            <div className="p-6 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-xl font-semibold">Нов данъчен период</h2>
              <button onClick={() => setShowNewModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Година</label>
                  <select
                    value={newPeriod.year}
                    onChange={(e) => setNewPeriod({ ...newPeriod, year: parseInt(e.target.value) })}
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  >
                    {[...Array(5)].map((_, i) => {
                      const year = new Date().getFullYear() - i;
                      return <option key={year} value={year}>{year}</option>;
                    })}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Месец</label>
                  <select
                    value={newPeriod.month}
                    onChange={(e) => setNewPeriod({ ...newPeriod, month: parseInt(e.target.value) })}
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  >
                    {monthNames.map((name, i) => (
                      <option key={i + 1} value={i + 1}>{name}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="pt-4 border-t flex justify-end space-x-2">
                <button onClick={() => setShowNewModal(false)} className="px-4 py-2 border rounded hover:bg-gray-50">
                  Отказ
                </button>
                <button
                  onClick={handleGenerate}
                  disabled={creating}
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                >
                  {creating ? 'Генериране...' : 'Генерирай'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
