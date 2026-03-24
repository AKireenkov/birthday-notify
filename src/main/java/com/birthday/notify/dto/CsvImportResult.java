package com.birthday.notify.dto;

public class CsvImportResult {
    private int imported;
    private int duplicatesSkipped;
    private int errorsSkipped;

    public CsvImportResult(int imported, int duplicatesSkipped, int errorsSkipped) {
        this.imported = imported;
        this.duplicatesSkipped = duplicatesSkipped;
        this.errorsSkipped = errorsSkipped;
    }

    public int getImported() { return imported; }
    public int getDuplicatesSkipped() { return duplicatesSkipped; }
    public int getErrorsSkipped() { return errorsSkipped; }

    public String toMessage() {
        return String.format("Импортировано: %d, дубликатов пропущено: %d, ошибок: %d",
                imported, duplicatesSkipped, errorsSkipped);
    }
}
