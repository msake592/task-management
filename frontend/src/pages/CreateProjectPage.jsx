import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createProject } from '../api/projectApi';
import { getApiErrorMessage } from '../utils/apiError';

const initialFormData = {
  name: '',
  description: '',
  startDate: '',
  endDate: '',
};

function CreateProjectPage() {
  const [formData, setFormData] = useState(initialFormData);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!formData.name.trim()) {
      setError('Project name is required.');
      return;
    }

    const projectData = {
      name: formData.name.trim(),
      description: formData.description.trim(),
      startDate: formData.startDate || null,
      endDate: formData.endDate || null,
    };

    try {
      setLoading(true);
      setError('');
      await createProject(projectData);
      navigate('/projects');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Project could not be created.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <h1>New Project</h1>
      <form className="task-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Name</span>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            placeholder="Project name"
          />
        </label>

        <label className="form-field">
          <span>Description</span>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
            placeholder="Project description"
          />
        </label>

        <div className="form-row">
          <label className="form-field">
            <span>Start date</span>
            <input type="date" name="startDate" value={formData.startDate} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>End date</span>
            <input type="date" name="endDate" value={formData.endDate} onChange={handleChange} />
          </label>
        </div>

        {error && <p className="error-message">{error}</p>}

        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Creating...' : 'Create Project'}
          </button>
          <Link className="secondary-button" to="/projects">
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}

export default CreateProjectPage;
