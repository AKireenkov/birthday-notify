package com.birthday.notify.controller;

import com.birthday.notify.service.FileSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncApiController {

    private final FileSyncService fileSyncService;

    public SyncApiController(FileSyncService fileSyncService) {
        this.fileSyncService = fileSyncService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> runSync() {
        String result = fileSyncService.syncFromFolder();
        return ResponseEntity.ok(Map.of(
                "message", result,
                "lastSync", fileSyncService.getLastSyncTime() != null
                        ? fileSyncService.getLastSyncTime().toString() : "never"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "lastSync", fileSyncService.getLastSyncTime() != null
                        ? fileSyncService.getLastSyncTime().toString() : "never",
                "lastStatus", fileSyncService.getLastSyncStatus() != null
                        ? fileSyncService.getLastSyncStatus() : "Синхронизация не запускалась"
        ));
    }
}
