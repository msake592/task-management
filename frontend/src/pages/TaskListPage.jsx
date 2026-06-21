import { useEffect, useState } from 'react';
import { getTasks } from '../api/taskApi';
import TaskCard from '../components/TaskCard.jsx';

function normalizeTasks(data) {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  return [];
}

function TaskListPage() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadTasks = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getTasks();
        setTasks(normalizeTasks(data));
      } catch (err) {
        setError('Tasks could not be loaded. Please check that the backend is running.');
      } finally {
        setLoading(false);
      }
    };

    loadTasks();
  }, []);

  if (loading) {
    return <section className="page">Loading tasks...</section>;
  }

  if (error) {
    return (
      <section className="page">
        <h1>Tasks</h1>
        <p className="error-message">{error}</p>
      </section>
    );
  }

  return (
    <section className="page">
      <h1>Tasks</h1>
      {tasks.length === 0 ? (
        <p>No tasks found.</p>
      ) : (
        <div className="task-list">
          {tasks.map((task, index) => (
            <TaskCard key={task?.id || index} task={task} />
          ))}
        </div>
      )}
    </section>
  );
}

export default TaskListPage;
