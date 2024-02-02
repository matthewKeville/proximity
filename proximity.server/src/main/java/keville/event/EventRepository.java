package keville.event;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Integer>{

  //todo , actually discriminate under enum..
  @Query("SELECT * FROM EVENT WHERE event_id = :eventId")
  Optional<Event> findByEventIdAndType(String eventId,EventTypeEnum type);

}
