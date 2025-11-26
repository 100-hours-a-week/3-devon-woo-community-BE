package com.devon.techblog.infra.redis.repository;

import com.devon.techblog.infra.redis.domain.RedisExampleEntity;
import org.springframework.data.repository.CrudRepository;

public interface RedisExampleRepository extends CrudRepository<RedisExampleEntity, String> {

}
