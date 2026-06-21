import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getProjects } from '../api/projectApi';
import { getApiErrorMessage } from '../utils/apiError';

function normalizeProjects(data) {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  return [];
}

function ProjectListPage() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadProjects = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getProjects();
        setProjects(normalizeProjects(data));
      } catch (err) {
        setError(getApiErrorMessage(err, 'Projects could not be loaded.'));
      } finally {
        setLoading(false);
      }
    };

    loadProjects();
  }, []);

  if (loading) {
    return <section className="page">Loading projects...</section>;
  }

  return (
    <section className="page">
      <div className="page-header">
        <h1>Projects</h1>
        <Link className="primary-button" to="/projects/new">
          New Project
        </Link>
      </div>

      {error && <p className="error-message">{error}</p>}

      {!error && projects.length === 0 ? (
        <p className="empty-message">No projects available. Create a project before adding tasks.</p>
      ) : (
        <div className="project-list">
          {projects.map((project) => (
            <article className="project-card" key={project.id}>
              <h2>{project.name}</h2>
              <p>{project.description || 'No description'}</p>
              <div className="task-meta">
                <span>Start: {project.startDate || 'No start date'}</span>
                <span>End: {project.endDate || 'No end date'}</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default ProjectListPage;
