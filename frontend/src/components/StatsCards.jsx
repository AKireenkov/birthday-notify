import React from 'react';
import dayjs from 'dayjs';
import { Typography } from '@alfalab/core-components-typography';
import { Status } from '@alfalab/core-components-status';

function isBirthdayToday(birthDate) {
  const today = new Date();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  return birthDate.slice(5) === `${mm}-${dd}`;
}

export default function StatsCards({ employees }) {
  const total = employees.length;
  const birthdayCount = employees.filter((e) => isBirthdayToday(e.birthDate)).length;
  const todayFormatted = dayjs().format('D MMMM YYYY');

  return (
    <div className="stats-row">
      <div className="stat-card">
        <Typography.Text view="secondary-large" color="secondary">
          Всего сотрудников
        </Typography.Text>
        <div className="stat-card-value">
          <Typography.Title tag="div" view="small">
            {total}
          </Typography.Title>
        </div>
      </div>

      <div className={`stat-card ${birthdayCount > 0 ? 'stat-card--birthday' : ''}`}>
        <Typography.Text view="secondary-large" color="secondary">
          Дни рождения сегодня
        </Typography.Text>
        <div className="stat-card-value">
          <Typography.Title tag="div" view="small" color={birthdayCount > 0 ? 'negative' : undefined}>
            {birthdayCount}
          </Typography.Title>
          {birthdayCount > 0 && (
            <Status color="red" view="soft" size={24}>
              Есть именинники!
            </Status>
          )}
        </div>
      </div>

      <div className="stat-card">
        <Typography.Text view="secondary-large" color="secondary">
          Сегодня
        </Typography.Text>
        <div className="stat-card-value">
          <Typography.Title tag="div" view="small">
            {todayFormatted}
          </Typography.Title>
        </div>
      </div>
    </div>
  );
}
