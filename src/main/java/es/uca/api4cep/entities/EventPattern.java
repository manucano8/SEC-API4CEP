package es.uca.api4cep.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TABLE_EVENT_PATTERN")
@Getter @Setter
public class EventPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    @Schema(accessMode = AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Column(name = "content", nullable = false, length = 2044)
    private String content;

    @Column(name = "is_ready_to_deploy", nullable = false)
    @Schema(defaultValue = "false")
    private boolean readyToDeploy;

    @Column(name = "is_deployed", nullable = false)
    @Schema(defaultValue = "false")
    private boolean deployed;
}
