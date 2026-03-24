import React, { useEffect } from 'react';
import { Modal, Form, Input, DatePicker, Button, Space } from 'antd';
import dayjs from 'dayjs';

export default function EmployeeModal({ open, employee, onSave, onSaveAndAddAnother, onCancel }) {
  const [form] = Form.useForm();
  const isEdit = Boolean(employee);

  useEffect(() => {
    if (open) {
      if (employee) {
        form.setFieldsValue({
          fullName: employee.fullName,
          birthDate: dayjs(employee.birthDate),
          position: employee.position,
          department: employee.department,
        });
      } else {
        form.resetFields();
      }
    }
  }, [open, employee, form]);

  const getValues = async () => {
    const values = await form.validateFields();
    return {
      fullName: values.fullName.trim(),
      birthDate: values.birthDate.format('YYYY-MM-DD'),
      position: (values.position || '').trim(),
      department: (values.department || '').trim(),
    };
  };

  const handleOk = async () => {
    try {
      const data = await getValues();
      onSave(data);
    } catch {
      // validation failed, form shows errors
    }
  };

  const handleSaveAndAddAnother = async () => {
    try {
      const data = await getValues();
      await onSaveAndAddAnother(data);
      form.resetFields();
    } catch {
      // validation failed, form shows errors
    }
  };

  return (
    <Modal
      title={isEdit ? 'Редактировать сотрудника' : 'Добавить сотрудника'}
      open={open}
      onCancel={onCancel}
      destroyOnClose
      forceRender
      footer={
        <Space>
          <Button onClick={onCancel}>Отмена</Button>
          {!isEdit && (
            <Button onClick={handleSaveAndAddAnother}>
              Сохранить и добавить ещё
            </Button>
          )}
          <Button type="primary" onClick={handleOk}>
            {isEdit ? 'Сохранить' : 'Добавить'}
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="fullName"
          label="ФИО"
          rules={[{ required: true, message: 'Введите ФИО сотрудника' }]}
        >
          <Input placeholder="Иванов Иван Иванович" />
        </Form.Item>

        <Form.Item
          name="birthDate"
          label="Дата рождения"
          rules={[{ required: true, message: 'Выберите дату рождения' }]}
        >
          <DatePicker
            format="DD.MM.YYYY"
            placeholder="Выберите дату"
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item name="position" label="Должность">
          <Input placeholder="Должность сотрудника" />
        </Form.Item>

        <Form.Item name="department" label="Подразделение">
          <Input placeholder="Название подразделения" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
