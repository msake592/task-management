import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createTask } from '../api/taskApi';

const initialFormData = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  projectId: '',
};

function CreateTaskPage() {
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

    if (!formData.title.trim()) {
      setError('Title is required.');
      return;
    }

    const taskData = {
      ...formData,
      title: formData.title.trim(),
      description: formData.description.trim(),
      dueDate: formData.dueDate || null,
      projectId: formData.projectId ? Number(formData.projectId) : null,
    };

    try {
      setLoading(true);
      setError('');
      await createTask(taskData);
      navigate('/tasks');
    } catch (err) {
      setError('Task could not be created. Please check the form or backend response.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <h1>Create Task</h1>
      <form className="task-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Title</span>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            required
            placeholder="Task title"
          />
        </label>

        <label className="form-field">
          <span>Description</span>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
            placeholder="Task description"
          />
        </label>

        <div className="form-row">
          <label className="form-field">
            <span>Status</span>
            <select name="status" value={formData.status} onChange={handleChange}>
              <option value="TODO">TODO</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="DONE">DONE</option>
            </select>
          </label>

          <label className="form-field">
            <span>Priority</span>
            <select name="priority" value={formData.priority} onChange={handleChange}>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
            </select>
          </label>
        </div>

        <div className="form-row">
          <label className="form-field">
            <span>Due date</span>
            <input type="date" name="dueDate" value={formData.dueDate} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>Project ID</span>
            <input
              type="number"
              name="projectId"
              value={formData.projectId}
              onChange={handleChange}
              min="1"
              placeholder="Project ID"
            />
          </label>
        </div>

        {error && <p className="error-message">{error}</p>}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create Task'}
        </button>
      </form>
    </section>
  );
}

export default CreateTaskPage;
