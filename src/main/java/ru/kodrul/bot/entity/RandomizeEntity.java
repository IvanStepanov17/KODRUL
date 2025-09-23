package ru.kodrul.bot.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "randomize")
public class RandomizeEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Column
    String initiatorName;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "participant", joinColumns = @JoinColumn(name = "randomize_id"))
    @Column(name = "participants", nullable = false)
    Set<String> participants;

    @Column
    String loser;

    @Column
    LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }

        return id != null && Objects.equals(id, ((RandomizeEntity) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
