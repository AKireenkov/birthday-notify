import React from 'react';
import { Row, Col, Button, Space, Input, Tooltip } from 'antd';
import {
  PlusOutlined,
  UploadOutlined,
  MailOutlined,
  SyncOutlined,
  GiftOutlined,
} from '@ant-design/icons';

const { Search } = Input;

export default function Toolbar({
  onAdd,
  onUploadCsv,
  onSendTest,
  onSync,
  search,
  onSearchChange,
  showBirthdaysOnly,
  onToggleBirthdays,
}) {
  return (
    <Row
      gutter={[16, 16]}
      justify="space-between"
      align="middle"
      style={{ marginBottom: 16 }}
    >
      <Col>
        <Space wrap>
          <Tooltip title="Добавить сотрудника (Ctrl+N)">
            <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
              Добавить
            </Button>
          </Tooltip>
          <Button icon={<UploadOutlined />} onClick={onUploadCsv}>
            Загрузить CSV
          </Button>
          <Button
            icon={<MailOutlined />}
            onClick={onSendTest}
            className="btn-test-email"
          >
            Тестовое письмо
          </Button>
          <Button
            icon={<SyncOutlined />}
            onClick={onSync}
            className="btn-sync"
          >
            Синхронизировать
          </Button>
          <Button
            type={showBirthdaysOnly ? 'primary' : 'default'}
            icon={<GiftOutlined />}
            onClick={onToggleBirthdays}
          >
            Именинники
          </Button>
        </Space>
      </Col>
      <Col>
        <Search
          placeholder="Поиск по ФИО..."
          allowClear
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          style={{ width: 300 }}
        />
      </Col>
    </Row>
  );
}
