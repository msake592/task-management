export const TASK_DEADLINE_ERROR = 'Task tarihi proje tarih aralığı içinde olmalıdır.';

export function isTaskDeadlineWithinProject(project, dueDate) {
  if (!project || !dueDate) {
    return true;
  }

  return (!project.startDate || dueDate >= project.startDate)
    && (!project.endDate || dueDate <= project.endDate);
}
