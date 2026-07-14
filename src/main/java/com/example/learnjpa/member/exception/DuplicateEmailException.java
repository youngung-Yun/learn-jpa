package com.example.learnjpa.member.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("이미 사용중인 이메일입니다: %s".formatted(email));
    }
}
