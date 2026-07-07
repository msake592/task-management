import { useEffect, useMemo, useState } from 'react';
import { getAdminUsers, updateUser } from '../api/userApi';
import { getApiErrorMessage } from '../utils/apiError';
import { getCurrentUserInfo } from '../utils/authToken';

const DEFAULT_ADMIN_EMAIL = 'admin@example.com';

function normalizePage(data) {
  return Array.isArray(data) ? data : data?.content || [];
}

function getUserDisplayName(user) {
  const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ');
  return name || user?.username || user?.email || `User #${user?.id}`;
}

function getFriendlyError(error) {
  const message = getApiErrorMessage(error, 'User role could not be updated.');

  if (message === 'You cannot change your own role') {
    return 'Kendi rolünüzü değiştiremezsiniz.';
  }

  if (message === 'At least one admin must remain in the system') {
    return 'Sistemde en az bir admin kalmalıdır.';
  }

  if (message === "This admin user's role cannot be changed") {
    return 'Bu admin kullanıcısının rolü değiştirilemez.';
  }

  return message;
}

function AdminUsersPage() {
  const currentUser = getCurrentUserInfo();
  const [users, setUsers] = useState([]);
  const [selectedRoles, setSelectedRoles] = useState({});
  const [loading, setLoading] = useState(true);
  const [savingUserId, setSavingUserId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    const loadUsers = async () => {
      try {
        setLoading(true);
        setError('');
        const data = normalizePage(await getAdminUsers());
        setUsers(data);
        setSelectedRoles(Object.fromEntries(data.map((user) => [user.id, user.roleId || ''])));
      } catch (err) {
        setError(getApiErrorMessage(err, 'Users could not be loaded.'));
      } finally {
        setLoading(false);
      }
    };

    loadUsers();
  }, []);

  const roleOptions = useMemo(() => {
    const roles = new Map();
    users.forEach((user) => {
      if (user.roleId && user.roleName) {
        roles.set(user.roleName, { id: user.roleId, name: user.roleName });
      }
    });

    return ['USER', 'ADMIN']
      .map((roleName) => roles.get(roleName))
      .filter(Boolean);
  }, [users]);

  const handleRoleChange = (userId, roleId) => {
    setSelectedRoles((current) => ({
      ...current,
      [userId]: roleId,
    }));
  };

  const handleSave = async (user) => {
    const selectedRoleId = Number(selectedRoles[user.id]);

    if (!selectedRoleId || selectedRoleId === user.roleId) {
      return;
    }

    const payload = {
      firstName: user.firstName || user.email,
      lastName: user.lastName || '',
      email: user.email,
      roleId: selectedRoleId,
    };

    try {
      setSavingUserId(user.id);
      setError('');
      setSuccess('');
      const updatedUser = await updateUser(user.id, payload);
      setUsers((currentUsers) => currentUsers.map((item) => (
        item.id === updatedUser.id ? updatedUser : item
      )));
      setSelectedRoles((current) => ({
        ...current,
        [updatedUser.id]: updatedUser.roleId || '',
      }));
      setSuccess(`${getUserDisplayName(updatedUser)} rolü güncellendi.`);
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setSavingUserId(null);
    }
  };

  return (
    <section className="page">
      <h1>User Management</h1>

      {loading && <p>Loading users...</p>}
      {error && <p className="error-message">{error}</p>}
      {success && <p className="success-message">{success}</p>}

      {!loading && users.length === 0 ? (
        <p className="empty-message">No users found.</p>
      ) : (
        <div className="admin-users-table-wrap">
          <table className="admin-users-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Current Role</th>
                <th>New Role</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => {
                const isCurrentUser = String(user.id) === String(currentUser.id)
                  || user.email === currentUser.username;
                const isDefaultAdmin = user.email === DEFAULT_ADMIN_EMAIL;
                const disabled = isCurrentUser || isDefaultAdmin || savingUserId === user.id;
                const roleChanged = Number(selectedRoles[user.id]) !== user.roleId;

                return (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{getUserDisplayName(user)}</td>
                    <td>{user.email}</td>
                    <td>{user.roleName || 'No role'}</td>
                    <td>
                      <select
                        value={selectedRoles[user.id] || ''}
                        onChange={(event) => handleRoleChange(user.id, event.target.value)}
                        disabled={disabled || roleOptions.length === 0}
                      >
                        {roleOptions.map((role) => (
                          <option key={role.id} value={role.id}>
                            {role.name}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <button
                        className="secondary-button"
                        type="button"
                        onClick={() => handleSave(user)}
                        disabled={disabled || !roleChanged}
                      >
                        {savingUserId === user.id ? 'Saving...' : 'Save'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default AdminUsersPage;
