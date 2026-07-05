import { Link } from 'react-router-dom';

function TaskCard({ task }) {
  const taskId = task?.id;
  const title = task?.title || 'Untitled task';
  const description = task?.description || 'No description';
  const status = task?.status || 'No status';
  const priority = task?.priority || 'No priority';
  const dueDate = task?.dueDate || 'No due date';
  const projectName = task?.projectName || task?.project?.name || 'No project';
  const assigneeNames = Array.isArray(task?.assignees)
    ? task.assignees.map((assignee) => assignee.name || assignee.email).filter(Boolean)
    : [];

  return (
    <article className="task-card">
      <div className="task-card-header">
        <h2>{title}</h2>
        <span className="task-status">{status}</span>
      </div>
      <p>{description}</p>
      <div className="task-meta">
        <span>Project: {projectName}</span>
        <span>Priority: {priority}</span>
        <span>Due: {dueDate}</span>
        <span>Assignees: {assigneeNames.length ? assigneeNames.join(', ') : 'Unassigned'}</span>
      </div>
      {taskId ? (
        <Link className="secondary-button" to={`/tasks/${taskId}`}>
          View Details
        </Link>
      ) : (
        <button className="secondary-button" type="button" disabled>
          View Details
        </button>
      )}
    </article>
  );
}

export default TaskCard;
