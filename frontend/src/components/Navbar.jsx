import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

function Navbar() {
  const [hasToken, setHasToken] = useState(() => Boolean(localStorage.getItem('token')));
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const syncAuthState = () => {
      setHasToken(Boolean(localStorage.getItem('token')));
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
        <NavLink to="/tasks/new" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Create Task
        </NavLink>
        {hasToken ? (
          <button className="nav-button" type="button" onClick={handleLogout}>
            Logout
          </button>
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
