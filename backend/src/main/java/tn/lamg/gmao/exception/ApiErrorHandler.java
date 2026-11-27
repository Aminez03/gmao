package tn.lamg.gmao.exception;

import jakarta.persistence.OptimisticLockException;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import tn.lamg.gmao.common.ApiException;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> api(ApiException e) {
        return problem(e.status, e.code, e.getMessage());
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
    })
    ResponseEntity<ProblemDetail> invalid(Exception e) {
        return problem(422, "INVALID_INPUT", "Vérifiez les champs et leurs formats.");
    }

    @ExceptionHandler({ DataIntegrityViolationException.class })
    ResponseEntity<ProblemDetail> integrity(Exception e) {
        return problem(
            409,
            "INTEGRITY_CONFLICT",
            "Référence invalide, doublon ou ressource encore utilisée."
        );
    }

    @ExceptionHandler({ OptimisticLockException.class, OptimisticLockingFailureException.class })
    ResponseEntity<ProblemDetail> version(Exception e) {
        return problem(
            412,
            "VERSION_CONFLICT",
            "Cette ressource a été modifiée. Rechargez et comparez vos saisies."
        );
    }

    @ExceptionHandler({
        org.springframework.web.bind.MissingRequestHeaderException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
    })
    ResponseEntity<ProblemDetail> request(Exception e) {
        return problem(400, "INVALID_REQUEST", "Paramètre ou en-tête requis absent ou invalide.");
    }

    @ExceptionHandler({ org.springframework.web.multipart.MaxUploadSizeExceededException.class })
    ResponseEntity<ProblemDetail> size(Exception e) {
        return problem(413, "FILE_TOO_LARGE", "Fichier supérieur à la taille autorisée.");
    }

    private ResponseEntity<ProblemDetail> problem(int status, String code, String detail) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        p.setProperty("code", code);
        p.setProperty("correlationId", UUID.randomUUID());
        return ResponseEntity.status(status).body(p);
    }
}
