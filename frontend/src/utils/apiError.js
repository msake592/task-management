export function getApiErrorMessage(error, fallbackMessage) {
  const responseData = error?.response?.data;

  if (responseData?.validationErrors) {
    const firstValidationMessage = Object.values(responseData.validationErrors)[0];
    if (firstValidationMessage) {
      return firstValidationMessage;
    }
  }

  return responseData?.message || fallbackMessage;
}
