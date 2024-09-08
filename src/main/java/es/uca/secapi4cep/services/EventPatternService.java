package es.uca.secapi4cep.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import es.uca.secapi4cep.repositories.EventPatternRepository;
import com.rabbitmq.client.Channel;
import es.uca.secapi4cep.entities.EventPattern;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

@Service
public class EventPatternService {

    // Injects the RabbitMQ ConnectionFactory defined in RabbitMQConfig
    @Autowired
    private CachingConnectionFactory connectionFactory;

    private final EventPatternRepository eventPatternRepository;

    // Constructor to inject EventPatternRepository
    public EventPatternService(EventPatternRepository eventPatternRepository) {
        this.eventPatternRepository = eventPatternRepository;
    }

    /**
     * Retrieves all event patterns from the repository.
     * @return List of all EventPattern entities
     */
    public List<EventPattern> getAllEventPatterns() {
        return (ArrayList<EventPattern>) eventPatternRepository.findAll();
    }

    /**
     * Retrieves an event pattern by its ID.
     * @param id The ID of the event pattern
     * @return Optional containing the EventPattern if found, otherwise empty
     */
    public Optional<EventPattern> getEventPatternById(Long id) {
        return eventPatternRepository.findById(id);
    }

    /**
     * Saves a new event pattern to the repository.
     * @param eventPattern The EventPattern entity to save
     * @return The saved EventPattern entity
     */
    public EventPattern saveEventPattern(EventPattern eventPattern) {
        return eventPatternRepository.save(eventPattern);
    }

    /**
     * Updates an existing event pattern and handles deployment and undeployment logic.
     * @param newEventPattern The new event pattern data
     * @param id The ID of the event pattern to update
     * @return True if update was successful, false otherwise
     */
    public boolean updateEventPattern(EventPattern newEventPattern, Long id) {   
        try (com.rabbitmq.client.Connection outputConnectionUCA = connectionFactory.getRabbitConnectionFactory().newConnection();
            Channel outputChannel = outputConnectionUCA.createChannel();){         

            String deployQueue = "deploy";
            String undeployQueue = "undeploy";
            
            outputChannel.queueDeclare(deployQueue, false, false, false, null);
            outputChannel.queueDeclare(undeployQueue, false, false, false, null);

            Optional<EventPattern> eventPatternOptional = eventPatternRepository.findById(id);
            if (!eventPatternOptional.isEmpty()){
                EventPattern retrievedEventPattern = eventPatternOptional.get();
                String retrievedEventPatternName = retrievedEventPattern.getName();
                if(!retrievedEventPattern.isReadyToDeploy()) {
                    retrievedEventPattern.setName(newEventPattern.getName());
                    retrievedEventPattern.setContent(newEventPattern.getContent());

                    eventPatternRepository.save(retrievedEventPattern);
                    if (retrievedEventPattern.isDeployed()) {
                        String message;

                        // Send undeploy message
                        message = retrievedEventPatternName;
                        outputChannel.basicPublish("",undeployQueue, null, message.getBytes());

                        // Send deploy message
                        message = newEventPattern.getContent();
                        outputChannel.basicPublish("",deployQueue, null, message.getBytes());
                    }
                    
                    return true;
                }
                else {
                  return false;
                }    
            }
            else {
              return false;
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the 'ready to deploy' status of an event pattern.
     * @param id The ID of the event pattern
     * @param status The new 'ready to deploy' status
     * @return True if the update was successful, false otherwise
     */
    public boolean updateStatus(Long id, boolean status) {
        Optional<EventPattern> eventPatternOptional = eventPatternRepository.findById(id);
        if (!eventPatternOptional.isEmpty()){
            EventPattern retrievedEventPattern = eventPatternOptional.get();
            retrievedEventPattern.setReadyToDeploy(status);
            eventPatternRepository.save(retrievedEventPattern);
            return true;
        }
        else {
          return false;
        }
    }

    /**
     * Updates the deploying status of an event pattern and sends appropriate messages.
     * @param id The ID of the event pattern
     * @param status The new deploying status
     * @return True if the update was successful, false otherwise
     */
    public boolean updateDeployingStatus(Long id, boolean status) {

        try (com.rabbitmq.client.Connection outputConnectionUCA = connectionFactory.getRabbitConnectionFactory().newConnection();
            Channel outputChannel = outputConnectionUCA.createChannel();) {

            String deployQueue = "deploy";
            String undeployQueue = "undeploy";
            
            outputChannel.queueDeclare(deployQueue, false, false, false, null);
            outputChannel.queueDeclare(undeployQueue, false, false, false, null);

            Optional<EventPattern> eventPatternOptional = eventPatternRepository.findById(id);
            if (!eventPatternOptional.isEmpty()){
                EventPattern retrievedEventPattern = eventPatternOptional.get();
                retrievedEventPattern.setDeployed(status);
                retrievedEventPattern.setReadyToDeploy(false);
                eventPatternRepository.save(retrievedEventPattern);

                String message;

                if(status) {
                    // Send deploy message
                    message = retrievedEventPattern.getContent();
                    outputChannel.basicPublish("",deployQueue, null, message.getBytes());
                }
                else {
                    // Send undeploy message
                    message = retrievedEventPattern.getName();
                    outputChannel.basicPublish("",undeployQueue, null, message.getBytes());
                }
                
                return true;
            }
            else {
              return false;
            }
        } catch (IOException | TimeoutException e) {
            return false;
        }
    }

    /**
     * Finds event patterns by name.
     * @param name The name to search for
     * @return List of EventPattern entities with the given name
     */
    public List<EventPattern> findByName(String name) {
        return eventPatternRepository.findByName(name);
    }

    /**
     * Deletes an event pattern by ID.
     * @param id The ID of the event pattern to delete
     * @return True if the deletion was successful, false otherwise
     */
    public boolean deleteEventPattern(Long id) {
        try {
            Optional<EventPattern> eventPatternOptional = eventPatternRepository.findById(id);
            if (!eventPatternOptional.isEmpty()){
                EventPattern retrievedEventPattern = eventPatternOptional.get();
                if (retrievedEventPattern.isDeployed()) {
                    return false;
                }
                else if (retrievedEventPattern.isReadyToDeploy()) {
                    return false;
                }
                else {
                    eventPatternRepository.deleteById(id);
                    return true;
                }
            }
            else {
                return false;
            }      
        } catch (Exception e) {
            return false;
        }
    }    
}
