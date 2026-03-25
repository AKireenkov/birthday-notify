import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import StatsCards from '../components/StatsCards';

// Helper to make a date string with today's month/day
function todayBirthDate(year = 1990) {
  const now = new Date();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  return `${year}-${mm}-${dd}`;
}

describe('StatsCards', () => {
  it('shows empty message when no employees', () => {
    render(<StatsCards employees={[]} />);
    expect(screen.getByText('Сегодня именинников нет')).toBeInTheDocument();
  });

  it('shows empty message when no birthdays today', () => {
    const employees = [
      { id: 1, fullName: 'Иванов И.И.', birthDate: '1990-07-15', position: 'Dev', department: 'IT' },
    ];
    render(<StatsCards employees={employees} />);
    expect(screen.getByText('Сегодня именинников нет')).toBeInTheDocument();
    expect(screen.getByText(/Всего сотрудников: 1/)).toBeInTheDocument();
  });

  it('shows birthday banner when someone has birthday today', () => {
    const employees = [
      { id: 1, fullName: 'Петрова А.С.', birthDate: todayBirthDate(), position: 'HR', department: 'HR' },
    ];
    render(<StatsCards employees={employees} />);
    expect(screen.getByText('Сегодня день рождения!')).toBeInTheDocument();
    expect(screen.getByText('Петрова А.С.')).toBeInTheDocument();
  });

  it('shows multiple birthday people', () => {
    const employees = [
      { id: 1, fullName: 'Петрова А.С.', birthDate: todayBirthDate(), position: 'HR', department: 'HR' },
      { id: 2, fullName: 'Иванов И.И.', birthDate: todayBirthDate(1985), position: 'Dev', department: 'IT' },
      { id: 3, fullName: 'Сидоров К.К.', birthDate: '1992-01-01', position: '', department: '' },
    ];
    render(<StatsCards employees={employees} />);
    expect(screen.getByText('Петрова А.С.')).toBeInTheDocument();
    expect(screen.getByText('Иванов И.И.')).toBeInTheDocument();
    // Both birthday people should be listed
    expect(screen.queryByText('Сидоров К.К.')).not.toBeInTheDocument(); // not a birthday person
  });

  it('shows position and department for birthday person', () => {
    const employees = [
      { id: 1, fullName: 'Петрова А.С.', birthDate: todayBirthDate(), position: 'Менеджер', department: 'HR' },
    ];
    render(<StatsCards employees={employees} />);
    expect(screen.getByText(/Менеджер/)).toBeInTheDocument();
    expect(screen.getByText(/HR/)).toBeInTheDocument();
  });

  it('shows nearest birthday when no one today', () => {
    // Create employee with birthday tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const dd = String(tomorrow.getDate()).padStart(2, '0');
    const employees = [
      { id: 1, fullName: 'Завтрашний И.И.', birthDate: `1990-${mm}-${dd}`, position: '', department: '' },
    ];
    render(<StatsCards employees={employees} />);
    expect(screen.getByText('Сегодня именинников нет')).toBeInTheDocument();
    expect(screen.getByText(/Ближайший: Завтрашний И.И./)).toBeInTheDocument();
  });
});
