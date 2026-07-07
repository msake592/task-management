import { Link } from 'react-router-dom';
import { isCurrentUserAdmin } from '../utils/authToken';

function HomePage() {
  const isAdmin = isCurrentUserAdmin();

  return (
    <section className="page">
      <h1>Task Management Frontend</h1>
      <p>Manage and view tasks from the backend API.</p>
      {isAdmin && (
        <Link className="primary-button" to="/admin/users">
          User Management
        </Link>
      )}
    </section>
  );
}

export default HomePage;
