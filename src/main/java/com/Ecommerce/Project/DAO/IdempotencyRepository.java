package com.Ecommerce.Project.DAO;

import com.Ecommerce.Project.Entity.Idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;


public interface IdempotencyRepository extends JpaRepository<Idempotency,Long> {
    public Optional<Idempotency> findByUserIdAndIdempotencyKey(Integer userId, String idempotencyKey);

    @Modifying(clearAutomatically = true)//it used to remove stale data from db cache
    @Query("""
            update Idempotency i
            set i.status='IN_PROGRESS',
            i.expireAt= :newExpiry,
            i.updatedAt= :now
            where i.id= :id and
            i.expireAt< :now and
            i.status in('IN_PROGRESS','FAILED')
            """)
    public int tryAcquireRowForRetry(Long id, LocalDateTime now,LocalDateTime newExpiry);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Idempotency i
            set i.responsePayload= :responsePayload,
            i.updatedAt= :now,
            i.statusCode= :statusCode,
            i.status= 'COMPLETED'
            where i.id= :id and
            i.status= 'IN_PROGRESS'
            """)
    public int markSuccessIfInProgress(Long id, String responsePayload, LocalDateTime now, int statusCode );

    @Modifying
    @Query("""
            delete from Idempotency i where i.expireAt< :now
            """)
    public int cleanUpExpiredIdempotencyRecords(@Param("now") LocalDateTime now);
}
