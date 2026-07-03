export function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1];

    if (!payload) {
      return null;
    }

    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decodedPayload = window.atob(normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '='));
    return JSON.parse(decodedPayload);
  } catch (error) {
    return null;
  }
}

function containsAdminRole(value) {
  if (typeof value === 'string') {
    return value === 'ADMIN' || value === 'ROLE_ADMIN';
  }

  if (Array.isArray(value)) {
    return value.some(containsAdminRole);
  }

  if (value && typeof value === 'object') {
    return containsAdminRole(value.name) || containsAdminRole(value.authority);
  }

  return false;
}

function getRoleClaims(tokenPayload) {
  if (!tokenPayload) {
    return [];
  }

  return [
    tokenPayload.role,
    tokenPayload.roles,
    tokenPayload.authority,
    tokenPayload.authorities,
  ];
}

export function getCurrentUserRole() {
  const tokenPayload = decodeJwtPayload(localStorage.getItem('token') || '');
  return tokenPayload?.role || '';
}

export function getCurrentUserInfo() {
  const tokenPayload = decodeJwtPayload(localStorage.getItem('token') || '');
  const role = tokenPayload?.role || '';

  return {
    username: tokenPayload?.sub || tokenPayload?.email || '',
    role,
    displayRole: role.replace(/^ROLE_/, '') || 'USER',
  };
}

export function isCurrentUserAdmin() {
  const tokenPayload = decodeJwtPayload(localStorage.getItem('token') || '');
  return getRoleClaims(tokenPayload).some(containsAdminRole);
}
