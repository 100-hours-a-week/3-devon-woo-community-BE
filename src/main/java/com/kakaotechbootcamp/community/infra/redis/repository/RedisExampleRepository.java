package com.kakaotechbootcamp.community.infra.redis.repository;

import com.kakaotechbootcamp.community.infra.redis.domain.RedisExampleEntity;
import org.springframework.data.repository.CrudRepository;

public interface RedisExampleRepository extends CrudRepository<RedisExampleEntity, String> {

}
