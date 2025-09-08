package com.heattrip.heat_trip_backend.S3.media;

import com.heattrip.heat_trip_backend.S3.UploadCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaObjectRepository extends JpaRepository<MediaObject, Long> {
    Optional<MediaObject> findByKey(String key);
    /** 컨트롤러/서비스에서 사용 */
    List<MediaObject> findByRefTypeAndRefId(String refType, String refId);
    List<MediaObject> findByOwnerIdAndCategory(String ownerId, UploadCategory category);
}
