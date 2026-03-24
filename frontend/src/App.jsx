import React, { useState, useEffect, useCallback } from 'react';
import { Layout, message, Badge } from 'antd';
import { GiftOutlined } from '@ant-design/icons';
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

const { Header, Content } = Layout;

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
  const [messageApi, contextHolder] = message.useMessage();

  const birthdayCount = employees.filter((e) => isBirthdayToday(e.birthDate)).length;

  // Load employees on mount
  useEffect(() => {
    setLoading(true);
    getEmployees()
      .then(setEmployees)
      .catch(() => messageApi.error('Не удалось загрузить сотрудников'))
      .finally(() => setLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Filtered list
  const filtered = employees.filter((e) =>
    e.fullName.toLowerCase().includes(search.toLowerCase())
  );

  // Sort: birthday today first, then alphabetically
  const sorted = [...filtered].sort((a, b) => {
    const now = new Date();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const todayMmDd = `${mm}-${dd}`;
    const aBday = a.birthDate.slice(5) === todayMmDd;
    const bBday = b.birthDate.slice(5) === todayMmDd;
    if (aBday && !bBday) return -1;
    if (!aBday && bBday) return 1;
    return a.fullName.localeCompare(b.fullName, 'ru');
  });

  // Add
  const handleAdd = useCallback(() => {
    setEditingEmployee(null);
    setModalOpen(true);
  }, []);

  // Keyboard shortcut Ctrl+N / Cmd+N — Rec #9
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

  // Edit
  const handleEdit = (emp) => {
    setEditingEmployee(emp);
    setModalOpen(true);
  };

  // Save (add or update)
  const handleSave = async (values) => {
    if (editingEmployee) {
      try {
        const updated = await updateEmployee(editingEmployee.id, values);
        setEmployees((prev) =>
          prev.map((e) => (e.id === editingEmployee.id ? updated : e))
        );
        messageApi.success('Данные обновлены');
        setModalOpen(false);
      } catch {
        messageApi.error('Не удалось обновить данные');
      }
    } else {
      try {
        const emp = await createEmployee(values);
        setEmployees((prev) => [...prev, emp]);
        messageApi.success(`${emp.fullName} добавлен(а)`);
        setModalOpen(false);
      } catch {
        messageApi.error('Не удалось добавить сотрудника');
      }
    }
  };

  // Save and add another — Rec #8
  const handleSaveAndAddAnother = async (values) => {
    try {
      const emp = await createEmployee(values);
      setEmployees((prev) => [...prev, emp]);
      messageApi.success(`${emp.fullName} добавлен(а)`);
      // modal stays open, form is reset inside EmployeeModal
    } catch {
      messageApi.error('Не удалось добавить сотрудника');
    }
  };

  // Delete
  const handleDelete = async (emp) => {
    try {
      await deleteEmployee(emp.id);
      setEmployees((prev) => prev.filter((e) => e.id !== emp.id));
      messageApi.success(`${emp.fullName} удален(а)`);
    } catch {
      messageApi.error('Не удалось удалить сотрудника');
    }
  };

  // CSV upload — Rec #5: return result for summary display
  const handleCsvUpload = async (file) => {
    try {
      const result = await uploadCsv(file);
      const fresh = await getEmployees();
      setEmployees(fresh);
      messageApi.success(`CSV загружен: ${result.imported} сотрудник(ов) добавлено`);
      return result; // returned to CsvUploadModal for summary display
    } catch {
      messageApi.error('Не удалось загрузить CSV');
      return null;
    }
  };

  // Test email
  const handleSendTest = async () => {
    try {
      messageApi.loading({ content: 'Отправка тестового письма...', key: 'test-email' });
      await sendTestEmail();
      messageApi.success({ content: 'Тестовое письмо отправлено', key: 'test-email' });
    } catch {
      messageApi.error({ content: 'Не удалось отправить письмо', key: 'test-email' });
    }
  };

  // Sync
  const handleSync = async () => {
    try {
      messageApi.loading({ content: 'Синхронизация...', key: 'sync' });
      await syncFromFolder();
      const fresh = await getEmployees();
      setEmployees(fresh);
      messageApi.success({ content: 'Синхронизация завершена', key: 'sync' });
    } catch {
      messageApi.error({ content: 'Ошибка синхронизации', key: 'sync' });
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {contextHolder}
      <Header className="app-header" style={{ height: 'auto', lineHeight: 'normal' }}>
        <div>
          <h1>&#127874; Alfa Birthday</h1>
          <p>Мониторинг дней рождения сотрудников</p>
        </div>
        {birthdayCount > 0 && (
          <Badge count={birthdayCount} style={{ backgroundColor: '#fa541c' }}>
            <GiftOutlined style={{ fontSize: 24, color: 'white' }} />
          </Badge>
        )}
      </Header>

      <Content className="app-content">
        <StatsCards employees={employees} />

        <Toolbar
          onAdd={handleAdd}
          onUploadCsv={() => setCsvModalOpen(true)}
          onSendTest={handleSendTest}
          onSync={handleSync}
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
      </Content>
    </Layout>
  );
}
