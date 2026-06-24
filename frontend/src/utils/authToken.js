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
  const role = getCurrentUserRole();
  return role === 'ADMIN' || role === 'ROLE_ADMIN';
}
