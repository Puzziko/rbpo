package com.example.rbpo.entity;

public enum SessionStatus {

    // refresh ещё не использован и не отозван
    ACTIVE,

    // refresh успешно применён для обновления пары
    USED,

    // refresh отозван
    REVOKED
}
