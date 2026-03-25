import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock fetch before importing the module
const mockFetch = vi.fn();
global.fetch = mockFetch;

// Dynamic import after mock setup
let api;
beforeEach(async () => {
  vi.resetModules();
  mockFetch.mockReset();
  api = await import('../api/api.js');
});

function jsonResponse(data, status = 200) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(data),
  });
}

describe('API layer', () => {
  describe('getEmployees', () => {
    it('fetches /api/employees and returns data', async () => {
      const employees = [{ id: 1, fullName: 'Test' }];
      mockFetch.mockReturnValue(jsonResponse(employees));

      const result = await api.getEmployees();

      expect(mockFetch).toHaveBeenCalledWith('/api/employees');
      expect(result).toEqual(employees);
    });

    it('throws on error response', async () => {
      mockFetch.mockReturnValue(jsonResponse({ error: 'Server error' }, 500));

      await expect(api.getEmployees()).rejects.toThrow('Server error');
    });
  });

  describe('createEmployee', () => {
    it('posts to /api/employees with JSON body', async () => {
      const data = { fullName: 'Новый', birthDate: '1990-01-01' };
      mockFetch.mockReturnValue(jsonResponse({ id: 1, ...data }));

      const result = await api.createEmployee(data);

      expect(mockFetch).toHaveBeenCalledWith('/api/employees', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      expect(result.fullName).toBe('Новый');
    });
  });

  describe('updateEmployee', () => {
    it('puts to /api/employees/:id', async () => {
      const data = { fullName: 'Обновлённый', birthDate: '1990-01-01' };
      mockFetch.mockReturnValue(jsonResponse({ id: 5, ...data }));

      const result = await api.updateEmployee(5, data);

      expect(mockFetch).toHaveBeenCalledWith('/api/employees/5', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      expect(result.fullName).toBe('Обновлённый');
    });
  });

  describe('deleteEmployee', () => {
    it('deletes /api/employees/:id', async () => {
      mockFetch.mockReturnValue(Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve({}) }));

      const result = await api.deleteEmployee(3);

      expect(mockFetch).toHaveBeenCalledWith('/api/employees/3', { method: 'DELETE' });
      expect(result).toEqual({ success: true });
    });

    it('throws on error', async () => {
      mockFetch.mockReturnValue(jsonResponse({ error: 'Не найден' }, 404));

      await expect(api.deleteEmployee(999)).rejects.toThrow('Не найден');
    });
  });

  describe('uploadCsv', () => {
    it('posts FormData to /api/employees/upload', async () => {
      const file = new File(['data'], 'test.csv', { type: 'text/csv' });
      mockFetch.mockReturnValue(jsonResponse({ imported: 5, duplicatesSkipped: 0, errorsSkipped: 0 }));

      const result = await api.uploadCsv(file);

      expect(mockFetch).toHaveBeenCalledWith('/api/employees/upload', {
        method: 'POST',
        body: expect.any(FormData),
      });
      expect(result.imported).toBe(5);
    });
  });

  describe('sendTestEmail', () => {
    it('posts to /api/notify/send-test', async () => {
      mockFetch.mockReturnValue(jsonResponse({ message: 'Sent' }));

      const result = await api.sendTestEmail();

      expect(mockFetch).toHaveBeenCalledWith('/api/notify/send-test', { method: 'POST' });
      expect(result.message).toBe('Sent');
    });
  });

  describe('syncFromFolder', () => {
    it('posts to /api/sync/run', async () => {
      mockFetch.mockReturnValue(jsonResponse({ message: 'Done' }));

      const result = await api.syncFromFolder();

      expect(mockFetch).toHaveBeenCalledWith('/api/sync/run', { method: 'POST' });
      expect(result.message).toBe('Done');
    });
  });
});
