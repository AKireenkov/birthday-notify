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
    if (!form.birthDate) {
      errs.birthDate = 'Выберите дату рождения';
    } else {
      const bd = new Date(form.birthDate);
      const today = new Date();
      if (bd > today) errs.birthDate = 'Дата рождения не может быть в будущем';
      if (bd < new Date('1920-01-01')) errs.birthDate = 'Проверьте дату рождения';
    }
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

  // Format date for display label
  const birthDateLabel = form.birthDate
    ? `Дата рождения`
    : 'Дата рождения';

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
          size={56}
          autoFocus
        />
        <Gap size={16} />

        <div className="date-input-wrapper">
          <label className="date-input-label" htmlFor="birth-date-input">Дата рождения</label>
          <input
            id="birth-date-input"
            type="date"
            className="date-input-native"
            value={form.birthDate}
            min="1920-01-01"
            max={new Date().toISOString().split('T')[0]}
            onChange={(e) => {
              setForm((f) => ({ ...f, birthDate: e.target.value }));
              if (errors.birthDate) setErrors((er) => ({ ...er, birthDate: undefined }));
            }}
          />
          {errors.birthDate && <div className="date-input-error">{errors.birthDate}</div>}
        </div>
        <Gap size={16} />

        <Input
          label="Должность"
          placeholder="Должность сотрудника"
          value={form.position}
          onChange={handleChange('position')}
          block={true}
          size={56}
        />
        <Gap size={16} />

        <Input
          label="Подразделение"
          placeholder="Название подразделения"
          value={form.department}
          onChange={handleChange('department')}
          block={true}
          size={56}
        />
      </Modal.Content>
      <Modal.Footer>
        <div className="modal-footer-buttons">
          <div className="modal-footer-row">
            <Button view="accent" size={48} onClick={handleOk} block>
              {isEdit ? 'Сохранить' : 'Добавить'}
            </Button>
            {!isEdit && (
              <Button view="secondary" size={48} onClick={handleSaveAndAdd} block>
                Сохранить и добавить ещё
              </Button>
            )}
          </div>
          <Button view="outlined" size={48} onClick={onCancel} block>
            Отмена
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}
