import { useQuery } from '@apollo/client/react';
import { Link, useNavigate } from 'react-router-dom';
import { GET_COMPANIES, GET_CURRENT_USER } from '../../graphql/queries';
import { useCompany } from '../../contexts/CompanyContext';
import { useAuth } from '../../contexts/AuthContext';
import { useEffect } from 'react';

interface Company {
  id: string;
  name: string;
  eik: string;
}

interface CompaniesData {
    companies: Company[];
}

interface CurrentUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  group?: {
    id: string;
    name: string;
  };
}

interface HeaderProps {
  toggleSidebar: () => void;
}

export default function Header({ toggleSidebar }: HeaderProps) {
  const navigate = useNavigate();
  const { data, loading } = useQuery<CompaniesData>(GET_COMPANIES);
  const { data: userData } = useQuery<{ currentUser: CurrentUser }>(GET_CURRENT_USER);
  const { companyId, setCompanyId } = useCompany();
  const { logout } = useAuth();

  const companies = data?.companies || [];
  const currentCompany = companies.find(c => c.id === companyId);
  const currentUser = userData?.currentUser;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };
  
  useEffect(() => {
    // If there is no selected company, but there are companies available,
    // select the first one by default.
    if (!companyId && companies.length > 0) {
      setCompanyId(companies[0].id);
    }
  }, [companyId, companies, setCompanyId]);

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={toggleSidebar}
            className="p-2 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-50 lg:hidden transition-colors"
          >
            <span className="text-xl">☰</span>
          </button>

            <div className="relative group ml-4">
                <button className="flex items-center">
                    <div >
                        <h2 className="text-lg font-semibold text-gray-900">
                        {currentCompany?.name || "Изберете компания"}
                        </h2>
                        <p className="text-sm text-gray-500">
                        {currentCompany?.eik ? `ЕИК: ${currentCompany.eik}` : (loading ? 'Зареждане...' : 'Няма избрана фирма')}
                        </p>
                    </div>
                    <span className="ml-2 text-gray-400 text-xs">▼</span>
                </button>

                <div className="absolute left-0 mt-2 w-72 bg-white rounded-md shadow-lg py-1 hidden group-hover:block z-50 border border-gray-200">
                {companies.map(company => (
                    <button
                        key={company.id}
                        onClick={() => setCompanyId(company.id)}
                        className={`block w-full text-left px-4 py-2 text-sm ${companyId === company.id ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'}`}
                    >
                        {company.name}
                    </button>
                ))}
                <Link
                    to="/companies"
                    className="block w-full text-left px-4 py-2 mt-1 border-t border-gray-100 text-sm font-medium text-blue-600 hover:bg-blue-50"
                >
                    Управление на фирми
                </Link>
                </div>
            </div>
        </div>

        <div className="flex items-center space-x-3">
          {/* Currency indicator */}
          <div className="hidden sm:flex items-center px-3 py-1 bg-green-50 text-green-700 rounded-md text-sm">
            <span className="font-medium">€</span>
            <span className="ml-1">EUR базова</span>
          </div>

          {/* Notifications */}
          <button className="relative p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-md transition-colors">
            <span className="text-xl">🔔</span>
            <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-medium shadow-sm">
              3
            </span>
          </button>

          {/* User Menu */}
          <div className="relative group">
            <button className="flex items-center space-x-3 p-2 rounded-lg hover:bg-gray-50 transition-colors">
              <div className="w-9 h-9 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-full flex items-center justify-center text-sm font-semibold shadow-sm">
                {currentUser ? `${currentUser.firstName?.charAt(0) || ''}${currentUser.lastName?.charAt(0) || ''}` : 'АД'}
              </div>
              <div className="hidden sm:block text-left">
                <div className="text-sm font-medium text-gray-900">
                  {currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : 'Администратор'}
                </div>
                <div className="text-xs text-gray-500">
                  {currentUser?.group?.name || 'Потребител'}
                </div>
              </div>
              <span className="text-gray-400 text-xs">▼</span>
            </button>

            {/* Dropdown Menu */}
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 hidden group-hover:block z-50 border border-gray-200">
              <Link
                to="/profile"
                className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Моят профил
              </Link>
              <Link
                to="/settings"
                className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Настройки
              </Link>
              <button
                onClick={handleLogout}
                className="block w-full text-left px-4 py-2 text-sm text-red-700 hover:bg-red-50 transition-colors"
              >
                Изход
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
