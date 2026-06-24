import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { getCurrentUserInfo } from '../utils/authToken';

function getAuthState() {
  const hasToken = Boolean(localStorage.getItem('token'));

  return {
    hasToken,
    currentUser: hasToken ? getCurrentUserInfo() : null,
  };
}

function Navbar() {
  const [authState, setAuthState] = useState(getAuthState);
  const location = useLocation();
  const navigate = useNavigate();
  const { hasToken, currentUser } = authState;

  useEffect(() => {
    const syncAuthState = () => {
      setAuthState(getAuthState());
    };

    syncAuthState();
    window.addEventListener('storage', syncAuthState);
    window.addEventListener('authChanged', syncAuthState);

    return () => {
      window.removeEventListener('storage', syncAuthState);
      window.removeEventListener('authChanged', syncAuthState);
    };
  }, [location.pathname]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    window.dispatchEvent(new Event('authChanged'));
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">Task Management</div>
      <div className="navbar-links">
        <NavLink to="/" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Home
        </NavLink>
        <NavLink to="/tasks" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Tasks
        </NavLink>
        <NavLink to="/projects" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Projects
        </NavLink>
        <NavLink to="/tasks/new" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Create Task
        </NavLink>
        {hasToken ? (
          <div className="navbar-session">
            <div className="navbar-user" aria-label="Current user">
              <span className="navbar-user-name">{currentUser?.username || 'User'}</span>
              <span className="navbar-user-role">{currentUser?.displayRole || 'USER'}</span>
            </div>
            <button className="nav-button" type="button" onClick={handleLogout}>
              Logout
            </button>
          </div>
        ) : (
          <>
            <NavLink to="/login" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              Login
            </NavLink>
            <NavLink to="/register" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              Register
            </NavLink>
          </>
        )}
      </div>
    </nav>
  );
}

export default Navbar;
