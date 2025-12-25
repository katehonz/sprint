import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

export default function Layout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  const toggleSidebar = () => {
    setSidebarCollapsed(!sidebarCollapsed);
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
      />

      <div className="flex-1 flex flex-col">
        <Header toggleSidebar={toggleSidebar} />

        <main className="flex-1 overflow-auto">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            <Outlet />
          </div>
        </main>

        <footer className="bg-white border-t border-gray-200 px-6 py-3">
          <div className="flex justify-between items-center text-sm text-gray-500">
            <div>
              © 2025 SP-AC-BG | Българска счетоводна система
            </div>
            <div className="flex items-center space-x-4">
              <span className="flex items-center">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                ЕЦБ връзка активна
              </span>
              <span>v1.0.0</span>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
}
