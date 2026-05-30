package yt.wer.efms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        long maxBytes = ex.getMaxUploadSize();
        Map<String, Object> body = Map.of(
                "error", "file_too_large",
                "message", "File too large. Maximum upload size is "
                        + (maxBytes > 0 ? (maxBytes / (1024 * 1024)) + " MB" : "limited by the server."),
                "maxBytes", maxBytes);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }
}
