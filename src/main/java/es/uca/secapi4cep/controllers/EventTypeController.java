package es.uca.secapi4cep.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.uca.secapi4cep.entities.EventType;
import es.uca.secapi4cep.services.EventTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Event Types", description = "Event Types API")
@RestController
@RequestMapping("/event-type")
public class EventTypeController {

    // Logger for recording logs related to event types
    private static final Logger logger = LoggerFactory.getLogger(EventTypeController.class);

    // Service for handling event type operations
    private final EventTypeService eventTypeService;

    // Constant string for logging purposes
    private static final String EVENT_TYPE_STRING = "Event type with id: ";

    // Helper method to get the current username from the security context
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return "Anonymous"; // Default to "Anonymous" if no user is authenticated
    }

    // Constructor to inject EventTypeService
    EventTypeController(EventTypeService eventTypeService) {
        this.eventTypeService = eventTypeService;
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Retrieves all existing event types",
        description = "Retrieves all existing event types"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EventType> getAllEventTypes() {
        // Fetches and returns all event types from the service
        return eventTypeService.getAllEventTypes();
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Registers a new event type",
        description = "Registers a new event type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event type successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input provided"),
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Object>> createEventType(@RequestBody EventType eventType) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            // Attempt to save the event type using the service
            EventType createdEventType = this.eventTypeService.saveEventType(eventType);
            logger.info("User " + getCurrentUsername() + " has successfully created an event type with id: {}", createdEventType.getId());
            responseBody.put("message", "Event type created successfully.");
            responseBody.put("eventType", createdEventType);
            // Return success response with created event type
            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
        }
        catch (Exception e) {
            responseBody.put("message", "Event type has not been successfully created.");
            logger.warn("User " + getCurrentUsername() + " failed to create event type.");
            // Return failure response if an exception occurs
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        } 
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Retrieves the event type with the provided ID",
        description = "Retrieves the event type with the provided ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<EventType> getEventTypeById(@PathVariable("id") Long id) {
         // Fetches and returns the event type by ID
        return this.eventTypeService.getEventTypeById(id);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Updates the event type with the provided ID",
        description = "Updates an existing type identified by its ID in the database and in the CEP engine if necessary"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public String updateEventType(@RequestBody EventType eventType, @PathVariable("id") Long id) {
        // Attempt to update the event type and return appropriate status message
        boolean ok = this.eventTypeService.updateEventType(eventType, id);
        if (ok) {
            logger.info("User " + getCurrentUsername() + " has successfully updated the event type with id: {}", id);
            return EVENT_TYPE_STRING + id + " has been updated";
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to update event type with id: {}", id);
            return EVENT_TYPE_STRING + id + " has not been updated";
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Updates the status of the event type with the provided ID to ready to deploy",
        description = "Updates the status of the event type with the provided ID to ready to deploy"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/ready/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String readyToDeploy(@PathVariable Long id) {
        // Marks the event type as ready to deploy and return status message
        boolean ok = eventTypeService.updateStatus(id, true);
        if (ok) {
          return EVENT_TYPE_STRING + id + " has been set as ready to deploy";
        } else {
          return EVENT_TYPE_STRING + id + " has not been set as ready to deploy";
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Updates the status of the event type with the provided ID to not ready to deploy",
        description = "Updates the status of the event type with the provided ID to not ready to deploy"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/unready/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String unReadyToDeploy(@PathVariable Long id) {
        // Marks the event type as not ready to deploy and return status message
        boolean ok = eventTypeService.updateStatus(id, false);
        if (ok) {
          return EVENT_TYPE_STRING + id + " has been set as not ready to deploy";
        } else {
          return EVENT_TYPE_STRING + id + " has not been set as not ready to deploy";
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Deploys an existing event type identified by its ID",
        description = "Deploys an existing event type identified by its ID and sets it as deployed in the database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/deploy/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String deploy(@PathVariable Long id) {
        // Deploys the event type, marks it as deployed and return status message
        boolean ok = eventTypeService.updateDeployingStatus(id, true);
        if (ok) {
            logger.info(EVENT_TYPE_STRING + id + " has been deployed by " + getCurrentUsername());
            return EVENT_TYPE_STRING + id + " has been deployed";
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to deploy event type with id: {}", id);
            return EVENT_TYPE_STRING + id + " has not been deployed";
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Undeploys an existing event type identified by its ID",
        description = "Undeploys an existing event type identified by its ID and sets it as not deployed in the database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/undeploy/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String undeploy(@PathVariable Long id) {
        // Undeploys the event type, marks it as deployed and return status message
        boolean ok = eventTypeService.updateDeployingStatus(id, false);
        if (ok) {
            logger.info(EVENT_TYPE_STRING + id + " has been undeployed by " + getCurrentUsername());
            return EVENT_TYPE_STRING + id + " has been undeployed";
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to undeploy event type with id: {}", id);
            return EVENT_TYPE_STRING + id + " has not been undeployed";
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Retrieves the event type with the provided name",
        description = "Retrieves the event type with the provided name"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @GetMapping(value = "/name", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EventType> findByName(@RequestParam String name) {
        // Fetches and returns event type by name
        return this.eventTypeService.findByName(name);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Deletes the event type with the provided ID",
        description = "Deletes the event type with the provided ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @DeleteMapping(path = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    public String deleteEventType(@PathVariable("id") Long id) {
        // Attempt to delete the event type and return appropriate status message
        boolean ok = this.eventTypeService.deleteEventType(id);
        if (ok) {
            logger.info(EVENT_TYPE_STRING + id + " has been deleted by " + getCurrentUsername());
            return EVENT_TYPE_STRING + id + " has been deleted";
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to delete event type with id: {}", id);
            return EVENT_TYPE_STRING + id + " has not been deleted";
        }
    }
}