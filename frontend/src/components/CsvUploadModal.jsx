import React, { useState, useRef } from 'react';
import { ModalDesktop as Modal } from '@alfalab/core-components-modal/desktop';
import { ButtonDesktop as Button } from '@alfalab/core-components-button/desktop';
import { Dropzone } from '@alfalab/core-components-dropzone';
import { Typography } from '@alfalab/core-components-typography';
import { Status } from '@alfalab/core-components-status';
import { Gap } from '@alfalab/core-components-gap';

export default function CsvUploadModal({ open, onUpload, onCancel }) {
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState(null);
  const fileInputRef = useRef(null);

  const handleFile = async (file) => {
    if (!file || !file.name.endsWith('.csv')) return;
    setUploading(true);
    setUploadResult(null);
    try {
      const result = await onUpload(file);
      if (result) setUploadResult(result);
    } finally {
      setUploading(false);
    }
  };

  const handleClose = () => {
    setUploadResult(null);
    onCancel();
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      size={600}
      hasCloser={true}
    >
      <Modal.Header title="Загрузка CSV файла" />
      <Modal.Content>
        {uploadResult ? (
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            <Status color="green" view="soft" size={32}>
              CSV успешно загружен
            </Status>
            <Gap size={16} />
            <Typography.Text view="primary-medium">
              Добавлено: <strong>{uploadResult.imported ?? 0}</strong>
            </Typography.Text>
            {uploadResult.errorsSkipped > 0 && (
              <>
                <Gap size={4} />
                <Typography.Text view="primary-small" color="negative">
                  Пропущено (ошибки): {uploadResult.errorsSkipped}
                </Typography.Text>
              </>
            )}
            {uploadResult.duplicatesSkipped > 0 && (
              <>
                <Gap size={4} />
                <Typography.Text view="primary-small" color="secondary">
                  Дубликатов: {uploadResult.duplicatesSkipped}
                </Typography.Text>
              </>
            )}
          </div>
        ) : (
          <>
            <Dropzone
              text={uploading ? 'Загрузка...' : 'Нажмите или перетащите CSV файл'}
              block={true}
              onDrop={(files) => handleFile(files[0])}
              disabled={uploading}
            >
              <div
                style={{ padding: '40px 20px', textAlign: 'center', cursor: 'pointer' }}
                onClick={() => fileInputRef.current?.click()}
              >
                <Typography.Text view="primary-large" color="secondary">
                  {uploading ? 'Загрузка...' : 'Нажмите или перетащите CSV файл сюда'}
                </Typography.Text>
                <Gap size={8} />
                <Typography.Text view="secondary-medium" color="tertiary">
                  Формат: ФИО;Дата рождения;Должность;Подразделение
                </Typography.Text>
              </div>
            </Dropzone>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              style={{ display: 'none' }}
              onChange={(e) => {
                handleFile(e.target.files[0]);
                e.target.value = '';
              }}
            />
            <Gap size={8} />
            <Typography.Text view="secondary-small" color="tertiary">
              Поддерживается UTF-8, разделитель — точка с запятой (;) или запятая (,)
            </Typography.Text>
          </>
        )}
      </Modal.Content>
      {uploadResult && (
        <Modal.Footer>
          <Button view="accent" size={48} onClick={handleClose} block={true}>
            OK
          </Button>
        </Modal.Footer>
      )}
    </Modal>
  );
}
