package com.distributedkv.api;

import com.distributedkv.cluster.RaftCluster;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kv")
public class KeyValueController {

    private final RaftCluster raftCluster;

    public KeyValueController(RaftCluster raftCluster) {
        this.raftCluster = raftCluster;
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> put(
            @PathVariable String key,
            @RequestBody(required = false) KeyValueRequest request) {

        if (isBlank(key)) {
            return badRequest("Key must not be blank.");
        }

        if (request == null || isBlank(request.value())) {
            return badRequest("Value must not be blank.");
        }

        boolean committed = raftCluster.replicateLog(
                "PUT", key, request.value());

        if (!committed) {
            return writeFailure();
        }

        return ResponseEntity.ok(
                new KeyValueResponse(key, request.value()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> get(@PathVariable String key) {

        if (isBlank(key)) {
            return badRequest("Key must not be blank.");
        }

        if (raftCluster.getLeader() == null) {
            return noLeader();
        }

        String value = raftCluster.readFromLeader(key);

        if (value == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Key not found: " + key));
        }

        return ResponseEntity.ok(new KeyValueResponse(key, value));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<?> delete(@PathVariable String key) {

        if (isBlank(key)) {
            return badRequest("Key must not be blank.");
        }

        boolean committed = raftCluster.replicateLog(
                "DELETE", key, null);

        if (!committed) {
            return writeFailure();
        }

        return ResponseEntity.ok(new KeyValueResponse(key, null));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message));
    }

    private ResponseEntity<ErrorResponse> noLeader() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("No active leader available."));
    }

    private ResponseEntity<ErrorResponse> writeFailure() {

        if (raftCluster.getLeader() == null) {
            return noLeader();
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "Entry could not be committed by a majority."));
    }
}
