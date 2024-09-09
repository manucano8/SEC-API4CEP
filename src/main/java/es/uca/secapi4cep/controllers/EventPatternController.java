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

import es.uca.secapi4cep.entities.EventPattern;
import es.uca.secapi4cep.services.EventPatternService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "2. Event Patterns", description = "Event Patterns API")
@RestController
@RequestMapping("/event-pattern")
public class EventPatternController {

    // Logger for recording logs related to event patterns
    private static final Logger logger = LoggerFactory.getLogger(EventPatternController.class);

    // Service for handling event pattern operations
    private final EventPatternService eventPatternService;

    // Constant string for logging purposes
    private static final String EVENT_PATTERN_STRING = "Event pattern with id: ";

    // Helper method to get the current username from the security context
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return "Anonymous"; // Default to "Anonymous" if no user is authenticated
    } 

    // Constructor to inject EventPatternService
    EventPatternController(EventPatternService eventPatternService) {
        // Fetches and returns all event patterns from the service
        this.eventPatternService = eventPatternService;
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Retrieves all existing event patterns",
        description = "Retrieves all existing event patterns"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EventPattern>> getAllEventPatterns() {
        // Fetches and returns all event patterns from the service
        List<EventPattern> eventPatterns = eventPatternService.getAllEventPatterns();
        return ResponseEntity.ok(eventPatterns);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Registers a new event pattern",
        description = "Registers a new event pattern"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event pattern successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input provided"),
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Object>> createEventPattern(@RequestBody EventPattern eventPattern) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            // Attempt to save the event pattern using the service
            EventPattern createdEventPattern = this.eventPatternService.saveEventPattern(eventPattern);
            logger.info("User " + getCurrentUsername() + " has successfully created an event pattern with id: {}", createdEventPattern.getId());
            responseBody.put("message", "Event pattern created successfully.");
            responseBody.put("eventPattern", createdEventPattern);
            // Return success response with the created event pattern
            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
        }
        catch (Exception e) {
            responseBody.put("message", "Event pattern has not been successfully created.");
            logger.warn("User " + getCurrentUsername() + " failed to create event pattern.");
            // Return failure response if an exception occurs
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }     
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Retrieves the event pattern with the provided ID",
        description = "Retrieves the event pattern with the provided ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event Pattern successfully retrieved."),
        @ApiResponse(responseCode = "404", description = "Event Pattern not found."),
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getEventPatternById(@PathVariable("id") Long id) {
        // Fetches and returns the event pattern by ID
        Map<String, Object> responseBody = new HashMap<>();
        Optional<EventPattern> eventPattern = this.eventPatternService.getEventPatternById(id);
        if (eventPattern.isPresent()) {
            responseBody.put("status", HttpStatus.OK.value());
            responseBody.put("eventPattern", eventPattern.get());
            return ResponseEntity.ok(responseBody);
        } else {
            responseBody.put("message", "Event pattern not found.");
            responseBody.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Updates the event pattern with the provided ID",
        description = "Updates an existing pattern identified by its ID in the database and in the CEP engine if necessary"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated."),
        @ApiResponse(responseCode = "200", description = "Event pattern succesfully updated."),
        @ApiResponse(responseCode = "400", description = "Event pattern has not been updated.")
    })
    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> updateEventPattern(@RequestBody EventPattern eventPattern, @PathVariable("id") Long id) {
        // Attempt to update the event pattern and return appropriate status message
        Map<String, String> responseBody = new HashMap<>();
        boolean ok = this.eventPatternService.updateEventPattern(eventPattern, id);
        if (ok) {
            logger.info("User " + getCurrentUsername() + " has successfully updated the event pattern with id: {}", id);
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has been updated");
            responseBody.put("status", String.valueOf(HttpStatus.OK.value()));
            return ResponseEntity.ok(responseBody);
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to update event pattern with id: {}", id);
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has not been updated");
            responseBody.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        } 
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Sets an existing pattern identified by its ID as ready to deploy",
        description = "Sets an existing pattern identified by its ID as ready to deploy"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/ready/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> readyToDeploy(@PathVariable Long id) {
        // Marks the event pattern as ready to deploy and return status message
        Map<String, String> responseBody = new HashMap<>();
        boolean ok = eventPatternService.updateStatus(id, true);
        if (ok) {
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has been set as ready to deploy");
            responseBody.put("status", String.valueOf(HttpStatus.OK.value()));
            return ResponseEntity.ok(responseBody);
        } else {
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has not been set as ready to deploy");
            responseBody.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Sets an existing pattern identified by its ID as not ready to deploy",
        description = "Sets an existing pattern identified by its ID as not ready to deploy"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/unready/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> unReadyToDeploy(@PathVariable Long id) {
        // Marks the event pattern as not ready to deploy and return status message
        Map<String, String> responseBody = new HashMap<>();
        boolean ok = eventPatternService.updateStatus(id, false);
        if (ok) {
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has been set as not ready to deploy");
            responseBody.put("status", String.valueOf(HttpStatus.OK.value()));
            return ResponseEntity.ok(responseBody);
        } else {
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has not been set as not ready to deploy");
            responseBody.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Deploys an existing pattern identified by its ID",
        description = "Deploys an existing pattern identified by its ID and sets it as deployed in the database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/deploy/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> deploy(@PathVariable Long id) {
        // Deploys the event pattern, marks it as deployed and return status message
        Map<String, String> responseBody = new HashMap<>();
        boolean ok = eventPatternService.updateDeployingStatus(id, true);
        if (ok) {
            logger.info(EVENT_PATTERN_STRING + id + " has been deployed by " + getCurrentUsername());
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has been deployed");
            responseBody.put("status", String.valueOf(HttpStatus.OK.value()));
            return ResponseEntity.ok(responseBody);
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to deploy event pattern with id: {}", id);
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has not been deployed");
            responseBody.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Undeploys an existing pattern identified by its ID",
        description = "Undeploys an existing pattern identified by its ID and sets it as not deployed in the database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @PutMapping(value = "/undeploy/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> undeploy(@PathVariable Long id) {
        // Undeploys the event pattern, marks it as not deployed and return status message
        Map<String, String> responseBody = new HashMap<>();
        boolean ok = eventPatternService.updateDeployingStatus(id, false);
        if (ok) {
            logger.info(EVENT_PATTERN_STRING + id + " has been undeployed by " + getCurrentUsername());
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has been undeployed");
            responseBody.put("status", String.valueOf(HttpStatus.OK.value()));
            return ResponseEntity.ok(responseBody);
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to undeploy event pattern with id: {}", id);
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has not been undeployed");
            responseBody.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Retrieves the event pattern with the provided name",
        description = "Retrieves the event pattern with the provided name"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @GetMapping(value = "/name", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> findByName(@RequestParam String name) {
        // Fetches and returns the event pattern by name
        Map<String, Object> responseBody = new HashMap<>();
        List<EventPattern> eventPatterns = this.eventPatternService.findByName(name);
        responseBody.put("status", HttpStatus.OK.value());
        responseBody.put("eventPatterns", eventPatterns);
        return ResponseEntity.ok(responseBody);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Deletes the event pattern with the provided ID",
        description = "Deletes the event pattern with the provided ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> deleteEventPattern(@PathVariable("id") Long id) {
        // Attempt to delete the event pattern and return appropriate status message
        Map<String, String> responseBody = new HashMap<>();
        boolean ok = this.eventPatternService.deleteEventPattern(id);
        if (ok) {
            logger.info(EVENT_PATTERN_STRING + id + " has been deleted by " + getCurrentUsername());
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has been deleted");
            responseBody.put("status", String.valueOf(HttpStatus.OK.value()));
            return ResponseEntity.ok(responseBody);
        } else {
            logger.warn("User " + getCurrentUsername() + " failed to delete event pattern with id: {}", id);
            responseBody.put("message", EVENT_PATTERN_STRING + id + " has not been deleted");
            responseBody.put("status", String.valueOf(HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }
}
