package keville.providers.Eventbrite;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

//Eventbrite.Event ..
public interface EventbriteRepository extends CrudRepository<Event, Integer>{

  @Query("SELECT * FROM EVENTBRITE_EVENT WHERE event_id = :eventId")
  Optional<Event> findByEventId(String eventId);

}
