import React, { useMemo, useState } from 'react';
import dayjs from 'dayjs';
import { Table, THead, TBody, TRow, TCell, TSortableHeadCell, THeadCell } from '@alfalab/core-components-table';
import { ButtonDesktop as Button } from '@alfalab/core-components-button/desktop';
import { IconButtonDesktop as IconButton } from '@alfalab/core-components-icon-button/desktop';
import { TagDesktop as Tag } from '@alfalab/core-components-tag/desktop';
import { Status } from '@alfalab/core-components-status';
import { Typography } from '@alfalab/core-components-typography';
import { ModalDesktop as Modal } from '@alfalab/core-components-modal/desktop';
import { Gap } from '@alfalab/core-components-gap';

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

const PAGE_SIZE = 10;

// Simple edit/delete SVG icons
const EditIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
  </svg>
);

const DeleteIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="3 6 5 6 21 6" />
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
  </svg>
);

export default function EmployeeTable({
  employees,
  loading,
  onEdit,
  onDelete,
  hasSearch,
  showBirthdaysOnly,
}) {
  const [sortField, setSortField] = useState(null);
  const [sortDesc, setSortDesc] = useState(false);
  const [page, setPage] = useState(1);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const dataSource = useMemo(() => {
    if (!showBirthdaysOnly) return employees;
    return employees.filter((e) => isBirthdayToday(e.birthDate));
  }, [employees, showBirthdaysOnly]);

  const sortedData = useMemo(() => {
    if (!sortField) return dataSource;
    return [...dataSource].sort((a, b) => {
      const valA = a[sortField] || '';
      const valB = b[sortField] || '';
      const cmp = valA.localeCompare(valB, 'ru');
      return sortDesc ? -cmp : cmp;
    });
  }, [dataSource, sortField, sortDesc]);

  const totalPages = Math.ceil(sortedData.length / PAGE_SIZE);
  const pageData = sortedData.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  React.useEffect(() => { setPage(1); }, [employees, showBirthdaysOnly]);

  const handleSort = (field) => () => {
    if (sortField === field) {
      setSortDesc((d) => !d);
    } else {
      setSortField(field);
      setSortDesc(false);
    }
  };

  const confirmDelete = () => {
    if (deleteConfirm) {
      onDelete(deleteConfirm);
      setDeleteConfirm(null);
    }
  };

  if (loading) {
    return (
      <div className="empty-state">
        <Typography.Text view="primary-medium" color="secondary">Загрузка...</Typography.Text>
      </div>
    );
  }

  if (sortedData.length === 0) {
    return (
      <div className="empty-state">
        <Typography.Text view="primary-large" color="secondary">
          {hasSearch ? 'Сотрудники не найдены' : 'Нет данных о сотрудниках'}
        </Typography.Text>
      </div>
    );
  }

  return (
    <>
      <div className="table-wrapper">
        <Table>
          <THead>
            <TSortableHeadCell
              isSortedDesc={sortField === 'fullName' ? sortDesc : undefined}
              onSort={handleSort('fullName')}
            >
              ФИО
            </TSortableHeadCell>
            <TSortableHeadCell
              isSortedDesc={sortField === 'birthDate' ? sortDesc : undefined}
              onSort={handleSort('birthDate')}
            >
              Дата рождения
            </TSortableHeadCell>
            <TSortableHeadCell
              isSortedDesc={sortField === 'position' ? sortDesc : undefined}
              onSort={handleSort('position')}
            >
              Должность
            </TSortableHeadCell>
            <TSortableHeadCell
              isSortedDesc={sortField === 'department' ? sortDesc : undefined}
              onSort={handleSort('department')}
            >
              Подразделение
            </TSortableHeadCell>
            <THeadCell width={120} textAlign="center">
              Действия
            </THeadCell>
          </THead>
          <TBody>
            {pageData.map((emp) => {
              const birthday = isBirthdayToday(emp.birthDate);
              const diff = daysUntilBirthday(emp.birthDate);
              const isTomorrow = !birthday && diff === 1;

              return (
                <TRow key={emp.id} className={birthday ? 'birthday-row' : ''}>
                  <TCell>
                    <Typography.Text view="primary-small" weight="medium">
                      {emp.fullName}
                    </Typography.Text>
                  </TCell>
                  <TCell>
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      {dayjs(emp.birthDate).format('DD.MM.YYYY')}
                      {birthday && (
                        <Status color="red" view="contrast" size={20} className="status-birthday">
                          Сегодня
                        </Status>
                      )}
                      {isTomorrow && (
                        <Status color="orange" view="soft" size={20}>
                          Завтра
                        </Status>
                      )}
                    </span>
                  </TCell>
                  <TCell>
                    <Typography.Text view="primary-small" color={emp.position ? undefined : 'tertiary'}>
                      {emp.position || '—'}
                    </Typography.Text>
                  </TCell>
                  <TCell>
                    <Typography.Text view="primary-small" color={emp.department ? undefined : 'tertiary'}>
                      {emp.department || '—'}
                    </Typography.Text>
                  </TCell>
                  <TCell style={{ textAlign: 'center' }}>
                    <IconButton
                      icon={EditIcon}
                      view="secondary"
                      size={32}
                      onClick={() => onEdit(emp)}
                    />
                    <IconButton
                      icon={DeleteIcon}
                      view="negative"
                      size={32}
                      onClick={() => setDeleteConfirm(emp)}
                      style={{ marginLeft: 4 }}
                    />
                  </TCell>
                </TRow>
              );
            })}
          </TBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <Typography.Text view="secondary-large" color="secondary">
            {(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, sortedData.length)} из {sortedData.length}
          </Typography.Text>
          <div className="pagination-controls">
            <Button view="text" size={32} disabled={page === 1} onClick={() => setPage((p) => p - 1)}>
              &lsaquo;
            </Button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
              <Button
                key={p}
                view={p === page ? 'accent' : 'text'}
                size={32}
                onClick={() => setPage(p)}
              >
                {p}
              </Button>
            ))}
            <Button view="text" size={32} disabled={page === totalPages} onClick={() => setPage((p) => p + 1)}>
              &rsaquo;
            </Button>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      <Modal
        open={Boolean(deleteConfirm)}
        onClose={() => setDeleteConfirm(null)}
        size={500}
        hasCloser={true}
      >
        <Modal.Header title="Удалить сотрудника?" />
        <Modal.Content>
          <Typography.Text view="primary-medium">
            Вы уверены, что хотите удалить {deleteConfirm?.fullName}?
          </Typography.Text>
        </Modal.Content>
        <Modal.Footer layout="space-between">
          <Button view="transparent" size={48} onClick={confirmDelete} className="delete-action">
            Удалить
          </Button>
          <Button view="secondary" size={48} onClick={() => setDeleteConfirm(null)}>
            Отмена
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
