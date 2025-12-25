import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

const menuSections = [
  {
    title: "Основни",
    icon: "📊",
    items: [
      { title: "Табло", icon: "🏠", path: "/" },
      { title: "Отчети", icon: "📈", path: "/reports" },
      { title: "По контрагенти", icon: "👤", path: "/reports/counterparty" },
      { title: "Месечна статистика", icon: "📅", path: "/reports/monthly-stats" },
    ]
  },
  {
    title: "Счетоводство",
    icon: "📚",
    items: [
      { title: "Нов запис", icon: "📝", path: "/journal-entry/new" },
      { title: "Журнални записи", icon: "📋", path: "/journal-entries" },
      { title: "Сметкоплан", icon: "🗂️", path: "/accounts" },
      { title: "Контрагенти", icon: "👥", path: "/counterparts" },
      { title: "Банки", icon: "🏦", path: "/banks" },
      { title: "Дълготрайни активи", icon: "🏭", path: "/fixed-assets" },
      { title: "Склад", icon: "📦", path: "/inventory" },
      { title: "Производство", icon: "⚙️", path: "/production" },
      { title: "Център за импорти", icon: "📥", path: "/imports" },
      { title: "AI Сканиране", icon: "🔎", path: "/doc-scanner" },
      { title: "Сканирани фактури", icon: "📑", path: "/scanned-invoices" },
    ]
  },
  {
    title: "ДДС",
    icon: "💰",
    items: [
      { title: "Нова ДДС операция", icon: "➕", path: "/vat/entry" },
      { title: "Декларации", icon: "📄", path: "/vat/returns", badge: "1", color: "bg-red-100 text-red-600" },
      { title: "Ставки", icon: "🏷️", path: "/vat/rates" },
    ]
  },
  {
    title: "Системни",
    icon: "⚙️",
    items: [
      { title: "Компании", icon: "🏢", path: "/companies" },
      { title: "Валути", icon: "💱", path: "/currencies" },
      { title: "Потребители", icon: "👤", path: "/settings/users" },
      { title: "Настройки", icon: "🔧", path: "/settings" },
    ]
  }
];

interface SidebarProps {
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
}

export default function Sidebar({ collapsed, setCollapsed }: SidebarProps) {
  const location = useLocation();
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(['Основни', 'Счетоводство', 'ДДС']));

  const toggleSection = (sectionTitle: string) => {
    setExpandedSections(prev => {
      const newSet = new Set(prev);
      if (newSet.has(sectionTitle)) {
        newSet.delete(sectionTitle);
      } else {
        newSet.add(sectionTitle);
      }
      return newSet;
    });
  };

  const isActive = (path: string) => {
    if (path === '/' && location.pathname === '/') return true;
    if (path !== '/' && location.pathname.startsWith(path)) return true;
    return false;
  };

  return (
    <div className={`bg-white border-r border-gray-200 flex flex-col transition-all duration-300 shadow-sm ${
      collapsed ? 'w-16' : 'w-64'
    }`}>
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center">
          {!collapsed && (
            <div>
              <h1 className="text-lg font-bold text-gray-900">SP-AC-BG</h1>
              <p className="text-xs text-gray-500">Счетоводна система</p>
            </div>
          )}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className={`p-1 rounded-md hover:bg-gray-100 ${collapsed ? 'mx-auto' : 'ml-auto'}`}
          >
            <div className="w-4 h-4 text-gray-600">
              {collapsed ? '→' : '←'}
            </div>
          </button>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-4">
        {menuSections.map((section) => (
          <div key={section.title} className="mb-4">
            {/* Section Header */}
            <button
              onClick={() => toggleSection(section.title)}
              className={`w-full flex items-center px-4 py-2 text-left hover:bg-gray-50 ${
                collapsed ? 'justify-center' : 'justify-between'
              }`}
            >
              <div className="flex items-center">
                <span className="text-lg">{section.icon}</span>
                {!collapsed && (
                  <span className="ml-3 text-sm font-medium text-gray-700">
                    {section.title}
                  </span>
                )}
              </div>
              {!collapsed && (
                <div className="text-gray-400 text-xs">
                  {expandedSections.has(section.title) ? '▼' : '▶'}
                </div>
              )}
            </button>

            {/* Menu Items */}
            {(!collapsed && expandedSections.has(section.title)) && (
              <div className="ml-4 mt-1 space-y-1">
                {section.items.map((item) => (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`flex items-center px-4 py-2 rounded-md text-sm transition-colors ${
                      isActive(item.path)
                        ? 'bg-blue-50 text-blue-700 border-r-2 border-blue-700'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    }`}
                  >
                    <span className="text-base">{item.icon}</span>
                    <span className="ml-3">{item.title}</span>
                    {'badge' in item && item.badge && (
                      <span className={`ml-auto px-2 py-0.5 text-xs rounded-full ${
                        'color' in item && item.color ? item.color : 'bg-gray-100 text-gray-600'
                      }`}>
                        {item.badge}
                      </span>
                    )}
                  </Link>
                ))}
              </div>
            )}
          </div>
        ))}
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-gray-200">
        {!collapsed && (
          <div className="text-xs text-gray-500 text-center">
            <div className="flex items-center justify-center">
              <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
              ЕЦБ връзка активна
            </div>
            <div className="mt-1">EUR базова валута</div>
          </div>
        )}
      </div>
    </div>
  );
}
