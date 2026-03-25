import React from 'react';
import dayjs from 'dayjs';
import { Typography } from '@alfalab/core-components-typography';

function isBirthdayToday(birthDate) {
  const today = new Date();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  return birthDate.slice(5) === `${mm}-${dd}`;
}

function daysUntilBirthday(dateStr) {
  const today = dayjs().startOf('day');
  let bd = dayjs(dateStr).year(today.year());
  if (bd.isBefore(today, 'day')) {
    bd = bd.year(today.year() + 1);
  }
  return bd.diff(today, 'day');
}

export default function StatsCards({ employees }) {
  const birthdayPeople = employees.filter((e) => isBirthdayToday(e.birthDate));
  const total = employees.length;

  // Find nearest upcoming birthday if no one today
  let nearest = null;
  if (birthdayPeople.length === 0 && employees.length > 0) {
    let minDays = Infinity;
    for (const emp of employees) {
      const d = daysUntilBirthday(emp.birthDate);
      if (d > 0 && d < minDays) {
        minDays = d;
        nearest = { name: emp.fullName, days: d };
      }
    }
  }

  if (birthdayPeople.length > 0) {
    return (
      <div className="birthday-banner birthday-banner--active">
        <div className="birthday-banner-icon">🎂</div>
        <div className="birthday-banner-content">
          <Typography.Title tag="div" view="xsmall" color="static-primary-light">
            Сегодня день рождения!
          </Typography.Title>
          <div className="birthday-banner-names">
            {birthdayPeople.map((emp, i) => (
              <span key={emp.id} className="birthday-banner-person">
                <Typography.Text view="primary-medium" weight="bold" color="static-primary-light">
                  {emp.fullName}
                </Typography.Text>
                {emp.position && (
                  <Typography.Text view="primary-small" color="static-primary-light" className="birthday-banner-position">
                    {' — '}{emp.position}
                    {emp.department ? `, ${emp.department}` : ''}
                  </Typography.Text>
                )}
                {i < birthdayPeople.length - 1 && <span className="birthday-banner-separator" />}
              </span>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="birthday-banner birthday-banner--empty">
      <div className="birthday-banner-content">
        <Typography.Text view="primary-medium" color="secondary">
          Сегодня именинников нет
        </Typography.Text>
        {nearest && (
          <Typography.Text view="primary-small" color="tertiary">
            Ближайший: {nearest.name} — через {nearest.days} {nearest.days === 1 ? 'день' : nearest.days < 5 ? 'дня' : 'дней'}
          </Typography.Text>
        )}
      </div>
      {total > 0 && (
        <Typography.Text view="secondary-medium" color="tertiary">
          Всего сотрудников: {total}
        </Typography.Text>
      )}
    </div>
  );
}
