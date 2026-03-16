package com.luizfernando.gestaousuarios.api.exceptionhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Captura qualquer RuntimeException (como aquela que lançamos no Service se o email já existir)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRegraDeNegocioException(RuntimeException ex) {
        Map<String, String> resposta = new HashMap<>();
        resposta.put("erro", ex.getMessage()); //mensagem padrão de email cadastrado, senha invalida e etc...
        
        // Status de 400 erros de validação, mostrando a mensagem de erro, e que o erro foi nos dados enviados pelo usuário
    
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
}