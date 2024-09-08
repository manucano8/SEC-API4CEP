package es.uca.api4cep.repositories;

import org.springframework.stereotype.Repository;

import es.uca.api4cep.entities.EventType;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Repository interface for accessing EventType entities.
 */
@Repository
public interface EventTypeRepository extends CrudRepository<EventType, Long>{

    /**
     * Finds EventTypes by their name.
     * @param name The name of the EventType
     * @return A list of EventTypes with the given name
     */
    public abstract List<EventType> findByName(String name);
}