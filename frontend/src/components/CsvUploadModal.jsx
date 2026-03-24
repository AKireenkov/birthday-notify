import React, { useState } from 'react';
import { Modal, Upload, Typography, Button, Alert, Space } from 'antd';
import { InboxOutlined, CheckCircleOutlined } from '@ant-design/icons';

const { Dragger } = Upload;
const { Text } = Typography;

export default function CsvUploadModal({ open, onUpload, onCancel }) {
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState(null);

  const handleUpload = async (file) => {
    setUploading(true);
    setUploadResult(null);
    try {
      const result = await onUpload(file);
      if (result) {
        setUploadResult(result);
      }
    } finally {
      setUploading(false);
    }
    return false; // prevent antd's default upload
  };

  const handleClose = () => {
    setUploadResult(null);
    onCancel();
  };

  return (
    <Modal
      title="Загрузка CSV файла"
      open={open}
      onCancel={handleClose}
      footer={
        uploadResult ? (
          <Button type="primary" onClick={handleClose}>
            OK
          </Button>
        ) : null
      }
      destroyOnClose
      afterClose={() => setUploadResult(null)}
    >
      {uploadResult ? (
        <Alert
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          message="CSV успешно загружен"
          description={
            <Space direction="vertical" size={2}>
              <Text>Добавлено: {uploadResult.imported ?? 0}</Text>
              {uploadResult.errorsSkipped != null && uploadResult.errorsSkipped > 0 && <Text>Пропущено (ошибки): {uploadResult.errorsSkipped}</Text>}
              {uploadResult.duplicatesSkipped != null && uploadResult.duplicatesSkipped > 0 && <Text>Дубликатов: {uploadResult.duplicatesSkipped}</Text>}
            </Space>
          }
          style={{ marginTop: 8 }}
        />
      ) : (
        <>
          <Dragger
            name="file"
            accept=".csv"
            multiple={false}
            showUploadList={false}
            beforeUpload={handleUpload}
            disabled={uploading}
            style={{ padding: '20px 0' }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">
              {uploading
                ? 'Загрузка...'
                : 'Нажмите или перетащите CSV файл в эту область'}
            </p>
            <p className="ant-upload-hint">
              Формат: ФИО;Дата рождения;Должность;Подразделение
            </p>
          </Dragger>
          <div style={{ marginTop: 12 }}>
            <Text type="secondary">
              Поддерживается UTF-8, разделитель — точка с запятой (;) или запятая (,)
            </Text>
          </div>
        </>
      )}
    </Modal>
  );
}
