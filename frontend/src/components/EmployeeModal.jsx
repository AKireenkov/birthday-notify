import React, { useEffect, useState } from 'react';
import { ModalDesktop as Modal } from '@alfalab/core-components-modal/desktop';
import { ButtonDesktop as Button } from '@alfalab/core-components-button/desktop';
import { InputDesktop as Input } from '@alfalab/core-components-input/desktop';
import { Gap } from '@alfalab/core-components-gap';

export default function EmployeeModal({ open, employee, onSave, onSaveAndAddAnother, onCancel }) {
  const isEdit = Boolean(employee);
  const [form, setForm] = useState({ fullName: '', birthDate: '', position: '', department: '' });
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (open) {
      if (employee) {
        setForm({
          fullName: employee.fullName || '',
          birthDate: employee.birthDate || '',
          position: employee.position || '',
          department: employee.department || '',
        });
      } else {
        setForm({ fullName: '', birthDate: '', position: '', department: '' });
      }
      setErrors({});
    }
  }, [open, employee]);

  const validate = () => {
    const errs = {};
    if (!form.fullName.trim()) errs.fullName = 'Введите ФИО сотрудника';
    if (!form.birthDate) errs.birthDate = 'Выберите дату рождения';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const getValues = () => ({
    fullName: form.fullName.trim(),
    birthDate: form.birthDate,
    position: form.position.trim(),
    department: form.department.trim(),
  });

  const handleOk = () => {
    if (!validate()) return;
    onSave(getValues());
  };

  const handleSaveAndAdd = async () => {
    if (!validate()) return;
    await onSaveAndAddAnother(getValues());
    setForm({ fullName: '', birthDate: '', position: '', department: '' });
    setErrors({});
  };

  const handleChange = (field) => (e, payload) => {
    const value = payload?.value ?? e?.target?.value ?? '';
    setForm((f) => ({ ...f, [field]: value }));
    if (errors[field]) {
      setErrors((er) => ({ ...er, [field]: undefined }));
    }
  };

  return (
    <Modal
      open={open}
      onClose={onCancel}
      size={600}
      hasCloser={true}
    >
      <Modal.Header title={isEdit ? 'Редактировать сотрудника' : 'Добавить сотрудника'} />
      <Modal.Content>
        <Input
          label="ФИО"
          placeholder="Иванов Иван Иванович"
          value={form.fullName}
          onChange={handleChange('fullName')}
          error={errors.fullName}
          block={true}
          size={48}
          autoFocus
        />
        <Gap size={16} />

        <Input
          label="Дата рождения"
          type="date"
          value={form.birthDate}
          onChange={handleChange('birthDate')}
          error={errors.birthDate}
          block={true}
          size={48}
        />
        <Gap size={16} />

        <Input
          label="Должность"
          placeholder="Должность сотрудника"
          value={form.position}
          onChange={handleChange('position')}
          block={true}
          size={48}
        />
        <Gap size={16} />

        <Input
          label="Подразделение"
          placeholder="Название подразделения"
          value={form.department}
          onChange={handleChange('department')}
          block={true}
          size={48}
        />
      </Modal.Content>
      <Modal.Footer>
        <div className="modal-footer-buttons">
          <Button view="accent" size={48} onClick={handleOk} block>
            {isEdit ? 'Сохранить' : 'Добавить'}
          </Button>
          {!isEdit && (
            <Button view="secondary" size={48} onClick={handleSaveAndAdd} block>
              Сохранить и добавить ещё
            </Button>
          )}
          <Button view="transparent" size={48} onClick={onCancel} block>
            Отмена
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}
