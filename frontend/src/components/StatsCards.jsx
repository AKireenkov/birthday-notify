import React from 'react';
import { Row, Col, Card, Statistic } from 'antd';
import { TeamOutlined, GiftOutlined, CalendarOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

function isBirthdayToday(birthDate) {
  const today = new Date();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  return birthDate.slice(5) === `${mm}-${dd}`;
}

export default function StatsCards({ employees }) {
  const total = employees.length;
  const birthdayCount = employees.filter((e) => isBirthdayToday(e.birthDate)).length;
  const todayFormatted = dayjs().format('DD MMMM YYYY');

  const birthdayCardStyle = birthdayCount > 0
    ? { borderColor: '#fa541c', background: '#fff2e8' }
    : {};

  return (
    <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
      <Col xs={24} sm={8}>
        <Card className="stat-card" bordered={false}>
          <Statistic
            title="Всего сотрудников"
            value={total}
            prefix={<TeamOutlined />}
            valueStyle={{ color: '#1677ff' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card className="stat-card" bordered={birthdayCount > 0} style={birthdayCardStyle}>
          <Statistic
            title="Дни рождения сегодня"
            value={birthdayCount}
            prefix={
              <>
                <GiftOutlined />
                {birthdayCount > 0 && <span style={{ marginLeft: 4 }}>&#127881;</span>}
              </>
            }
            valueStyle={{ color: birthdayCount > 0 ? '#fa541c' : '#8c8c8c' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card className="stat-card" bordered={false}>
          <Statistic
            title="Сегодня"
            value={todayFormatted}
            prefix={<CalendarOutlined />}
            valueStyle={{ color: '#52c41a', fontSize: 20 }}
          />
        </Card>
      </Col>
    </Row>
  );
}
