import React from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import EmployeeTable from '../components/EmployeeTable';

function todayBirthDate(year = 1990) {
  const now = new Date();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  return `${year}-${mm}-${dd}`;
}

const defaultProps = {
  employees: [],
  loading: false,
  onEdit: vi.fn(),
  onDelete: vi.fn(),
  hasSearch: false,
  showBirthdaysOnly: false,
  onAdd: vi.fn(),
  onOpenCsv: vi.fn(),
};

describe('EmployeeTable', () => {
  it('shows loading state', () => {
    render(<EmployeeTable {...defaultProps} loading={true} />);
    expect(screen.getByText('Загрузка...')).toBeInTheDocument();
  });

  it('shows onboarding empty state with CTA buttons when no data', () => {
    render(<EmployeeTable {...defaultProps} />);
    expect(screen.getByText('Список сотрудников пуст')).toBeInTheDocument();
    expect(screen.getByText('+ Добавить сотрудника')).toBeInTheDocument();
    expect(screen.getByText('Загрузить CSV')).toBeInTheDocument();
  });

  it('shows search empty state when no results', () => {
    render(<EmployeeTable {...defaultProps} hasSearch={true} />);
    expect(screen.getByText('Сотрудники не найдены')).toBeInTheDocument();
  });

  it('renders employee rows', () => {
    const employees = [
      { id: 1, fullName: 'Иванов И.И.', birthDate: '1990-05-15', position: 'Dev', department: 'IT' },
      { id: 2, fullName: 'Петров П.П.', birthDate: '1985-03-25', position: 'QA', department: 'QA' },
    ];
    render(<EmployeeTable {...defaultProps} employees={employees} />);
    expect(screen.getByText('Иванов И.И.')).toBeInTheDocument();
    expect(screen.getByText('Петров П.П.')).toBeInTheDocument();
    expect(screen.getByText('Dev')).toBeInTheDocument();
    expect(screen.getAllByText('QA').length).toBeGreaterThanOrEqual(1);
  });

  it('shows "Сегодня" status for birthday employees', () => {
    const employees = [
      { id: 1, fullName: 'Именинник А.А.', birthDate: todayBirthDate(), position: 'Dev', department: 'IT' },
    ];
    render(<EmployeeTable {...defaultProps} employees={employees} />);
    expect(screen.getByText('Сегодня')).toBeInTheDocument();
  });

  it('shows dash for empty position/department', () => {
    const employees = [
      { id: 1, fullName: 'Без должности', birthDate: '1990-01-15', position: '', department: '' },
    ];
    render(<EmployeeTable {...defaultProps} employees={employees} />);
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBe(2);
  });

  it('calls onAdd when CTA button clicked in empty state', async () => {
    const user = userEvent.setup();
    const onAdd = vi.fn();
    render(<EmployeeTable {...defaultProps} onAdd={onAdd} />);
    await user.click(screen.getByText('+ Добавить сотрудника'));
    expect(onAdd).toHaveBeenCalledOnce();
  });

  it('calls onOpenCsv when CSV button clicked in empty state', async () => {
    const user = userEvent.setup();
    const onOpenCsv = vi.fn();
    render(<EmployeeTable {...defaultProps} onOpenCsv={onOpenCsv} />);
    await user.click(screen.getByText('Загрузить CSV'));
    expect(onOpenCsv).toHaveBeenCalledOnce();
  });

  it('has edit and delete buttons with titles', () => {
    const employees = [
      { id: 1, fullName: 'Тест Т.Т.', birthDate: '1990-01-15', position: 'Dev', department: 'IT' },
    ];
    render(<EmployeeTable {...defaultProps} employees={employees} />);
    expect(screen.getByTitle('Редактировать')).toBeInTheDocument();
    expect(screen.getByTitle('Удалить')).toBeInTheDocument();
  });

  it('shows delete confirmation modal when delete clicked', async () => {
    const user = userEvent.setup();
    const employees = [
      { id: 1, fullName: 'Удаляемый С.С.', birthDate: '1990-01-15', position: '', department: '' },
    ];
    render(<EmployeeTable {...defaultProps} employees={employees} />);
    await user.click(screen.getByTitle('Удалить'));
    expect(screen.getByText(/Вы уверены, что хотите удалить/)).toBeInTheDocument();
    expect(screen.getByText('Удаляемый С.С.')).toBeInTheDocument();
  });

  it('filters to birthday-only when showBirthdaysOnly is true', () => {
    const employees = [
      { id: 1, fullName: 'Именинник', birthDate: todayBirthDate(), position: '', department: '' },
      { id: 2, fullName: 'Не именинник', birthDate: '1990-07-15', position: '', department: '' },
    ];
    render(<EmployeeTable {...defaultProps} employees={employees} showBirthdaysOnly={true} />);
    expect(screen.getByText('Именинник')).toBeInTheDocument();
    expect(screen.queryByText('Не именинник')).not.toBeInTheDocument();
  });
});
