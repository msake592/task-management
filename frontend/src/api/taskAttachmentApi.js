import axiosInstance from './axiosInstance';

const getAttachmentsEndpoint = (taskId) => `/api/tasks/${taskId}/attachments`;

export const getTaskAttachments = async (taskId) => {
  const response = await axiosInstance.get(getAttachmentsEndpoint(taskId));
  return response.data;
};

export const uploadTaskAttachment = async (taskId, file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosInstance.post(getAttachmentsEndpoint(taskId), formData);
  return response.data;
};

export const downloadTaskAttachment = async (taskId, attachmentId) => {
  const response = await axiosInstance.get(
    `${getAttachmentsEndpoint(taskId)}/${attachmentId}/download`,
    { responseType: 'blob' }
  );
  return response.data;
};
