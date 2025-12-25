import { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_CURRENT_USER, UPDATE_PROFILE, CHANGE_PASSWORD } from '../graphql/queries';

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

export default function MyProfile() {
  const [activeTab, setActiveTab] = useState<'profile' | 'password'>('profile');
  const [profileData, setProfileData] = useState({
    email: '',
    firstName: '',
    lastName: '',
  });
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data, loading, refetch } = useQuery<{ currentUser: CurrentUser }>(GET_CURRENT_USER);
  const [updateProfile, { loading: updating }] = useMutation(UPDATE_PROFILE);
  const [changePassword, { loading: changing }] = useMutation(CHANGE_PASSWORD);

  const currentUser = data?.currentUser;

  useEffect(() => {
    if (currentUser) {
      setProfileData({
        email: currentUser.email,
        firstName: currentUser.firstName,
        lastName: currentUser.lastName,
      });
    }
  }, [currentUser]);

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!profileData.email || !profileData.firstName || !profileData.lastName) {
      setError('Всички полета са задължителни');
      return;
    }

    try {
      await updateProfile({
        variables: {
          input: {
            email: profileData.email,
            firstName: profileData.firstName,
            lastName: profileData.lastName,
          },
        },
      });
      setSuccess('Профилът беше успешно обновен');
      refetch();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Грешка при запис');
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!passwordData.currentPassword || !passwordData.newPassword || !passwordData.confirmPassword) {
      setError('Всички полета са задължителни');
      return;
    }

    if (passwordData.newPassword.length < 6) {
      setError('Новата парола трябва да е поне 6 символа');
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('Новата парола и потвърждението не съвпадат');
      return;
    }

    try {
      await changePassword({
        variables: {
          input: {
            currentPassword: passwordData.currentPassword,
            newPassword: passwordData.newPassword,
          },
        },
      });
      setSuccess('Паролата беше успешно променена');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Грешка при смяна на парола');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-6 max-w-2xl">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Моят профил</h1>

      {/* User Info Card */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="flex items-center">
          <div className="w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-2xl font-bold">
            {currentUser?.firstName?.charAt(0)}{currentUser?.lastName?.charAt(0)}
          </div>
          <div className="ml-4">
            <h2 className="text-xl font-semibold text-gray-800">
              {currentUser?.firstName} {currentUser?.lastName}
            </h2>
            <p className="text-gray-500">@{currentUser?.username}</p>
            {currentUser?.group && (
              <span className="inline-block mt-1 px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                {currentUser.group.name}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-lg shadow">
        <div className="border-b">
          <nav className="flex">
            <button
              onClick={() => {
                setActiveTab('profile');
                setError('');
                setSuccess('');
              }}
              className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'profile'
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Профилни данни
            </button>
            <button
              onClick={() => {
                setActiveTab('password');
                setError('');
                setSuccess('');
              }}
              className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'password'
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Смяна на парола
            </button>
          </nav>
        </div>

        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded text-sm">
              {error}
            </div>
          )}

          {success && (
            <div className="mb-4 p-3 bg-green-50 border border-green-200 text-green-700 rounded text-sm">
              {success}
            </div>
          )}

          {activeTab === 'profile' && (
            <form onSubmit={handleProfileSubmit}>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Потребителско име
                  </label>
                  <input
                    type="text"
                    value={currentUser?.username || ''}
                    disabled
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500"
                  />
                  <p className="mt-1 text-xs text-gray-500">Потребителското име не може да се променя</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Email *
                  </label>
                  <input
                    type="email"
                    value={profileData.email}
                    onChange={(e) => setProfileData({ ...profileData, email: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Име *
                    </label>
                    <input
                      type="text"
                      value={profileData.firstName}
                      onChange={(e) => setProfileData({ ...profileData, firstName: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Фамилия *
                    </label>
                    <input
                      type="text"
                      value={profileData.lastName}
                      onChange={(e) => setProfileData({ ...profileData, lastName: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-4 border-t">
                <button
                  type="submit"
                  disabled={updating}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  {updating ? 'Запазване...' : 'Запази промените'}
                </button>
              </div>
            </form>
          )}

          {activeTab === 'password' && (
            <form onSubmit={handlePasswordSubmit}>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Текуща парола *
                  </label>
                  <input
                    type="password"
                    value={passwordData.currentPassword}
                    onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Нова парола *
                  </label>
                  <input
                    type="password"
                    value={passwordData.newPassword}
                    onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
                    required
                    minLength={6}
                  />
                  <p className="mt-1 text-xs text-gray-500">Минимум 6 символа</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Потвърди новата парола *
                  </label>
                  <input
                    type="password"
                    value={passwordData.confirmPassword}
                    onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
                    required
                    minLength={6}
                  />
                </div>
              </div>

              <div className="mt-6 pt-4 border-t">
                <button
                  type="submit"
                  disabled={changing}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  {changing ? 'Смяна...' : 'Смени паролата'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
