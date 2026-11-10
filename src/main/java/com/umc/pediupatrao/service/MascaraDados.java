package com.umc.pediupatrao.service;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class MascaraDados {

    private MascaraDados() {
    }

    // (11) 98765-4321 -> (**) *****-4321
    public static String telefone(String valor) {
        int digitos = (int) valor.chars().filter(Character::isDigit).count();
        int visiveis = Math.min(4, digitos);
        StringBuilder sb = new StringBuilder();
        int vistos = 0;
        for (char c : valor.toCharArray()) {
            if (Character.isDigit(c)) {
                vistos++;
                sb.append(vistos > digitos - visiveis ? c : '*');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Maria Aparecida Souza -> Maria A. S.
    public static String nome(String valor) {
        String[] partes = valor.trim().split("\\s+");
        if (partes.length <= 1) {
            return valor.trim();
        }
        return partes[0] + " " + Arrays.stream(partes, 1, partes.length)
                .map(p -> p.charAt(0) + ".")
                .collect(Collectors.joining(" "));
    }

    // 08780-120 -> 08780-***
    public static String cep(String valor) {
        StringBuilder sb = new StringBuilder();
        int vistos = 0;
        for (char c : valor.toCharArray()) {
            if (Character.isDigit(c)) {
                vistos++;
                sb.append(vistos > 5 ? '*' : c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String digitos(String valor) {
        return valor.replaceAll("\\d", "*");
    }

    public static String oculto(String valor) {
        return valor.isEmpty() ? valor : "***";
    }
}
