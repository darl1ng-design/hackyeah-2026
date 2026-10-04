package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.hubmalopolski.hub.domain.HubWorkflowRecord;

import java.util.List;
import java.util.Optional;

public interface HubWorkflowRecordRepository extends JpaRepository<HubWorkflowRecord, Long> {
    List<HubWorkflowRecord> findByModuleOrderByCreatedAtDescIdDesc(String module);

    List<HubWorkflowRecord> findByModuleAndStatusOrderByCreatedAtDescIdDesc(String module, String status);

    List<HubWorkflowRecord> findByModuleAndOwnerEmailOrderByCreatedAtDescIdDesc(String module, String email);

    Optional<HubWorkflowRecord> findByIdAndModule(Long id, String module);

    @Query("select r from HubWorkflowRecord r where r.module = :module and r.referenceId = :referenceId and r.owner.id = :ownerId")
    Optional<HubWorkflowRecord> findByModuleAndReferenceAndOwner(@Param("module") String module,
            @Param("referenceId") Long referenceId, @Param("ownerId") Long ownerId);

    @Query("select r from HubWorkflowRecord r where r.module = :module and r.referenceId = :referenceId order by r.createdAt asc, r.id asc")
    List<HubWorkflowRecord> findByModuleAndReference(@Param("module") String module,
            @Param("referenceId") Long referenceId);
}
