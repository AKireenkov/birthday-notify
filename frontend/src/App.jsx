import React, { useState, useEffect, useCallback } from 'react';
import dayjs from 'dayjs';
import { Notification } from '@alfalab/core-components-notification';
import { Typography } from '@alfalab/core-components-typography';
import { ButtonDesktop as Button } from '@alfalab/core-components-button/desktop';
import { Divider } from '@alfalab/core-components-divider';
import {
  getEmployees,
  createEmployee,
  updateEmployee,
  deleteEmployee,
  uploadCsv,
  sendTestEmail,
  syncFromFolder,
} from './api/api';
import StatsCards from './components/StatsCards';
import Toolbar from './components/Toolbar';
import EmployeeTable from './components/EmployeeTable';
import EmployeeModal from './components/EmployeeModal';
import CsvUploadModal from './components/CsvUploadModal';

function isBirthdayToday(birthDate) {
  const today = new Date();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  return birthDate.slice(5) === `${mm}-${dd}`;
}

export default function App() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [csvModalOpen, setCsvModalOpen] = useState(false);
  const [showBirthdaysOnly, setShowBirthdaysOnly] = useState(false);
  const [actionsOpen, setActionsOpen] = useState(false);
  const [sendingEmail, setSendingEmail] = useState(false);
  const [syncing, setSyncing] = useState(false);

  const [notification, setNotification] = useState({ visible: false, title: '', badge: 'positive-checkmark' });

  const birthdayCount = employees.filter((e) => isBirthdayToday(e.birthDate)).length;

  const showNotification = useCallback((title, badge = 'positive-checkmark') => {
    setNotification({ visible: true, title, badge });
  }, []);

  const hideNotification = useCallback(() => {
    setNotification((n) => ({ ...n, visible: false }));
  }, []);

  useEffect(() => {
    setLoading(true);
    getEmployees()
      .then(setEmployees)
      .catch(() => showNotification('Не удалось загрузить сотрудников', 'negative-cross'))
      .finally(() => setLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = employees.filter((e) =>
    e.fullName.toLowerCase().includes(search.toLowerCase())
  );

  const sorted = [...filtered].sort((a, b) => {
    const aBday = isBirthdayToday(a.birthDate);
    const bBday = isBirthdayToday(b.birthDate);
    if (aBday && !bBday) return -1;
    if (!aBday && bBday) return 1;
    return a.fullName.localeCompare(b.fullName, 'ru');
  });

  const handleAdd = useCallback(() => {
    setEditingEmployee(null);
    setModalOpen(true);
  }, []);

  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
        e.preventDefault();
        handleAdd();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [handleAdd]);

  const handleEdit = (emp) => {
    setEditingEmployee(emp);
    setModalOpen(true);
  };

  const handleSave = async (values) => {
    if (editingEmployee) {
      try {
        const updated = await updateEmployee(editingEmployee.id, values);
        setEmployees((prev) =>
          prev.map((e) => (e.id === editingEmployee.id ? updated : e))
        );
        showNotification('Данные обновлены');
        setModalOpen(false);
      } catch {
        showNotification('Не удалось обновить данные', 'negative-cross');
      }
    } else {
      try {
        const emp = await createEmployee(values);
        setEmployees((prev) => [...prev, emp]);
        showNotification(`${emp.fullName} добавлен(а)`);
        setModalOpen(false);
      } catch {
        showNotification('Не удалось добавить сотрудника', 'negative-cross');
      }
    }
  };

  const handleSaveAndAddAnother = async (values) => {
    try {
      const emp = await createEmployee(values);
      setEmployees((prev) => [...prev, emp]);
      showNotification(`${emp.fullName} добавлен(а)`);
    } catch {
      showNotification('Не удалось добавить сотрудника', 'negative-cross');
    }
  };

  const handleDelete = async (emp) => {
    try {
      await deleteEmployee(emp.id);
      setEmployees((prev) => prev.filter((e) => e.id !== emp.id));
      showNotification(`${emp.fullName} удален(а)`);
    } catch {
      showNotification('Не удалось удалить сотрудника', 'negative-cross');
    }
  };

  const handleCsvUpload = async (file) => {
    try {
      const result = await uploadCsv(file);
      const fresh = await getEmployees();
      setEmployees(fresh);
      showNotification(`CSV загружен: ${result.imported} сотрудник(ов)`);
      return result;
    } catch {
      showNotification('Не удалось загрузить CSV', 'negative-cross');
      return null;
    }
  };

  const handleSendTest = async () => {
    setSendingEmail(true);
    try {
      await sendTestEmail();
      showNotification('Письмо отправлено');
    } catch {
      showNotification('Не удалось отправить письмо', 'negative-cross');
    } finally {
      setSendingEmail(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      await syncFromFolder();
      const fresh = await getEmployees();
      setEmployees(fresh);
      showNotification('Синхронизация завершена');
    } catch {
      showNotification('Ошибка синхронизации', 'negative-cross');
    } finally {
      setSyncing(false);
    }
  };

  const todayFormatted = dayjs().format('D MMMM');

  return (
    <div style={{ minHeight: '100vh', background: 'var(--color-light-bg-secondary)' }}>
      <Notification
        visible={notification.visible}
        badge={notification.badge}
        title={notification.title}
        autoCloseDelay={3000}
        onCloseTimeout={hideNotification}
        onClickOutside={hideNotification}
        position="bottom"
      />

      <header className="app-header">
        <div className="app-header-left">
          <Typography.Title tag="h1" view="medium" color="static-primary-light" className="app-title">
            Alfa Birthday
          </Typography.Title>
          <Typography.Text view="primary-small" color="static-primary-light" className="app-subtitle">
            {todayFormatted}
          </Typography.Text>
        </div>
        <div className="app-header-right">
          <Button
            view="primary"
            size={40}
            className="header-actions-btn"
            onClick={() => setActionsOpen((v) => !v)}
          >
            Действия
          </Button>
        </div>
      </header>

      {/* Slide-down action panel */}
      {actionsOpen && (
        <>
          <div className="actions-backdrop" onClick={() => setActionsOpen(false)} />
          <div className="actions-panel">
            <Typography.Title tag="div" view="xsmall" style={{ marginBottom: 8 }}>
              Действия
            </Typography.Title>
            <Button view="accent" size={48} block onClick={() => { handleAdd(); setActionsOpen(false); }}>
              + Добавить сотрудника
              <span className="shortcut-hint">Ctrl+N</span>
            </Button>
            <Button view="secondary" size={48} block onClick={() => { setCsvModalOpen(true); setActionsOpen(false); }}>
              Загрузить CSV
            </Button>
            <Divider />
            <Button view="secondary" size={48} block onClick={handleSendTest} loading={sendingEmail} disabled={sendingEmail}>
              Отправить письмо вручную
            </Button>
            <Button view="secondary" size={48} block onClick={handleSync} loading={syncing} disabled={syncing}>
              Обновить из папки
            </Button>
          </div>
        </>
      )}

      <main className="app-content">
        <StatsCards employees={employees} />

        <Toolbar
          search={search}
          onSearchChange={setSearch}
          showBirthdaysOnly={showBirthdaysOnly}
          onToggleBirthdays={() => setShowBirthdaysOnly((v) => !v)}
        />

        <EmployeeTable
          employees={sorted}
          loading={loading}
          onEdit={handleEdit}
          onDelete={handleDelete}
          hasSearch={search.length > 0}
          showBirthdaysOnly={showBirthdaysOnly}
          onAdd={handleAdd}
          onOpenCsv={() => setCsvModalOpen(true)}
        />

        <EmployeeModal
          open={modalOpen}
          employee={editingEmployee}
          onSave={handleSave}
          onSaveAndAddAnother={handleSaveAndAddAnother}
          onCancel={() => setModalOpen(false)}
        />

        <CsvUploadModal
          open={csvModalOpen}
          onUpload={handleCsvUpload}
          onCancel={() => setCsvModalOpen(false)}
        />
      </main>
    </div>
  );
}
