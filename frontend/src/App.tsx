import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ApolloProvider } from '@apollo/client/react';
import { apolloClient } from './graphql/client';

// App version for cache busting
export const APP_VERSION = '2025.12.22.v1';
import Layout from './components/layout/Layout';
import Dashboard from './pages/Dashboard';
import Companies from './pages/Companies';
import Accounts from './pages/Accounts';
import JournalEntries from './pages/JournalEntries';
import JournalEntryForm from './pages/JournalEntryForm';
import Counterparts from './pages/Counterparts';
import Currencies from './pages/Currencies';
import VatRates from './pages/VatRates';
import VatReturns from './pages/VatReturns';
import VATEntry from './pages/VATEntry';
import FixedAssets from './pages/FixedAssets';
import ImportCenter from './pages/ImportCenter';
import Settings from './pages/Settings';
import Banks from './pages/Banks';
import DocumentScanner from './pages/DocumentScanner';
import ScannedInvoices from './pages/ScannedInvoices';
import { CompanyProvider } from './contexts/CompanyContext';
import Inventory from './pages/Inventory';
import Reports from './pages/Reports';
import CounterpartyReports from './pages/CounterpartyReports';
import MonthlyStats from './pages/MonthlyStats';
import LoginPage from './pages/LoginPage';
import RecoverPasswordPage from './pages/RecoverPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import PermissionsPage from './pages/PermissionsPage';
import AuditLogs from './pages/AuditLogs';
import Production from './pages/Production';
import Users from './pages/Users';
import MyProfile from './pages/MyProfile';
import AccountingPeriods from './pages/AccountingPeriods';

function App() {
  // Log version on mount (for debugging cache issues)
  console.log('[App] Version:', APP_VERSION);

  return (
    <ApolloProvider client={apolloClient}>
      <CompanyProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/recover-password" element={<RecoverPasswordPage />} />
              <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
              <Route element={<ProtectedRoute />}>
                <Route path="/" element={<Layout />}>
                  <Route index element={<Dashboard />} />
                  <Route path="companies" element={<Companies />} />
                  <Route path="accounts" element={<Accounts />} />
                  <Route path="journal-entries" element={<JournalEntries />} />
                  <Route path="journal-entry/new" element={<JournalEntryForm />} />
                  <Route path="journal-entry/:id" element={<JournalEntryForm />} />
                  <Route path="counterparts" element={<Counterparts />} />
                  <Route path="currencies" element={<Currencies />} />
                  <Route path="vat/rates" element={<VatRates />} />
                  <Route path="vat/returns" element={<VatReturns />} />
                  <Route path="vat/entry" element={<VATEntry />} />
                  <Route path="vat/entry/:id" element={<VATEntry />} />
                  <Route path="fixed-assets" element={<FixedAssets />} />
                  <Route path="imports" element={<ImportCenter />} />
                  <Route path="banks" element={<Banks />} />
                  <Route path="doc-scanner" element={<DocumentScanner />} />
                  <Route path="scanned-invoices" element={<ScannedInvoices />} />
                  <Route path="inventory" element={<Inventory />} />
                  <Route path="production" element={<Production />} />
                  <Route path="reports" element={<Reports />} />
                  <Route path="reports/counterparty" element={<CounterpartyReports />} />
                  <Route path="reports/monthly-stats" element={<MonthlyStats />} />
                  <Route path="settings" element={<Settings />} />
                  <Route path="settings/permissions" element={<PermissionsPage />} />
                  <Route path="settings/audit-logs" element={<AuditLogs />} />
                  <Route path="settings/users" element={<Users />} />
                  <Route path="settings/periods" element={<AccountingPeriods />} />
                  <Route path="profile" element={<MyProfile />} />
                </Route>
              </Route>
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </CompanyProvider>
    </ApolloProvider>
  );
}

export default App;
