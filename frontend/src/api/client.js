/**
 * Backwards-compat shim. Older modules imported a default axios-like client from
 * '../api/client'. We now use a fetch-based service layer in 'src/services/'.
 *
 * This file re-exports a tiny axios-shaped facade so legacy imports still work.
 * New code should import from '../services/*' directly.
 */
import { http } from '../services/http';

const wrap = (promise) => promise.then((data) => ({ data }));

const client = {
  get:  (path)        => wrap(http.get(path)),
  post: (path, body)  => wrap(http.post(path, body)),
  put:  (path, body)  => wrap(http.put(path, body)),
  delete: (path)      => wrap(http.del(path)),
};

export default client;
