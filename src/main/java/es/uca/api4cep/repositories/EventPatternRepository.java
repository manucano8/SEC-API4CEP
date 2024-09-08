package es.uca.api4cep.repositories;

import org.springframework.stereotype.Repository;

import es.uca.api4cep.entities.EventPattern;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Repository interface for accessing EventPattern entities.
 */
@Repository
public interface EventPatternRepository extends CrudRepository<EventPattern, Long>{

    /**
     * Finds EventPatterns by their name.
     * @param name The name of the EventPattern
     * @return A list of EventPatterns with the given name
     */
    public abstract List<EventPattern> findByName(String name);
}