/**
 * MSW browser setup — creates the service worker instance for development.
 */
import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);
