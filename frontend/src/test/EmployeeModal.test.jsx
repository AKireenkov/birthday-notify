import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import EmployeeModal from '../components/EmployeeModal';

const defaultProps = {
  open: true,
  employee: null,
  onSave: vi.fn(),
  onSaveAndAddAnother: vi.fn(),
  onCancel: vi.fn(),
};

describe('EmployeeModal', () => {
  it('renders add mode title', () => {
    render(<EmployeeModal {...defaultProps} />);
    expect(screen.getByText('Добавить сотрудника')).toBeInTheDocument();
  });

  it('renders edit mode title with employee data', () => {
    const employee = {
      id: 1,
      fullName: 'Иванов И.И.',
      birthDate: '1990-05-15',
      position: 'Dev',
      department: 'IT',
    };
    render(<EmployeeModal {...defaultProps} employee={employee} />);
    expect(screen.getByText('Редактировать сотрудника')).toBeInTheDocument();
  });

  it('shows validation error for empty name', async () => {
    const user = userEvent.setup();
    render(<EmployeeModal {...defaultProps} />);
    await user.click(screen.getByText('Добавить'));
    expect(screen.getByText('Введите ФИО сотрудника')).toBeInTheDocument();
  });

  it('shows validation error for empty birth date', async () => {
    const user = userEvent.setup();
    render(<EmployeeModal {...defaultProps} />);
    // Fill name but leave date empty
    const nameInput = screen.getByPlaceholderText('Иванов Иван Иванович');
    await user.type(nameInput, 'Тест');
    await user.click(screen.getByText('Добавить'));
    expect(screen.getByText('Выберите дату рождения')).toBeInTheDocument();
  });

  it('has date input with min and max constraints', () => {
    render(<EmployeeModal {...defaultProps} />);
    const dateInput = screen.getByLabelText('Дата рождения');
    expect(dateInput).toHaveAttribute('min', '1920-01-01');
    expect(dateInput).toHaveAttribute('max');
    // max should be today's date
    const today = new Date().toISOString().split('T')[0];
    expect(dateInput.getAttribute('max')).toBe(today);
  });

  it('shows "Сохранить и добавить ещё" only in add mode', () => {
    render(<EmployeeModal {...defaultProps} />);
    expect(screen.getByText('Сохранить и добавить ещё')).toBeInTheDocument();
  });

  it('hides "Сохранить и добавить ещё" in edit mode', () => {
    const employee = {
      id: 1,
      fullName: 'Иванов И.И.',
      birthDate: '1990-05-15',
      position: 'Dev',
      department: 'IT',
    };
    render(<EmployeeModal {...defaultProps} employee={employee} />);
    expect(screen.queryByText('Сохранить и добавить ещё')).not.toBeInTheDocument();
  });

  it('calls onCancel when Отмена clicked', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(<EmployeeModal {...defaultProps} onCancel={onCancel} />);
    await user.click(screen.getByText('Отмена'));
    expect(onCancel).toHaveBeenCalledOnce();
  });
});
