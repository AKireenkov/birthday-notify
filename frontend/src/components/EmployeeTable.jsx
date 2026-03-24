import React, { useMemo } from 'react';
import { Table, Tag, Button, Tooltip, Popconfirm, Empty } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

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

export default function EmployeeTable({
  employees,
  loading,
  onEdit,
  onDelete,
  hasSearch,
  showBirthdaysOnly,
}) {
  const dataSource = useMemo(() => {
    if (!showBirthdaysOnly) return employees;
    return employees.filter((e) => isBirthdayToday(e.birthDate));
  }, [employees, showBirthdaysOnly]);

  const departmentFilters = useMemo(() => {
    const deps = [...new Set(employees.map((e) => e.department).filter(Boolean))];
    return deps.sort((a, b) => a.localeCompare(b, 'ru')).map((d) => ({
      text: d,
      value: d,
    }));
  }, [employees]);

  const columns = [
    {
      title: 'ФИО',
      dataIndex: 'fullName',
      key: 'fullName',
      sorter: (a, b) => a.fullName.localeCompare(b.fullName, 'ru'),
      ellipsis: true,
      render: (text) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: 'Дата рождения',
      dataIndex: 'birthDate',
      key: 'birthDate',
      width: 260,
      sorter: (a, b) => a.birthDate.localeCompare(b.birthDate),
      render: (date) => {
        const formatted = dayjs(date).format('DD.MM.YYYY');
        const birthday = isBirthdayToday(date);
        const diff = daysUntilBirthday(date);
        const upcoming = !birthday && diff > 0 && diff <= 7;
        return (
          <span>
            {formatted}
            {birthday && (
              <span className="birthday-tag">
                <Tag color="red" style={{ marginLeft: 8 }}>
                  Сегодня!
                </Tag>
              </span>
            )}
            {upcoming && (
              <Tag color="blue" style={{ marginLeft: 8 }}>
                Через {diff} дн.
              </Tag>
            )}
          </span>
        );
      },
    },
    {
      title: 'Должность',
      dataIndex: 'position',
      key: 'position',
      ellipsis: true,
      render: (text) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: 'Подразделение',
      dataIndex: 'department',
      key: 'department',
      filters: departmentFilters,
      onFilter: (value, record) => record.department === value,
      ellipsis: true,
      render: (text) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: 'Действия',
      key: 'actions',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <span>
          <Tooltip title="Редактировать">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => onEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="Удалить сотрудника?"
            description={`Вы уверены, что хотите удалить ${record.fullName}?`}
            onConfirm={() => onDelete(record)}
            okText="Удалить"
            cancelText="Отмена"
            okButtonProps={{ danger: true }}
          >
            <Tooltip title="Удалить">
              <Button type="text" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </span>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      rowKey="id"
      loading={loading}
      pagination={{
        pageSize: 10,
        showSizeChanger: true,
        showTotal: (total, range) => `${range[0]}-${range[1]} из ${total}`,
      }}
      rowClassName={(record) =>
        isBirthdayToday(record.birthDate) ? 'birthday-row' : ''
      }
      locale={{
        emptyText: (
          <Empty
            description={
              hasSearch
                ? 'Сотрудники не найдены'
                : 'Нет данных о сотрудниках'
            }
          />
        ),
      }}
      bordered
      size="middle"
    />
  );
}
