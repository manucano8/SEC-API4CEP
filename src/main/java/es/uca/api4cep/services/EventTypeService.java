package es.uca.api4cep.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import es.uca.api4cep.repositories.EventTypeRepository;
import com.rabbitmq.client.Channel;
import es.uca.api4cep.entities.EventType;

@Service
public class EventTypeService {

    // Injects the RabbitMQ ConnectionFactory defined in RabbitMQConfig
    @Autowired
    private CachingConnectionFactory connectionFactory;
    
    private final EventTypeRepository eventTypeRepository;

    // Constructor to inject EventTypeRepository
    EventTypeService(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    /**
     * Retrieves all event types from the repository.
     * @return List of all EventType entities
     */
    public List<EventType> getAllEventTypes() {
        return (ArrayList<EventType>) eventTypeRepository.findAll();
    }

    /**
     * Retrieves an event type by its ID.
     * @param id The ID of the event type
     * @return Optional containing the EventType if found, otherwise empty
     */
    public Optional<EventType> getEventTypeById(Long id) {
        return eventTypeRepository.findById(id);
    }

    /**
     * Saves a new event type to the repository.
     * @param eventType The EventType entity to save
     * @return The saved EventType entity
     */
    public EventType saveEventType(EventType eventType) {
        return eventTypeRepository.save(eventType);
    }

    /**
     * Updates an existing event type and handles deployment and deployment logic.
     * @param newEventType The new event type data
     * @param id The ID of the event type to update
     * @return True if update was successful, false otherwise
     */
    public boolean updateEventType(EventType newEventType, Long id) {
        
        try (com.rabbitmq.client.Connection outputConnectionUCA = connectionFactory.getRabbitConnectionFactory().newConnection();
            Channel outputChannel = outputConnectionUCA.createChannel();){

            String deployQueue = "deploy";
            String undeployQueue = "undeploy";
            
            outputChannel.queueDeclare(deployQueue, false, false, false, null);
            outputChannel.queueDeclare(undeployQueue, false, false, false, null);

            Optional<EventType> eventTypeOptional = eventTypeRepository.findById(id);
            if (!eventTypeOptional.isEmpty()){
                EventType retrievedEventType = eventTypeOptional.get();
                String retrievedEventTypeName = retrievedEventType.getName();
                if(!retrievedEventType.isReadyToDeploy()) {
                    retrievedEventType.setName(newEventType.getName());
                    retrievedEventType.setContent(newEventType.getContent());

                    eventTypeRepository.save(retrievedEventType);
                    if (retrievedEventType.isDeployed()) {
                        String message;
                        
                        // Send undeploy message
                        message = retrievedEventTypeName;
                        outputChannel.basicPublish("",undeployQueue, null, message.getBytes());

                        // Send deploy message
                        message = newEventType.getContent();
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
            return false;
        }
    }

    /**
     * Updates the 'ready to deploy' status of an event type.
     * @param id The ID of the event type
     * @param status The new 'ready to deploy' status
     * @return True if the update was successful, false otherwise
     */
    public boolean updateStatus(Long id, boolean status) {
        Optional<EventType> eventTypeOptional = eventTypeRepository.findById(id);
        if (!eventTypeOptional.isEmpty()){
            EventType retrievedEventType = eventTypeOptional.get();
            retrievedEventType.setReadyToDeploy(status);
            eventTypeRepository.save(retrievedEventType);
            return true;
        }
        else {
          return false;
        }
    }

    /**
     * Updates the deploying status of an event type and sends appropriate messages.
     * @param id The ID of the event type
     * @param status The new deploying status
     * @return True if the update was successful, false otherwise
     */
    public boolean updateDeployingStatus(Long id, boolean status) {
        
        try(com.rabbitmq.client.Connection outputConnectionUCA = connectionFactory.getRabbitConnectionFactory().newConnection();
            Channel outputChannel = outputConnectionUCA.createChannel();) {

            String deployQueue = "deploy";
            String undeployQueue = "undeploy";
            
            outputChannel.queueDeclare(deployQueue, false, false, false, null);
            outputChannel.queueDeclare(undeployQueue, false, false, false, null);

            Optional<EventType> eventTypeOptional = eventTypeRepository.findById(id);
            if (!eventTypeOptional.isEmpty()){
                EventType retrievedEventType = eventTypeOptional.get();
                retrievedEventType.setDeployed(status);
                retrievedEventType.setReadyToDeploy(false);
                eventTypeRepository.save(retrievedEventType);

                String message;

                if(status) {
                     // Send deploy message
                    message = retrievedEventType.getContent();
                    outputChannel.basicPublish("",deployQueue, null, message.getBytes());
                }
                else {
                    // Send undeploy message
                    message = retrievedEventType.getName();
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
     * Finds event types by name.
     * @param name The name to search for
     * @return List of EventType entities with the given name
     */
    public List<EventType> findByName(String name) {
        return eventTypeRepository.findByName(name);
    }

    /**
     * Deletes an event type by ID.
     * @param id The ID of the event type to delete
     * @return True if the deletion was successful, false otherwise
     */
    public boolean deleteEventType(Long id) {
        try {
            Optional<EventType> eventTypeOptional = eventTypeRepository.findById(id);
            if (!eventTypeOptional.isEmpty()){
                EventType retrievedEventType = eventTypeOptional.get();
                if (retrievedEventType.isDeployed()) {
                    return false;
                }
                else if (retrievedEventType.isReadyToDeploy()) {
                    return false;
                }
                else {
                    eventTypeRepository.deleteById(id);
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