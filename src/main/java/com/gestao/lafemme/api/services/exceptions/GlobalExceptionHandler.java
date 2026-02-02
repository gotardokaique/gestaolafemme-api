package com.gestao.lafemme.api.services.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ 404 - NOT FOUND (sua)
    @ResponseBody
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        logNoStack(ex, HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg(ex));
    }

    // ✅ 404 - NOT FOUND (JPA)
    @ResponseBody
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleJpaNotFound(EntityNotFoundException ex) {
        logNoStack(ex, HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg(ex));
    }

    // ✅ 400/409 - BUSINESS (SEM stack)
    @ResponseBody
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        // se quiser padronizar como 409 quando for duplicidade, você pode criar uma BusinessConflictException.
        logNoStack(ex, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg(ex));
    }

    // ✅ Genérico: só erro REAL (COM stack)
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        logWithStack(ex, HttpStatus.INTERNAL_SERVER_ERROR);

        Map<String, String> body = new HashMap<>();
        body.put("message", "Ops, algo deu errado!");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ----------------- helpers -----------------

    private Map<String, String> msg(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return body;
    }

    private void logNoStack(Throwable ex, HttpStatus status) {
        StackTraceElement elem = firstBusinessFrame(ex);

        String exceptionType = ex.getClass().getSimpleName();
        String className = elem != null ? elem.getClassName() : "unknown";
        String methodName = elem != null ? elem.getMethodName() : "unknown";
        int lineNumber = elem != null ? elem.getLineNumber() : -1;

        logger.warn("[EXCEPTION] Status: {} | Tipo: {} | Classe: {} | Método: {} | Linha: {} | Mensagem: {}",
                status.value(), exceptionType, className, methodName, lineNumber, ex.getMessage());
    }

    private void logWithStack(Throwable ex, HttpStatus status) {
        StackTraceElement elem = firstBusinessFrame(ex);

        String exceptionType = ex.getClass().getSimpleName();
        String className = elem != null ? elem.getClassName() : "unknown";
        String methodName = elem != null ? elem.getMethodName() : "unknown";
        int lineNumber = elem != null ? elem.getLineNumber() : -1;

        logger.error("[EXCEPTION] Status: {} | Tipo: {} | Classe: {} | Método: {} | Linha: {} | Mensagem: {}",
                status.value(), exceptionType, className, methodName, lineNumber, ex.getMessage(), ex);
    }

    // pega o ponto onde você chamou (service/controller), pulando infra
    private StackTraceElement firstBusinessFrame(Throwable ex) {
        StackTraceElement[] st = ex.getStackTrace();
        if (st == null || st.length == 0) return null;

        String[] skipPrefixes = new String[] {
                "org.springframework.", "java.", "jdk.", "sun.", "jakarta.",
                "com.gestao.lafemme.api.db.",
                "com.gestao.lafemme.api.services.exceptions."
        };

        for (StackTraceElement e : st) {
            String cn = e.getClassName();
            if (cn == null) continue;
            if (!cn.startsWith("com.gestao.")) continue;
            if (startsWithAny(cn, skipPrefixes)) continue;
            return e;
        }

        for (StackTraceElement e : st) {
            String cn = e.getClassName();
            if (cn != null && cn.startsWith("com.gestao.")) return e;
        }

        return st[0];
    }

    private boolean startsWithAny(String value, String[] prefixes) {
        for (String p : prefixes) {
            if (value.startsWith(p)) return true;
        }
        return false;
    }
}
