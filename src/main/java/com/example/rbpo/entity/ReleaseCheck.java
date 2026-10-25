package com.example.rbpo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "release_checks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"release_id", "check_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReleaseCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Column(name = "check_name", nullable = false, length = 200)
    private String checkName;

    @Column(name = "mandatory", nullable = false)
    private boolean mandatory = true;

    @Column(name = "result", nullable = false, length = 30)
    private String result = "PENDING";
}