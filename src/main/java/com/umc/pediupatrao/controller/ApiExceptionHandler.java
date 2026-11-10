package com.umc.pediupatrao.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte violações de regra de negócio em respostas JSON {"erro": "..."}. */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> entradaInvalida(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estadoInvalido(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
    }

    // Ex.: ?dataInicio=31/12/2026 ou ?status=ENTREGUE em parâmetros agrupados num objeto
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(BindException e) {
        FieldError erro = e.getFieldError();
        String campo = erro == null ? "requisição" : erro.getField();
        Object valor = erro == null ? null : erro.getRejectedValue();
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem(campo, valor)));
    }

    // Ex.: ?agrupamento=ANO ou ?limite=abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem(e.getName(), e.getValue())));
    }

    private static String mensagem(String campo, Object valor) {
        return "Valor inválido para o parâmetro '" + campo + "': " + valor;
    }
}
