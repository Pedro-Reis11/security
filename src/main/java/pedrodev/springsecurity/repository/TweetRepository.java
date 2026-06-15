package pedrodev.springsecurity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pedrodev.springsecurity.entity.Role;
import pedrodev.springsecurity.entity.Tweet;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
}
