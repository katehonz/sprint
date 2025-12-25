import React, { createContext, useState, useContext, useMemo } from 'react';
import type { ReactNode } from 'react';

interface CompanyContextType {
    companyId: string | null;
    setCompanyId: (companyId: string) => void;
}

const CompanyContext = createContext<CompanyContextType | undefined>(undefined);

export const CompanyProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [companyId, setCompanyId] = useState<string | null>(localStorage.getItem('selectedCompanyId'));

    const handleSetCompanyId = (newCompanyId: string) => {
        setCompanyId(newCompanyId);
        localStorage.setItem('selectedCompanyId', newCompanyId);
    };
    
    const value = useMemo(() => ({
        companyId,
        setCompanyId: handleSetCompanyId
    }), [companyId]);

    return (
        <CompanyContext.Provider value={value}>
            {children}
        </CompanyContext.Provider>
    );
};

export const useCompany = (): CompanyContextType => {
    const context = useContext(CompanyContext);
    if (context === undefined) {
        throw new Error('useCompany must be used within a CompanyProvider');
    }
    return context;
};
