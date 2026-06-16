/**
 * Utilitaire de réponse HTTP standardisée.
 * Format imposé : { error: <code_erreur>, status: <http_status>, data: <payload> }
 *
 * - error = 0  : succès
 * - error > 0  : code d'erreur applicatif
 * - status     : code HTTP standard (200, 201, 400, 401, 404, 500...)
 * - data       : le payload (objet, tableau, message string)
 */

const sendSuccess = (res, data, status = 200) => {
  return res.status(status).json({ error: 0, status, data });
};

const sendError = (res, message, status = 500, errorCode = 1) => {
  return res.status(status).json({ error: errorCode, status, data: message });
};

module.exports = { sendSuccess, sendError };
